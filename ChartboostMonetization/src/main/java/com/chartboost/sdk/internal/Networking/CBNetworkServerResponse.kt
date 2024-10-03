package com.chartboost.sdk.internal.Networking

import javax.net.ssl.HttpsURLConnection

// TODO Internal visibility
// Intended to be treated as immutable
data class CBNetworkServerResponse(
    val statusCode: Int,
    val data: ByteArray,
) {
    // 200 to 299
    fun isStatusOk() = statusCode >= HttpsURLConnection.HTTP_OK && statusCode < HttpsURLConnection.HTTP_MULT_CHOICE
}
