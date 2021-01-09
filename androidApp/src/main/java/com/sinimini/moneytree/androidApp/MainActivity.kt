package com.sinimini.moneytree.androidApp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import de.codecrafters.tableview.TableView
import de.codecrafters.tableview.toolkit.SimpleTableDataAdapter
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter
import io.grpc.ManagedChannelBuilder
import io.grpc.ManagedChannel
import android.widget.Button
import java.lang.Exception

import com.sinimini.moneytree.proto.*
import kotlinx.coroutines.*
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.logging.Logger
import org.ocpsoft.prettytime.PrettyTime




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

    private val TABLE_DATA = arrayOf(
        arrayOf("2020-12-30T12:11:11", "Upward", "27629.92", "0.01453420", "27712.81", "0.01451246"),
        arrayOf("2020-12-30T11:40:23", "Downward", "27553.09", "0.01451237", "27636.00", "0.01449054"),
    )

    fun sendUpPair() = runBlocking {
        try {
            val request = MoneytreeProto.PlacePairRequest.newBuilder().setDirection("UP").build()
            moneytree.placePair(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        launch {
            delay(500)
            refreshData()
        }
    }

    fun sendDownPair() = runBlocking {
        try {
            val request = MoneytreeProto.PlacePairRequest.newBuilder().setDirection("DOWN").build()
            moneytree.placePair(request)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        launch {
            delay(500)
            refreshData()
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

    suspend fun refreshData() {
        val tableView: TableView<Array<String>> = findViewById<View>(R.id.open_pairs_table) as TableView<Array<String>>
        val p = PrettyTime()
        val pairs = getOpenPairs().pairsList
        val data = mutableListOf<Array<String>>()

        for (pair in pairs) {
            data.add(arrayOf(
                p.format(LocalDateTime.ofEpochSecond(pair.created,0, (ZoneOffset.of("-7")))),
                pair.direction,
                pair.buyOrder.price,
                pair.buyOrder.quantity,
                pair.sellOrder.price,
                pair.sellOrder.quantity,
            ))
        }
        val refreshAdapter = SimpleTableDataAdapter(this, data.toTypedArray())
        refreshAdapter.setTextSize(10)
        refreshAdapter.setPaddings(4,2, 4, 2)
        refreshAdapter.setTextColor(resources.getColor(R.color.colorHeaderForeground, null))
        tableView.dataAdapter = refreshAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup the Open Orders Table
        val tableView: TableView<Array<String>> = findViewById<View>(R.id.open_pairs_table) as TableView<Array<String>>
        val headerAdpter = SimpleTableHeaderAdapter(this, *TABLE_HEADERS)
        headerAdpter.setTextSize(10)
        headerAdpter.setTextColor(resources.getColor(R.color.colorHeaderForeground, null))
        tableView.headerAdapter = headerAdpter
        tableView.setHeaderBackgroundColor(resources.getColor(R.color.colorHeaderBackground, null))
        tableView.isSwipeToRefreshEnabled = true
        tableView.setSwipeToRefreshListener {
            runBlocking {
                refreshData()
            }
            it.hide()
        }

        val downButton = findViewById<Button>(R.id.button_downward)
        val upButton = findViewById<Button>(R.id.button_upward)

        upButton.setOnClickListener {
            upButton.isEnabled = false
            sendUpPair()
            upButton.isEnabled = true
        }

        downButton.setOnClickListener {
            downButton.isEnabled = false
            sendDownPair()
            downButton.isEnabled = true
        }

        runBlocking {
            launch {
                refreshData()
            }
        }
    }
}
