/*
 * Copyright 2022-2023 Chartboost, Inc.
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
    namespace = "com.chartboost.heliumsdk"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    compileSdk = HeliumAndroidSupport.heliumAndroidApiVersion
    buildToolsVersion = HeliumAndroidSupport.heliumBuildToolsVersion

    defaultConfig {
        minSdk = HeliumAndroidSupport.heliumMinimumAndroidApiVersion
        targetSdk = HeliumAndroidSupport.heliumAndroidApiVersion
        buildConfigField(
            "String",
            "CHARTBOOST_MEDIATION_VERSION",
            "\"${HeliumSdkInfo.heliumSdkVersion}\""
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
    }
}

dependencies {
    "testImplementation"("androidx.test:core:1.5.0")
    "testImplementation"("androidx.test:core-ktx:1.5.0")
    "testImplementation"("junit:junit:4.13.2")
    "testImplementation"("org.json:json:20230227")
    "testImplementation"("org.powermock:powermock-api-mockito2:2.0.9")
    "testImplementation"("org.powermock:powermock-module-junit4-rule-agent:2.0.9")
    "testImplementation"("org.powermock:powermock-module-junit4-rule:2.0.9")
    "testImplementation"("org.powermock:powermock-module-junit4:2.0.9")
    "testImplementation"("org.jmockit:jmockit:1.49")
    "testImplementation"("io.mockk:mockk:1.13.3")
    "testImplementation"("org.robolectric:robolectric:4.10.2")
    "testImplementation"("org.skyscreamer:jsonassert:1.5.1")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    "testImplementation"("net.javacrumbs.json-unit:json-unit-assertj:2.38.0")
    "testImplementation"("com.squareup.okhttp3:mockwebserver:4.10.0")

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.android.gms:play-services-base:18.1.0")
    implementation("com.google.android.gms:play-services-appset:16.0.2")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}
