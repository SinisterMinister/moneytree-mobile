import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
    id("com.google.protobuf") version "0.8.14"
    idea
}

group = "com.sinimini.moneytree"
version = "1.0-SNAPSHOT"


dependencies {
    implementation(project(":shared"))
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.2")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("de.codecrafters.tableview:tableview:2.8.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    protobuf(project(":protos"))

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.0.Final")

    api("com.google.protobuf:protobuf-javalite:${rootProject.ext["protobufVersion"]}")
    api("io.grpc:grpc-kotlin-stub-lite:${rootProject.ext["grpcKotlinVersion"]}")
    runtimeOnly("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
}

repositories {
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "com.sinimini.moneytree.androidApp"
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"

        val serverUrl: String? by project
        if (serverUrl != null) {
            resValue("string", "server_url", serverUrl!!)
        } else {
            resValue("string", "server_url", "http://10.128.10.35:44444/")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildToolsVersion = "30.0.3"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${rootProject.ext["protobufVersion"]}"
    }
    plugins {
        id("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${rootProject.ext["grpcKotlinVersion"]}:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("java") {
                    option("lite")
                }
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
        }
    }
}