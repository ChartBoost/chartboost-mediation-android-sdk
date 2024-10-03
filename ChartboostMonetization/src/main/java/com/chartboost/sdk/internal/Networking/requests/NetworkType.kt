package com.chartboost.sdk.internal.Networking.requests

enum class NetworkType(val value: Int, val asString: String) {
    UNKNOWN(0, "Unknown"),
    ETHERNET(1, "Ethernet"),
    WIFI(2, "WIFI"),
    CELLULAR_UNKNOWN(3, "Cellular_Unknown"),
    CELLULAR_2G(4, "Cellular_2G"),
    CELLULAR_3G(5, "Cellular_3G"),
    CELLULAR_4G(6, "Cellular_4G"),
    CELLULAR_5G(7, "Cellular_5G"),
}
