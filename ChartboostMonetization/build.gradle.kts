import java.io.ByteArrayOutputStream

plugins {
    id("jacoco")
    id("com.android.library")
    id("maven-publish")
    id("kotlin-android")
    id("com.jfrog.artifactory")
    kotlin("plugin.allopen") version "1.8.0"
}

allOpen {
    annotation("org.mockito.Mock")
    annotation("org.mockito.InjectMocks")
    annotation("org.mockito.Captor")
    annotation("org.mockito.Spy")
    annotation("com.chartboost.sdk.OpenForTesting")
}

val getGitHash = {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    }
    stdout.toString().trim()
}

android {
    namespace = "com.chartboost.sdk"
    useLibrary("org.apache.http.legacy")
    compileSdk = 34 // rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = ChartboostMonetizationAndroidSupport.chartboostMonetizationMinimumAndroidApiVersion
        targetSdk = ChartboostMonetizationAndroidSupport.chartboostMonetizationAndroidApiVersion
        buildConfigField("String", "RELEASE_COMMIT_HASH", "\"${getGitHash()}\"")
        buildConfigField("String", "API_VERSION", "\"${ChartboostMonetizationSdkInfo.chartboostMonetizationSdkVersion}\"")
        buildConfigField("String", "SDK_VERSION", "\"${ChartboostMonetizationSdkInfo.chartboostMonetizationSdkVersion}\"")
        buildConfigField("String", "VERSION_FOR_GOOGLE", "\"${ChartboostMonetizationSdkInfo.chartboostMonetizationSdkVersion}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        javaCompileOptions {
            annotationProcessorOptions {
                arguments(mapOf("library" to "true"))
            }
        }
        base.archivesBaseName = "${base.archivesBaseName}-${ChartboostMonetizationSdkInfo.chartboostMonetizationSdkVersion}"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_PROTOCOL", "\"https\"")
            isTestCoverageEnabled = true
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_PROTOCOL", "\"https\"")
        }
    }

    flavorDimensions("version")
    productFlavors {
        create("production") {
            dimension = "version"
            buildConfigField("boolean", "DEBUG_WEBVIEW", "false")
            buildConfigField("boolean", "BYPASS_NETWORK_CHECK", "false")
        }
    }

    publishing {
        singleVariant("production")
    }

    testOptions {
        unitTests.apply {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        unitTests.all {
            it.jvmArgs("-Xshare:off")
            it.systemProperty("robolectric.dependency.repo.id", "mavenCentral")
            it.systemProperty("robolectric.dependency.repo.url", "https://repo1.maven.org/maven2")
            it.minHeapSize = "512m"
            it.maxHeapSize = "2048m"
            it.useJUnitPlatform()
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.io=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util.concurrent=ALL-UNNAMED",
        "-Dnet.bytebuddy.experimental=true",
    )

    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

dependencies {
    // Bridge for communication with SDK
    implementation(files("libs/omsdk-android-1.5.0-release.jar"))

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.android.gms:play-services-appset:16.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.7")

    // Testing libraries
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.11")

    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")

    // https://mvnrepository.com/artifact/net.bytebuddy/byte-buddy
    testImplementation("net.bytebuddy:byte-buddy:1.14.17")
//    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

    testImplementation("org.robolectric:shadows-playservices:4.10.2")
    testImplementation("org.robolectric:robolectric:4.10.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.json:json:20231013")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
    testImplementation("com.google.android.gms:play-services-auth:20.7.0") {
        exclude(group = "com.android.support")
    }
    val kotestVersion = "5.9.0"
    val kotlinVersion: String by rootProject.extra
    testImplementation("io.kotest:kotest-framework-engine-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
