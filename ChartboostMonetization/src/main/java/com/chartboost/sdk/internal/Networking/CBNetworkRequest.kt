package com.chartboost.sdk.internal.Networking

import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Priority
import java.io.File
import java.util.concurrent.atomic.AtomicReference

open class CBNetworkRequest<T>(
    val method: Method,
    var uri: String,
    val priority: Priority,
    @JvmField
    val outputFile: File?,
) {
    @JvmField
    val status: AtomicReference<Status> = AtomicReference(Status.QUEUED)

    @JvmField
    var processingNs: Long = 0

    @JvmField
    var getResponseCodeNs: Long = 0

    @VisibleForTesting
    @JvmField
    var readDataNs: Long = 0

    @JvmField
    var dispatch: Dispatch = Dispatch.UI

    /*
        buildRequestInfo() is called within a network dispatcher thread,
        right before submitting the request.

        We use this to look up the identifiers, which cannot happen on the UI thread.

        Also, we calculate the request signature here, since that has to happen
        after the identifiers are updated in the body.  As a bonus, that means the
        hashing happens on a background thread, rather than the UI thread.

        I welcome suggestions for a better name.
     */
    open fun buildRequestInfo(): CBNetworkRequestInfo? {
        return CBNetworkRequestInfo(null, null, null)
    }

    // consider returning T and throwing on errors
    // Consider returning Result<T>
    open fun parseServerResponse(serverResponse: CBNetworkServerResponse?): CBNetworkRequestResult<T?>? {
        return CBNetworkRequestResult.success(null)
    }

    open fun deliverResponse(
        response: T,
        serverResponse: CBNetworkServerResponse?,
    ) {}

    open fun deliverError(
        error: CBError?,
        serverResponse: CBNetworkServerResponse?,
    ) {}

    // Cancel the request if it hasn't already started being downloaded.
    fun cancel(): Boolean {
        return status.compareAndSet(Status.QUEUED, Status.CANCELED)
    }

    open fun notifyTempFileIsReady(
        uri: String,
        contentSize: Long,
    ) {}

    enum class Method {
        GET,
        POST,
    }

    enum class Status {
        CANCELED,
        QUEUED,
        PROCESSING,
    }

    enum class Dispatch {
        UI,
        ASYNC,
    }

    companion object {
        const val API_ENDPOINT_CONFIG = "/api/config"
        const val API_ENDPOINT_INSTALL = "/api/install"
        const val API_ENDPOINT_PREFETCH = "/prefetch"
        const val API_ENDPOINT_INTERSTITIAL_GET = "/interstitial/get"
        const val API_ENDPOINT_INTERSTITIAL_SHOW = "/interstitial/show"
        const val API_ENDPOINT_REWARD_GET = "/reward/get"
        const val API_ENDPOINT_REWARD_SHOW = "/reward/show"
        const val API_ENDPOINT_BANNER_GET = "/auction/sdk/banner"
        const val API_ENDPOINT_BANNER_SHOW = "/banner/show"
        const val API_ENDPOINT_CLICK = "/api/click"
        const val API_ENDPOINT_VIDEO_COMPLETE = "/api/video-complete"
    }
}
