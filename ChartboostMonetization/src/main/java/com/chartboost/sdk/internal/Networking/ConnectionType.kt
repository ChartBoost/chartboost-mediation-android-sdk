package com.chartboost.sdk.internal.Networking

/**
 * Enum class to indicate the type of network available on the device.
 */
enum class ConnectionType(val value: Int) {
    CONNECTION_UNKNOWN(-1),
    CONNECTION_ERROR(0),
    CONNECTION_WIFI(1),
    CONNECTION_MOBILE(2),
}
