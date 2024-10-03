package com.chartboost.sdk.internal.Model

class CBError(
    val type: Type,
    val errorDesc: String,
) : Exception(errorDesc) {
    sealed interface Type {
        val name: String
    }

    enum class Impression : Type {
        /**
         * An error internal to the Chartboost SDK
         */
        INTERNAL,

        /**
         * No internet connection was found
         */
        INTERNET_UNAVAILABLE,

        /**
         * Too many simultaneous requests
         * eg more than one show request, more than one cache request of the same type and location
         */
        TOO_MANY_CONNECTIONS,

        /**
         * The impression sent was not compatible with the device orientation
         */
        WRONG_ORIENTATION,

        /**
         * This is the first user session and interstitials are disabled in the first session
         */
        FIRST_SESSION_INTERSTITIALS_DISABLED,

        /**
         * An error occurred during network communication with the Chartboost server
         */
        NETWORK_FAILURE,

        /**
         * No ad was available for the user from the Chartboost server
         */
        NO_AD_FOUND,

        /**
         * The startSession() method was not called as per implementation instructions
         */
        SESSION_NOT_STARTED,

        /**
         * There is already an impression visible or in the process of loading to be displayed
         */
        IMPRESSION_ALREADY_VISIBLE,

        /**
         * There is no currently active activity with Chartboost properly integrated
         */
        NO_HOST_ACTIVITY,

        /**
         * User cancels the alert notification pop-up
         */
        USER_CANCELLATION,

        /**
         * Invalid location
         */
        INVALID_LOCATION,

        /**
         * Video not available in cache
         */
        VIDEO_UNAVAILABLE,

        /**
         * Video url missing in response
         */
        VIDEO_ID_MISSING,

        /**
         * Error playing video
         */
        ERROR_PLAYING_VIDEO,

        /**
         * Invalid response
         */
        INVALID_RESPONSE,

        /**
         * Error downloading assets
         */
        ASSETS_DOWNLOAD_FAILURE,

        /**
         * Error while creating views
         */
        ERROR_CREATING_VIEW,

        /**
         * Error when trying to display view
         */
        ERROR_DISPLAYING_VIEW,

        /**
         * API Version is incompatible
         */
        INCOMPATIBLE_API_VERSION,

        /**
         * Error loading Web view
         */
        ERROR_LOADING_WEB_VIEW,

        /**
         * Asset prefetch in progress
         */
        ASSET_PREFETCH_IN_PROGRESS,

        /**
         * Missing activity
         */
        ACTIVITY_MISSING_IN_MANIFEST,

        /**
         * Empty video list
         */
        EMPTY_LOCAL_VIDEO_LIST,

        /**
         * End Point Disabled
         */
        END_POINT_DISABLED,

        /**
         * Hardware Acceleration Disabled
         */
        HARDWARE_ACCELERATION_DISABLED,

        /**
         * Pending impression error
         */
        PENDING_IMPRESSION_ERROR,

        /**
         * Response do not include video for the current orientation
         */
        VIDEO_UNAVAILABLE_FOR_CURRENT_ORIENTATION,

        /**
         * a previously-download asset was missing at show time
         */
        ASSET_MISSING,

        /**
         * A WebView took longer than 3 seconds to load
         */
        WEB_VIEW_PAGE_LOAD_TIMEOUT,

        /**
         * The WebViewClient onReceivedError method was called
         */
        WEB_VIEW_CLIENT_RECEIVED_ERROR,

        /**
         * No internet connection during Ad display with cached Ad
         */
        INTERNET_UNAVAILABLE_AT_SHOW,

        /**
         * No internet connection during Ad cache
         */
        INTERNET_UNAVAILABLE_AT_CACHE,
    }

    enum class Click : Type {
        /**
         * Invalid URI
         */
        URI_INVALID,

        /**
         * The device does not know how to open the protocol of the URI
         */
        URI_UNRECOGNIZED,

        /**
         * The current ad has not finished loading
         */
        LOAD_NOT_FINISHED,

        /**
         * Unknown internal error
         */
        INTERNAL,
    }

    enum class Internal : Type {
        MISCELLANEOUS,
        INTERNET_UNAVAILABLE,
        INVALID_RESPONSE,
        UNEXPECTED_RESPONSE,
        NETWORK_FAILURE,
        HTTP_NOT_FOUND,
        HTTP_NOT_OK,
        UNSUPPORTED_OS_VERSION,
    }

    val impressionError: Impression
        get() =
            when (type) {
                Internal.INTERNET_UNAVAILABLE -> Impression.INTERNET_UNAVAILABLE
                Internal.HTTP_NOT_FOUND -> Impression.NO_AD_FOUND
                Internal.INVALID_RESPONSE -> Impression.INVALID_RESPONSE
                Internal.NETWORK_FAILURE -> Impression.NETWORK_FAILURE
                else -> Impression.INTERNAL
            }
}
