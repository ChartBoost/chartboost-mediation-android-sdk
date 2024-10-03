package com.chartboost.sdk.internal.api

import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.ClickError
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.internal.Model.CBError

fun parseCBImpressionErrorToCacheError(error: CBError.Type): CacheError {
    val code =
        when (error) {
            CBError.Impression.INTERNET_UNAVAILABLE -> CacheError.Code.INTERNET_UNAVAILABLE
            CBError.Impression.TOO_MANY_CONNECTIONS -> CacheError.Code.NETWORK_FAILURE
            CBError.Impression.NETWORK_FAILURE -> CacheError.Code.NETWORK_FAILURE
            CBError.Impression.NO_AD_FOUND -> CacheError.Code.NO_AD_FOUND
            CBError.Impression.SESSION_NOT_STARTED -> CacheError.Code.SESSION_NOT_STARTED
            CBError.Impression.INVALID_RESPONSE -> CacheError.Code.SERVER_ERROR
            CBError.Impression.ASSETS_DOWNLOAD_FAILURE -> CacheError.Code.ASSET_DOWNLOAD_FAILURE
            CBError.Impression.ASSET_PREFETCH_IN_PROGRESS -> CacheError.Code.ASSET_DOWNLOAD_FAILURE
            CBError.Impression.ASSET_MISSING -> CacheError.Code.ASSET_DOWNLOAD_FAILURE
            CBError.Impression.INTERNET_UNAVAILABLE_AT_CACHE -> CacheError.Code.INTERNET_UNAVAILABLE
            else -> CacheError.Code.INTERNAL
        }
    return CacheError(code)
}

fun parseCBImpressionErrorToShowError(error: CBError.Impression): ShowError {
    val code =
        when (error) {
            CBError.Impression.INTERNET_UNAVAILABLE -> ShowError.Code.INTERNET_UNAVAILABLE
            CBError.Impression.NO_AD_FOUND -> ShowError.Code.NO_CACHED_AD
            CBError.Impression.SESSION_NOT_STARTED -> ShowError.Code.SESSION_NOT_STARTED
            CBError.Impression.IMPRESSION_ALREADY_VISIBLE -> ShowError.Code.AD_ALREADY_VISIBLE
            CBError.Impression.NO_HOST_ACTIVITY -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.USER_CANCELLATION -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.VIDEO_UNAVAILABLE -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.VIDEO_ID_MISSING -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.ERROR_PLAYING_VIDEO -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.ERROR_CREATING_VIEW -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.ERROR_DISPLAYING_VIEW -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.ERROR_LOADING_WEB_VIEW -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.PENDING_IMPRESSION_ERROR -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.WEB_VIEW_PAGE_LOAD_TIMEOUT -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.WEB_VIEW_CLIENT_RECEIVED_ERROR -> ShowError.Code.PRESENTATION_FAILURE
            CBError.Impression.INTERNET_UNAVAILABLE_AT_SHOW -> ShowError.Code.INTERNET_UNAVAILABLE
            else -> {
                ShowError.Code.INTERNAL
            }
        }
    return ShowError(code)
}

fun parseCBImpressionClickErrorToClickError(
    error: CBError.Click,
    errorMsg: String,
): ClickError {
    val code =
        when (error) {
            CBError.Click.URI_INVALID -> ClickError.Code.URI_INVALID
            CBError.Click.URI_UNRECOGNIZED -> ClickError.Code.URI_UNRECOGNIZED
            else -> {
                ClickError.Code.INTERNAL
            }
        }
    return ClickError(code, Exception(errorMsg))
}
