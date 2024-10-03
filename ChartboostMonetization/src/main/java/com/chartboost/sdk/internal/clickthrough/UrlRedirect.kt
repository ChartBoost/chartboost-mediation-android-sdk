package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Networking.CBSSLSocketFactory
import com.chartboost.sdk.internal.logging.Logger
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

private const val HEADER_LOCATION = "Location"

private const val DEFAULT_REDIRECT_LIMIT = 10

internal class UrlRedirect(
    private val urlFactory: (String) -> URL = ::URL,
    private val sslSocket: SSLSocketFactory = CBSSLSocketFactory.getSSLSocketFactory(),
) {
    fun redirect(
        url: String?,
        limit: Int = DEFAULT_REDIRECT_LIMIT,
    ): Result<String> {
        if (url.isNullOrEmpty()) {
            return Failure.EmptyOrNullUrl.asFailure()
        }

        if (limit < 0) {
            return Failure.TooManyRedirects.asFailure()
        }

        var conn: HttpsURLConnection? = null
        return try {
            val parsedURL = urlFactory(url)
            conn = parsedURL.openURLConnection()
            conn?.run {
                when {
                    responseCode.isSuccessCode() -> Result.success(url)
                    responseCode.isRedirectCode() -> {
                        var location = getHeaderField(HEADER_LOCATION)
                        // relative location - needs reconstruction happens for amazon
                        if (location.startsWith("/")) {
                            location = parsedURL.protocol + "://" + parsedURL.host + location
                        }
                        redirect(location, limit - 1)
                    }
                    else -> Failure.HttpErrorCode(responseCode).asFailure()
                }
            } ?: Failure.NullConnection.asFailure()
        } catch (e: Exception) {
            Logger.e("Cannot redirect $url", e)
            // here we need to return the last url as it might be valid url but with custom schema
            Failure.UncontrolledError(url, e).asFailure()
        } finally {
            conn?.disconnect()
        }
    }

    private fun Failure.asFailure(): Result<String> = Result.failure(this)

    private fun Int.isRedirectCode(): Boolean = this in HttpStatus.REDIRECTION_START.code..HttpStatus.REDIRECTION_END.code

    private fun Int.isSuccessCode(): Boolean = this in HttpStatus.REQUEST_SUCCESS_START.code..HttpStatus.REQUEST_SUCCESS_END.code

    private fun URL.openURLConnection(): HttpsURLConnection? =
        (openConnection() as? HttpsURLConnection)?.apply {
            sslSocketFactory = sslSocket
            instanceFollowRedirects = false
            connectTimeout = CBConstants.REQUEST_TIME_OUT
            readTimeout = CBConstants.REQUEST_TIME_OUT
        }

    sealed class Failure(message: String, cause: Throwable? = null) : Exception(message, cause) {
        object EmptyOrNullUrl : Failure("Empty or null URL") {
            private fun readResolve(): Any = EmptyOrNullUrl
        }

        object TooManyRedirects : Failure("Too many redirects") {
            private fun readResolve(): Any = TooManyRedirects
        }

        object NullConnection : Failure("Returned connection is null") {
            private fun readResolve(): Any = NullConnection
        }

        class HttpErrorCode(val statusCode: Int) : Failure("Failed with HTTP code $statusCode")

        class UncontrolledError(val url: String, cause: Throwable) : Failure("Uncontrolled error", cause) {
            override fun toString(): String = cause?.toString() ?: "No cause"
        }

        override fun toString(): String = message ?: "No message"
    }
}

private enum class HttpStatus(val code: Int) {
    REQUEST_SUCCESS_START(200),
    REQUEST_SUCCESS_END(299),
    REDIRECTION_START(300),
    REDIRECTION_END(399),
}
