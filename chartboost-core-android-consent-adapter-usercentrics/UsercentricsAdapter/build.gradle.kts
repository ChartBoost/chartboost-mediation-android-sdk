/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("com.jfrog.artifactory")
}

repositories {
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

android {
    namespace = "com.chartboost.core.consent.usercentrics"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
        // If you touch the following line, don't forget to update scripts/get_rc_version.zsh
        android.defaultConfig.versionName = System.getenv("VERSION_OVERRIDE") ?: "1.2.16.0.0"

        buildConfigField(
            "String",
            "CHARTBOOST_CORE_USERCENTRICS_ADAPTER_VERSION",
            "\"${android.defaultConfig.versionName}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "location"
    productFlavors {
        create("local")
        create("remote")
        create("candidate")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    "localImplementation"(project(":ChartboostCore"))
    // For external usage, please use the following production dependency.
    // You may choose a different release version.
    "remoteImplementation"("com.chartboost:chartboost-core-sdk:1.0.0")
    "candidateImplementation"("com.chartboost:chartboost-mediation-sdk:5.0.0")

    // Consent Management Platform SDK
    implementation("com.usercentrics.sdk:usercentrics-ui:2.16.0")

    // Adapter Dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

artifactory {
    clientConfig.isIncludeEnvVars = true
    setContextUrl("https://cboost.jfrog.io/artifactory")

    publish {
        repository {
            // If this is a release build, push to the public "chartboost-mediation" artifactory.
            // Otherwise, push to the "private-chartboost-mediation" artifactory.
            val isReleaseBuild = "true" == System.getenv("CHARTBOOST_CORE_IS_RELEASE")
            if (isReleaseBuild) {
                setRepoKey("chartboost-core")
            } else {
                setRepoKey("private-chartboost-core")
            }
            // Set the environment variables for these to be able to push to artifactory.
            System.getenv("JFROG_USER")?.let{
                setUsername(it)
            }
            System.getenv("JFROG_PASS")?.let{
                setPassword(it)
            }
        }

        defaults {
            publications("UsercentricsAdapter", "aar")
            setPublishArtifacts(true)
            setPublishPom(true)
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("remoteRelease") {
                from(components["remoteRelease"])

                groupId = "com.chartboost"
                artifactId = "chartboost-core-consent-adapter-usercentrics"
                version = if (project.hasProperty("snapshot")) {
                    android.defaultConfig.versionName + rootProject.ext["SNAPSHOT"]
                } else {
                    android.defaultConfig.versionName
                }

                pom {
                    name.set("Chartboost Core Consent Adapter Usercentrics")
                    description.set("Better monetization. Powered by bidding")
                    url.set("https://www.chartboost.com/mediate/")

                    licenses {
                        license {
                            name.set("https://answers.chartboost.com/en-us/articles/200780239")
                        }
                    }

                    developers {
                        developer {
                            id.set("chartboostmobile")
                            name.set("chartboost mobile")
                            email.set("support@chartboost.com")
                        }
                    }

                    scm {
                        val gitUrl = "https://github.com/ChartBoost/chartboost-core-android-consent-adapter-usercentrics"
                        url.set(gitUrl)
                        connection.set(gitUrl)
                        developerConnection.set(gitUrl)
                    }
                }
            }
        }
    }

    tasks.named<ArtifactoryTask>("artifactoryPublish") {
        publications(publishing.publications["remoteRelease"])
    }
}
