package com.chartboost.sdk.internal.Networking.requests

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.CBUtility
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkRequestInfo
import com.chartboost.sdk.internal.Networking.CBNetworkServerResponse
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import java.io.File

class VideoRequest(
    private val reachability: CBReachability?,
    outputFile: File,
    uri: String,
    private val callback: VideoRequestCallback?,
    priority: Priority = Priority.NORMAL,
    private val appId: String,
) : CBNetworkRequest<Any?>(Method.GET, uri, priority, outputFile) {
    init {
        dispatch = Dispatch.ASYNC
    }

    override fun buildRequestInfo(): CBNetworkRequestInfo {
        val headers: MutableMap<String, String> = HashMap()
        headers[CBConstants.REQUEST_PARAM_APP_HEADER_KEY] = appId
        headers[CBConstants.REQUEST_PARAM_CLIENT_HEADER_KEY] = CBUtility.getUserAgent()
        headers[CBConstants.REQUEST_PARAM_REACHABILITY_HEADER_KEY] =
            reachability?.connectionTypeFromActiveNetwork().toString()
        return CBNetworkRequestInfo(headers, null, null)
    }

    override fun deliverResponse(
        response: Any?,
        serverResponse: CBNetworkServerResponse?,
    ) {
        callback?.onSuccess(uri, outputFile!!.name)
    }

    override fun deliverError(
        error: CBError?,
        serverResponse: CBNetworkServerResponse?,
    ) {
        callback?.onError(uri, outputFile!!.name, error)
    }

    override fun notifyTempFileIsReady(
        uri: String,
        contentSize: Long,
    ) {
        callback?.tempFileIsReady(uri, outputFile!!.name, contentSize, null)
    }

    interface VideoRequestCallback {
        fun tempFileIsReady(
            url: String,
            videoFileName: String,
            expectedContentSize: Long,
            adUnitVideoPrecacheTempCallback: AdUnitVideoPrecacheTemp?,
        )

        fun onSuccess(
            uri: String,
            videoFileName: String,
        )

        fun onError(
            uri: String,
            videoFileName: String,
            error: CBError?,
        )
    }
}
