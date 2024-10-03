package com.chartboost.sdk.internal.adType

import com.chartboost.sdk.internal.Networking.EndpointRepository

sealed class AdType(
    val name: String,
    val getEndPoint: EndpointRepository.EndPoint,
    val showEndPoint: EndpointRepository.EndPoint,
    val shouldDisplayOnHostView: Boolean = false,
    val canBeClosed: Boolean = true,
) {
    val isFullScreen: Boolean = !shouldDisplayOnHostView

    data object Interstitial : AdType(
        name = "Interstitial",
        getEndPoint = EndpointRepository.EndPoint.INTERSTITIAL_GET,
        showEndPoint = EndpointRepository.EndPoint.INTERSTITIAL_SHOW,
    )

    data object Rewarded : AdType(
        name = "Rewarded",
        getEndPoint = EndpointRepository.EndPoint.REWARDED_GET,
        showEndPoint = EndpointRepository.EndPoint.REWARDED_SHOW,
        canBeClosed = false,
    )

    data object Banner : AdType(
        name = "Banner",
        getEndPoint = EndpointRepository.EndPoint.BANNER_GET,
        showEndPoint = EndpointRepository.EndPoint.BANNER_SHOW,
        shouldDisplayOnHostView = true,
    )
}

internal fun String.toAdType(): AdType? {
    return when (this) {
        AdType.Interstitial.name -> AdType.Interstitial
        AdType.Rewarded.name -> AdType.Rewarded
        AdType.Banner.name -> AdType.Banner
        else -> null
    }
}
