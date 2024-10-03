package com.chartboost.sdk.internal.Networking

import android.content.Context
import com.chartboost.sdk.internal.Networking.requests.NetworkType
import com.chartboost.sdk.internal.logging.Logger.d

class CBReachability(private val context: Context) {
    /**
     * Network availability getter method
     *
     * @return true if network is available (although there is NO GUARANTEE of internet connectivity)
     */
    val isNetworkAvailable: Boolean
        get() = context.isNetworkConnected()

    /**
     * Retrieve connection type from active network
     */
    fun connectionTypeFromActiveNetwork(): ConnectionType {
        return with(context) {
            if (!isNetworkConnected()) {
                ConnectionType.CONNECTION_ERROR
            } else if (isWifiConnected()) {
                ConnectionType.CONNECTION_WIFI
            } else if (isMobileConnected()) {
                ConnectionType.CONNECTION_MOBILE
            } else {
                ConnectionType.CONNECTION_UNKNOWN
            }
        }.also {
            d("NETWORK TYPE: $it")
        }
    }

    /**
     * Return OpenRTB expected values for connection type
     * Unknown = 0
     * Ethernet = 1
     * WIFI = 2
     * Cellular_Unknown = 3
     * Cellular_2G = 4
     * Cellular_3G = 5
     * Cellular_4G = 6
     * Cellular_5G = 7
     *
     * @return @NetworkType.OpenRtbConnectionType int
     */
    fun openRTBConnectionType(): NetworkType = context.openRTBConnectionType()

    fun connectionTypeAsString(): String = context.openRTBConnectionType().asString

    fun cellularConnectionType(): Int = context.networkConnectionType()

    /**
     * Check if connection is Cellular eg. 3G, 4G
     */
    fun isConnectionCellular(): Boolean = connectionTypeFromActiveNetwork() == ConnectionType.CONNECTION_MOBILE
}
