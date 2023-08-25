/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.domain.ChartboostMediationError
import retrofit2.Response
import java.net.HttpURLConnection.*

/**
 * @suppress
 */
object NetworkErrorTransformer {
    // This is not already defined in HttpURLConnection
    private const val HTTP_NO_RESPONSE = -1

    fun <T> transform(response: Response<T>?): NetworkError? {
        if (response == null) {
            return NetworkError(
                code = -1,
                ChartboostMediationError.CM_INTERNAL_ERROR
            )
        }

        val code = response.code()
        val body = response.body()

        return when {
            code == HTTP_OK -> null
            code == HTTP_NO_CONTENT -> null
            code == HTTP_NO_RESPONSE ->
                ChartboostMediationError.CM_LOAD_FAILURE_INVALID_BID_RESPONSE
            code < HTTP_OK || code >= HTTP_MULT_CHOICE ->
                if (body == null) {
                    ChartboostMediationError.CM_LOAD_FAILURE_INVALID_BID_RESPONSE
                } else {
                    ChartboostMediationError.CM_AD_SERVER_ERROR
                }
            else -> ChartboostMediationError.CM_LOAD_FAILURE_INVALID_BID_RESPONSE
        }?.let {
            NetworkError(code, it)
        }
    }
}

/**
 * @suppress
 */
data class NetworkError(
    val code: Int,
    val chartboostMediationError: ChartboostMediationError
) : Throwable()
