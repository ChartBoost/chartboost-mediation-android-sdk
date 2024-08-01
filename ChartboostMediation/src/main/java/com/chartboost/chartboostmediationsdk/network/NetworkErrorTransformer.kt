/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
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
                ChartboostMediationError.OtherError.InternalError,
            )
        }

        val code = response.code()
        val body = response.body()

        return when {
            code == HTTP_OK -> null

            code == HTTP_NO_CONTENT -> null

            code == HTTP_NO_RESPONSE ->
                ChartboostMediationError.LoadError.InvalidBidResponse

            code < HTTP_OK || code >= HTTP_MULT_CHOICE ->
                if (body == null) {
                    ChartboostMediationError.LoadError.InvalidBidResponse
                } else {
                    ChartboostMediationError.OtherError.AdServerError
                }

            else -> ChartboostMediationError.LoadError.InvalidBidResponse
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
    val chartboostMediationError: ChartboostMediationError,
) : Throwable()
