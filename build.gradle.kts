/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

   import java.time.Instant
   import java.time.format.DateTimeFormatter
   import java.util.*

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("com.jfrog.artifactory") version "4.32.0"
    id("maven-publish")

}

buildscript {
    val kotlin_version by extra("1.7.20")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")

    }
}


allprojects {
    repositories {
        google()
        mavenCentral()

    }
}

