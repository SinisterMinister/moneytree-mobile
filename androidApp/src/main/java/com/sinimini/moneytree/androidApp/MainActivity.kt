package com.sinimini.moneytree.androidApp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import de.codecrafters.tableview.TableView
import de.codecrafters.tableview.toolkit.SimpleTableDataAdapter
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter
import io.grpc.ManagedChannelBuilder
import io.grpc.ManagedChannel
import android.widget.Button
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import java.lang.Exception

import com.sinimini.moneytree.proto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.logging.Logger
import org.ocpsoft.prettytime.PrettyTime
import java.time.Instant


class MainActivity : AppCompatActivity() {
    private val logger = Logger.getLogger(this.javaClass.name)

    private fun channel(): ManagedChannel {
        val url = URL(resources.getString(R.string.server_url))
        val port = if (url.port == -1) url.defaultPort else url.port

        logger.info("Connecting to ${url.host}:$port")

        val builder = ManagedChannelBuilder.forAddress(url.host, port)
        if (url.protocol == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        return builder.executor(Dispatchers.Default.asExecutor()).build()
    }

    private val moneytree by lazy { MoneytreeGrpcKt.MoneytreeCoroutineStub(channel()) }

    private val TABLE_HEADERS = arrayOf("Created", "Direction", "Buy Price", "Buy Qty", "Sell Price", "Sell Qty")

    fun sendUpPair() = runBlocking {
        try {
            val request = MoneytreeProto.PlacePairRequest.newBuilder().setDirection("UP").build()
            moneytree.placePair(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendDownPair() = runBlocking {
        try {
            val request = MoneytreeProto.PlacePairRequest.newBuilder().setDirection("DOWN").build()
            moneytree.placePair(request)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getOpenPairs(): MoneytreeProto.PairCollection {
        try {
            return  moneytree.getOpenPairs(MoneytreeProto.NullRequest.getDefaultInstance())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return MoneytreeProto.PairCollection.getDefaultInstance()
    }

    suspend fun refreshPairData() {
        val pairs = getOpenPairs().pairsList
        runOnUiThread {
            val tableView = findViewById<TableView<Array<String>>>(R.id.open_pairs_table)
            val p = PrettyTime()

            tableView.dataAdapter.clear()
            for (pair in pairs) {
                tableView.dataAdapter.add(
                    arrayOf(
                        p.format(LocalDateTime.ofEpochSecond(pair.created, 0, (ZoneOffset.of("-7")))),
                        pair.direction,
                        pair.buyOrder.price,
                        pair.buyOrder.quantity,
                        pair.sellOrder.price,
                        pair.sellOrder.quantity,
                    )
                )
            }
            tableView.invalidate()
        }
    }

    suspend fun refreshCandleData() {
        val chart = findViewById<CandleStickChart>(R.id.candle_stick_chart)
        val candles = getCandleCollection().candlesList.reversed()
        val data = mutableListOf<CandleEntry>()
        var start = 0L

        for (candle in candles) {
            if (start == 0L) {
                start = candle.ts
            }
            data.add(CandleEntry(candle.ts.minus(start).div(60).toFloat(), candle.high.toFloat(), candle.low.toFloat(), candle.open.toFloat(), candle.close.toFloat()))
        }

        val candleDataSet = CandleDataSet(data, "Price")
        candleDataSet.increasingColor = resources.getColor(R.color.colorGreen, null)
        candleDataSet.decreasingColor = resources.getColor(R.color.colorRed, null)
        candleDataSet.valueTextColor = resources.getColor(R.color.colorLight, null)
        candleDataSet.axisDependency = YAxis.AxisDependency.RIGHT
        candleDataSet.isHighlightEnabled = true
        candleDataSet.shadowColorSameAsCandle = true
        candleDataSet.setDrawValues(false)
        val candleData = CandleData(candleDataSet)
        chart.data = candleData
        chart.invalidate()
    }

    suspend fun getCandleCollection(): MoneytreeProto.CandleCollection {
        try {
            val e = Instant.now()
            val s = e.minusSeconds(60 * 180)
            return moneytree.getCandles(MoneytreeProto.GetCandlesRequest.newBuilder()
                .setDuration(MoneytreeProto.GetCandlesRequest.Duration.ONE_MINUTE)
                .setStartTime(s.toEpochMilli().div(1000))
                .setEndTime(e.toEpochMilli().div(1000))
                .build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return MoneytreeProto.CandleCollection.getDefaultInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup the Open Orders Table
        val tableView = findViewById<TableView<Array<String>>>(R.id.open_pairs_table)
        val headerAdpter = SimpleTableHeaderAdapter(this, *TABLE_HEADERS)
        headerAdpter.setTextSize(10)
        headerAdpter.setTextColor(resources.getColor(R.color.colorLight, null))
        tableView.headerAdapter = headerAdpter
        tableView.setHeaderBackgroundColor(resources.getColor(R.color.colorDark, null))
        val dataAdapter = SimpleTableDataAdapter(this, mutableListOf<Array<String>>())
        dataAdapter.setTextSize(10)
        dataAdapter.setPaddings(4,2, 4, 2)
        dataAdapter.setTextColor(resources.getColor(R.color.colorLight, null))
        tableView.dataAdapter = dataAdapter
        tableView.isSwipeToRefreshEnabled = true
        tableView.setSwipeToRefreshListener {
            runBlocking {
                refreshPairData()
            }
            it.hide()
        }
        runBlocking {
            launch {
                refreshPairData()
            }
        }

        // Setup pair submission buttons
        val downButton = findViewById<Button>(R.id.button_downward)
        val upButton = findViewById<Button>(R.id.button_upward)
        upButton.setOnClickListener {
            upButton.isEnabled = false
            sendUpPair()
            upButton.isEnabled = true
            GlobalScope.launch {
                delay(1000)
                refreshPairData()
            }
        }
        downButton.setOnClickListener {
            downButton.isEnabled = false
            sendDownPair()
            downButton.isEnabled = true
            GlobalScope.launch {
                delay(1000)
                refreshPairData()
            }
        }

        // Setup the candlestick chart
        val chart = findViewById<CandleStickChart>(R.id.candle_stick_chart)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setBackgroundColor(resources.getColor(R.color.colorDark, null))
        chart.setNoDataTextColor(resources.getColor(R.color.colorLight, null))
        chart.requestDisallowInterceptTouchEvent(true)
        chart.setDrawBorders(false)
        chart.axisRight.textColor = resources.getColor(R.color.colorLight, null)
        chart.axisRight.axisLineColor = resources.getColor(R.color.colorGrey, null)
        chart.axisRight.gridColor =  resources.getColor(R.color.colorGrey, null)
        chart.axisLeft.textColor = resources.getColor(R.color.colorLight, null)
        chart.axisLeft.axisLineColor = resources.getColor(R.color.colorGrey, null)
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.textColor = resources.getColor(R.color.colorLight, null)
        chart.xAxis.axisLineColor = resources.getColor(R.color.colorGrey, null)
        chart.xAxis.gridColor =  resources.getColor(R.color.colorGrey, null)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.setDrawLabels(false)
        chart.xAxis.isGranularityEnabled = true
        chart.xAxis.granularity = 1f

        // Refresh the candle data every minute
        GlobalScope.launch {
            val tickerChannel = ticker(delayMillis = 15 * 1000, initialDelayMillis = 0)
            for (event in tickerChannel) {
                refreshCandleData()
                refreshPairData()
            }
        }

        // Refresh the ticker data every 5 seconds
        GlobalScope.launch {
            val tickerChannel = ticker(delayMillis = 5 * 1000, initialDelayMillis = 0)
            for (event in tickerChannel) {
                refreshPairData()
            }
        }
    }
}
