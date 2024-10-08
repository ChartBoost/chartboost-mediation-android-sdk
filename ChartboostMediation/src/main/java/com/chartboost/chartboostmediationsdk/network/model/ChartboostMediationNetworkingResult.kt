/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.chartboostmediationsdk.network.NetworkError
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import okhttp3.Headers
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_OK

/**
 * @suppress
 */
sealed class ChartboostMediationNetworkingResult<out T> {
    class Success<out T>
        @PublishedApi
        internal constructor(
            val httpCode: Int,
            val headers: Headers,
            val body: T?,
        ) : ChartboostMediationNetworkingResult<T>()

    class JsonParsingFailure
        @PublishedApi
        internal constructor(
            val code: Int,
            val headers: Headers,
            val error: ChartboostMediationError,
            val exception: SerializationException,
        ) : ChartboostMediationNetworkingResult<Nothing>()

    class Failure
        @PublishedApi
        internal constructor(
            val code: Int,
            val headers: Headers?,
            val error: ChartboostMediationError,
            val throwable: Throwable? = null,
        ) : ChartboostMediationNetworkingResult<Nothing>()

    companion object {
        inline fun <reified T> makeResult(
            response: Response<String>,
            error: NetworkError?,
        ): ChartboostMediationNetworkingResult<T> =
            when {
                response.isSuccessful ->
                    try {
                        Success<T>(
                            httpCode = response.code(),
                            headers = response.headers(),
                            body =
                                if (response.code() == HTTP_OK) {
                                    ChartboostMediationJson.decodeFromString<T>(
                                        serializer(),
                                        response.body() ?: "",
                                    )
                                } else {
                                    null
                                },
                        )
                    } catch (serializationException: SerializationException) {
                        JsonParsingFailure(
                            code = -1,
                            headers = response.headers(),
                            error = ChartboostMediationError.OtherError.InternalError,
                            exception = serializationException,
                        )
                    } catch (throwable: Throwable) {
                        Failure(
                            code = -1,
                            headers = response.headers(),
                            error = ChartboostMediationError.OtherError.InternalError,
                            throwable = throwable,
                        )
                    }

                else ->
                    Failure(
                        code = response.code(),
                        headers = response.headers(),
                        error =
                            error?.chartboostMediationError
                                ?: ChartboostMediationError.OtherError.InternalError,
                    )
            }
    }
}
