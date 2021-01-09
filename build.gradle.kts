buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
        classpath("com.android.tools.build:gradle:4.0.1")
    }
}

group = "com.sinimini.moneytree"
version = "1.0-SNAPSHOT"

ext["grpcVersion"] = "1.32.1"
ext["grpcKotlinVersion"] = "1.0.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.13.0"

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}


allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
    }

//    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}