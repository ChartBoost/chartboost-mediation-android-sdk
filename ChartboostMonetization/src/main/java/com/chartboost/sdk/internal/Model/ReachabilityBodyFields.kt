package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.internal.Networking.requests.NetworkType

data class ReachabilityBodyFields(
    val cellularConnectionType: Int? = null,
    val connectionTypeFromActiveNetwork: Int? = 0,
    val detailedConnectionType: String? = null,
    val openRTBConnectionType: NetworkType = NetworkType.UNKNOWN,
)
