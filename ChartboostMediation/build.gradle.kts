/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlinx-serialization")
}

android {
    namespace = "com.chartboost.chartboostmediationsdk"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    kotlinOptions {
        freeCompilerArgs += "-Xjvm-default=all"
    }

    compileSdk = ChartboostMediationAndroidSupport.chartboostMediationAndroidApiVersion

    defaultConfig {
        minSdk = ChartboostMediationAndroidSupport.chartboostMediationMinimumAndroidApiVersion
        targetSdk = ChartboostMediationAndroidSupport.chartboostMediationAndroidApiVersion
        buildConfigField(
            "String",
            "CHARTBOOST_MEDIATION_VERSION",
            "\"${ChartboostMediationSdkInfo.chartboostMediationSdkVersion}\"",
        )

        buildConfigField(
            "String",
            "CHARTBOOST_MEDIATION_GIT_HASH",
            "\"$${rootProject.ext["SHORT_GIT_HASH"]}\"",
        )

        consumerProguardFiles("proguard-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    "testImplementation"("androidx.test:core:1.5.0") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    "testImplementation"("androidx.test:core-ktx:1.5.0") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    "testImplementation"("com.google.guava:guava:32.1.2-jre")
    "testImplementation"("junit:junit:4.13.2")
    "testImplementation"("org.bouncycastle:bcprov-jdk18on:1.74")
    "testImplementation"("org.json:json:20231013")
    "testImplementation"("io.mockk:mockk:1.13.10")
    "testImplementation"("org.robolectric:robolectric:4.10.2") {
        exclude(group = "com.google.guava", module = "guava")
    }
    "testImplementation"("org.skyscreamer:jsonassert:1.5.1")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    "testImplementation"("net.javacrumbs.json-unit:json-unit-assertj:2.38.0")
    "testImplementation"("com.squareup.okhttp3:mockwebserver:4.10.0") {
        exclude(group = "com.squareup.okio", module = "okio")
    }

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    api(project(":ChartboostCore"))
    implementation("androidx.lifecycle:lifecycle-common:2.6.2")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.android.gms:play-services-base:18.4.0")
    implementation("com.google.android.gms:play-services-appset:16.0.2")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0") {
        exclude(group = "com.squareup.okio", module = "okio")
    }
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.okio:okio:3.4.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0") {
        exclude(group = "com.squareup.okio", module = "okio")
    }
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}
