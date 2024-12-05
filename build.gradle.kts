/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.TimeZone

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("com.jfrog.artifactory") version "4.32.0"
    id("maven-publish")

}

buildscript {
    val kotlinVersion by extra("1.8.10")

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()

        maven("https://cboost.jfrog.io/artifactory/private-chartboost-core/") {
            credentials {
                username = System.getenv("JFROG_USER")
                password = System.getenv("JFROG_PASS")
            }
        }
        maven("https://cboost.jfrog.io/artifactory/chartboost-core/")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.4.1")
        classpath("com.vanniktech:gradle-android-apk-size-plugin:0.4.0")
        classpath("com.getkeepsafe.dexcount:dexcount-gradle-plugin:4.0.0")
        classpath("com.google.gms:google-services:4.3.14")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.5")
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
        classpath("com.google.firebase:firebase-appdistribution-gradle:3.1.1")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.31.9")
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    }
}



allprojects {
    repositories {
        google()
        mavenCentral()

    }
}

