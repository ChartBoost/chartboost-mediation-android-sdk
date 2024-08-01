/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.TimeZone

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.jfrog.artifactory") version "4.32.0"
    id("maven-publish")

}

fun getShortGitCommitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

buildscript {
    val kotlinVersion by extra("1.9.21")

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
        classpath("com.android.tools:r8:8.3.37")
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.vanniktech:gradle-android-apk-size-plugin:0.4.0")
        classpath("com.getkeepsafe.dexcount:dexcount-gradle-plugin:4.0.0")
        classpath("com.google.gms:google-services:4.4.1")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
        classpath("com.google.firebase:firebase-appdistribution-gradle:4.2.0")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.32.0")
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    }
}



allprojects {

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    repositories {
        google()
        mavenCentral()

    }
}

