/*
 * Copyright 2024 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "UnmanagedAdapter"
include(":UnmanagedAdapter")
include(":chartboostcore")
include(":ChartboostCore")
