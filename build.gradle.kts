import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jetbrains.dokka") version "1.8.20"
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
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
        classpath("org.jetbrains.dokka:dokka-base:1.8.20") {
            exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
        }
        classpath("com.fasterxml.jackson.core:jackson-databind:2.14.1")

    }
}

project(":Helium") {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.getkeepsafe.dexcount")
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    ////
    // -- Publishing
    ////
    val groupProjectID = "com.chartboost"
    val artifactProjectID = "chartboost-mediation-sdk"

    artifactory {
        clientConfig.isIncludeEnvVars = true
        setContextUrl("https://cboost.jfrog.io/artifactory")

        publish {
            repository {
                // If this is a release build, push to the public "helium" artifactory.
                // Otherwise, push to the "private-helium" artifactory.
                val isReleaseBuild = "true" == System.getenv("CHARTBOOST_MEDIATION_IS_RELEASE")
                if (isReleaseBuild) {
                    setRepoKey("chartboost-mediation")
                } else {
                    setRepoKey("private-chartboost-mediation")
                }
                // Set the environment variables for these to be able to push to artifactory.
                setUsername(System.getenv("JFROG_USER"))
                setPassword(System.getenv("JFROG_PASS"))
            }

            defaults {
                publications("HeliumRelease")
                setPublishArtifacts(true)
                setPublishPom(true)
            }
        }
    }

    afterEvaluate {
        this.afterEvaluate {
            publishing {
                publications {
                    register<MavenPublication>("HeliumRelease") {
                        from(components["release"])

                        groupId = groupProjectID
                        artifactId = artifactProjectID
                        version = if (project.hasProperty("snapshot")) {
                            HeliumSdkInfo.heliumSdkVersion + rootProject.ext["SNAPSHOT"]
                        } else {
                            HeliumSdkInfo.heliumSdkVersion
                        }

                        pom {
                            name.set(HeliumSdkInfo.heliumName)
                            description.set(HeliumPomInfo.DESCRIPTION)
                            url.set(HeliumPomInfo.HELIUM_URL)

                            licenses {
                                license {
                                    name.set(HeliumPomInfo.LICENSE)
                                }
                            }

                            developers {
                                developer {
                                    id.set(HeliumPomInfo.DEVELOPER_ID)
                                    name.set(HeliumPomInfo.DEVELOPER_NAME)
                                    email.set(HeliumPomInfo.DEVELOPER_EMAIL)
                                }
                            }

                            scm {
                                url.set(HeliumPomInfo.GIT_URL)
                                connection.set(HeliumPomInfo.GIT_URL)
                                developerConnection.set(HeliumPomInfo.GIT_URL)
                            }
                        }
                    }
                }
            }

            tasks.named<org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask>("artifactoryPublish") {
                publications(publishing.publications.getByName("HeliumRelease"))
            }

            tasks.named("assemble") {
                finalizedBy("artifactoryPublish")
            }
        }
    }

}

allprojects {
    repositories {
        google()
        mavenCentral()

        maven("https://cboost.jfrog.io/artifactory/chartboost-mediation/") {
            name = "Chartboost Mediation's Production Repo"
        }
        maven("https://cboost.jfrog.io/artifactory/private-chartboost-mediation/") {
            name = "Chartboost Mediation's Private Repo"
            credentials {
                System.getenv("JFROG_USER")?.let {
                    username = it
                }
                System.getenv("JFROG_PASS")?.let {
                    password = it
                }
            }
        }
    }
}

ext {
    val isNightlyBuild = "true" == System.getenv("CHARTBOOST_MEDIATION_IS_NIGHTLY_BUILD")
    val isReleaseBuild = "true" == System.getenv("CHARTBOOST_MEDIATION_IS_RELEASE")
    val heliumReleaseCandidate by rootProject.extra { System.getenv("CHARTBOOST_MEDIATION_RELEASE_CANDIDATE") }

    HeliumSdkInfo.heliumSdkVersion = if (isNightlyBuild) {
        if (isReleaseBuild) {
            throw Throwable("Can't be nightly and release build!")
        }
        getNightlyVersionString()
    } else {
        if (heliumReleaseCandidate == null || heliumReleaseCandidate.isBlank()) HeliumSdkInfo.heliumSdkVersion else heliumReleaseCandidate
    }
}

task<Delete>("delete") {
    delete(rootProject.buildDir)
}

task("ci") {
    // first, try things that fail quickly, like test and lint.
    // If those succeed, then try things that take longer, like assemble
    dependsOn("clean")
    dependsOn(":Helium:lint")
    dependsOn(":Helium:build")
    dependsOn(":HeliumCanary:assembleLocal")
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            sourceRoots.setFrom(files("Helium/src/main/java"))
            displayName.set(name)
            documentedVisibilities.set(setOf(org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC))
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(false)
            suppressInheritedMembers.set(true)

            // Other options and their definitions can be found at https://kotlinlang.org/docs/dokka-gradle.html#source-set-configuration
        }
    }
}

fun getNightlyVersionString(): String {
    val now = Instant.now()
    val yearMonthDayFormat =
        DateTimeFormatter.ofPattern("yyyy.M.d").withZone(TimeZone.getDefault().toZoneId())
    return "${yearMonthDayFormat.format(now)}.${now.epochSecond}-nightly"
}
