package com.chartboost.sdk.internal.Networking

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.SdkConfiguration
import java.net.URL

private const val DEFAULT_WEBVIEW_PREFIX = "/webview/v2"

interface EndpointRepository {
    fun setEndpoint(
        endPoint: EndPoint,
        host: String,
        path: String,
    )

    fun getEndPointUrl(endPoint: EndPoint): URL

    fun restoreDefaults()

    enum class EndPoint(val defaultValue: String) {
        CONFIG(CBNetworkRequest.API_ENDPOINT_CONFIG),
        INSTALL(CBNetworkRequest.API_ENDPOINT_INSTALL),
        PREFETCH("$DEFAULT_WEBVIEW_PREFIX${CBNetworkRequest.API_ENDPOINT_PREFETCH}"),
        INTERSTITIAL_GET("$DEFAULT_WEBVIEW_PREFIX${CBNetworkRequest.API_ENDPOINT_INTERSTITIAL_GET}"),
        INTERSTITIAL_SHOW("$DEFAULT_WEBVIEW_PREFIX${CBNetworkRequest.API_ENDPOINT_INTERSTITIAL_SHOW}"),
        REWARDED_GET("$DEFAULT_WEBVIEW_PREFIX${CBNetworkRequest.API_ENDPOINT_REWARD_GET}"),
        REWARDED_SHOW("$DEFAULT_WEBVIEW_PREFIX${CBNetworkRequest.API_ENDPOINT_REWARD_SHOW}"),
        BANNER_GET(CBNetworkRequest.API_ENDPOINT_BANNER_GET),
        BANNER_SHOW(CBNetworkRequest.API_ENDPOINT_BANNER_SHOW),
        CLICK(CBNetworkRequest.API_ENDPOINT_CLICK),
        VIDEO_COMPLETE(CBNetworkRequest.API_ENDPOINT_VIDEO_COMPLETE),
    }

    enum class DefaultHosts(val defaultValue: String) {
        AD_GET("live.chartboost.com"),
        DA("da.chartboost.com"),
    }
}

internal open class EndpointRepositoryBase(
    private val sdkConfiguration: SdkConfiguration,
) : EndpointRepository {
    private val EndpointRepository.EndPoint.configUrl: URL?
        get() =
            when (this) {
                EndpointRepository.EndPoint.INTERSTITIAL_GET ->
                    CBConstants.API_ENDPOINT_WEBVIEW_INTERSTITIAL_GET_FORMAT
                        .format(sdkConfiguration.webviewVersion)
                        .let {
                            configUrl(it)
                        }

                EndpointRepository.EndPoint.REWARDED_GET ->
                    CBConstants.API_ENDPOINT_WEBVIEW_REWARD_GET_FORMAT
                        .format(sdkConfiguration.webviewVersion)
                        .let {
                            configUrl(it)
                        }

                EndpointRepository.EndPoint.PREFETCH ->
                    configUrl(sdkConfiguration.webviewPrefetchEndpoint)

                else -> null
            }

    private fun EndpointRepository.EndPoint.configUrl(configEndpoint: String): URL =
        URL(
            CBConstants.API_PROTOCOL,
            defaultHost,
            "/$configEndpoint",
        )

    override fun setEndpoint(
        endPoint: EndpointRepository.EndPoint,
        host: String,
        path: String,
    ) {
        error("Cannot set endpoint")
    }

    override fun getEndPointUrl(endPoint: EndpointRepository.EndPoint): URL = endPoint.configUrl ?: endPoint.defaultUrl

    // Resets host and all endpoints to their default values
    override fun restoreDefaults() {}
}

internal val EndpointRepository.EndPoint.defaultUrl: URL
    get() = URL(CBConstants.API_PROTOCOL, defaultHost, defaultValue)

internal val EndpointRepository.EndPoint.defaultHost: String
    get() =
        when (this) {
            EndpointRepository.EndPoint.BANNER_GET -> EndpointRepository.DefaultHosts.DA.defaultValue
            else -> EndpointRepository.DefaultHosts.AD_GET.defaultValue
        }

internal val URL.cbRequestHost
    get() = "$protocol://$host"
