/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

object ChartboostMonetizationSdkInfo {
    const val chartboostMonetizationName = "Chartboost Ads SDK"
    var chartboostMonetizationSdkVersion = "9.7.0"
    public var isReleaseBuild = false
        internal set
}

object ChartboostMonetizationAndroidSupport {
    const val chartboostMonetizationAndroidApiVersion = 34
    const val chartboostMonetizationMinimumAndroidApiVersion = 19
}

object ChartboostMonetizationPomInfo {
    // POM CONSTANTS
    const val DESCRIPTION = "Chartboost Android SDK"
    const val DEVELOPER_EMAIL = "mobile@chartboost.com"
    const val DEVELOPER_ID = "chartboostmobile"
    const val DEVELOPER_NAME = "chartboost mobile"
    const val GIT_URL = "https://github.com/ChartBoost/android-sdk"
    const val CHARTBOOST_MONETIZATION_URL = "https://chartboost.com"
    const val LICENSE = "https://answers.chartboost.com/en-us/articles/200780239"

    // SNAPSHOT CONSTANT
    const val SNAPSHOT = "-SNAPSHOT"
}
