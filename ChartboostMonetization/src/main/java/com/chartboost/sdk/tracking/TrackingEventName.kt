package com.chartboost.sdk.tracking

// If you add a new category enum remember to update allEvents listing on the companion
// object below :)
interface TrackingEventName {
    val value: String

    enum class Cache(override val value: String) : TrackingEventName {
        IGNORED("cache_ignored"),
        START("cache_start"),
        FINISH_SUCCESS("cache_finish_success"),
        FINISH_FAILURE("cache_finish_failure"),
        GET_RESPONSE_PARSING_ERROR("cache_get_response_parsing_error"),
        BID_RESPONSE_PARSING_ERROR("cache_bid_response_parsing_error"),
        ASSET_DOWNLOAD_ERROR("cache_asset_download_error"),
        REQUEST_ERROR("cache_request_error"),
        SERVER_ERROR("cache_server_error"),
    }

    enum class Show(override val value: String) : TrackingEventName {
        START("show_start"),
        FINISH_SUCCESS("show_finish_success"),
        FINISH_FAILURE("show_finish_failure"),
        UNAVAILABLE_ASSET_ERROR("show_unavailable_asset_error"),
        TIMEOUT_EVENT("show_timeout_error"),
        HTML_MISSING_MUSTACHE_ERROR("show_html_missing_mustache_error"),
        WEBVIEW_SSL_ERROR("show_webview_ssl_error"),
        WEBVIEW_ERROR("show_webview_error"),
        WEBVIEW_CRASH("show_webview_crash"),
        UNEXPECTED_DISMISS_ERROR("show_unexpected_dismiss_error"),
        REQUEST_ERROR("show_request_error"),
        CLOSE_BEFORE_TEMPLATE_SHOW_ERROR("show_close_before_template_show_error"),
        DISMISS_MISSING("dismiss_missing"),
    }

    enum class Click(override val value: String) : TrackingEventName {
        SUCCESS("click_success"),
        FAILURE("click_failure"),
        INVALID_URL_ERROR("click_invalid_url_error"),
    }

    enum class Consent(override val value: String) : TrackingEventName {
        SUBCLASSING_ERROR("consent_subclassing_error"),
        DECODING_ERROR("consent_decoding_error"),
        CREATION_ERROR("consent_creation_error"),
        PERSISTED_DATA_READING_ERROR("consent_persisted_data_reading_error"),
        PERSISTENCE_ERROR("consent_persistence_error"),
    }

    enum class Navigation(override val value: String) : TrackingEventName {
        SUCCESS("navigation_success"),
        FAILURE("navigation_failure"),
    }

    enum class Network(override val value: String) : TrackingEventName {
        REQUEST_JSON_SERIALIZATION_ERROR("request_json_serialization_error"),
        RESPONSE_JSON_SERIALIZATION_ERROR("response_json_serialization_error"),
        RESPONSE_DATA_WRITE_ERROR("response_data_write_error"),
        DISPATCHER_EXCEPTION("network_failure_dispatcher_exception"),
    }

    enum class Video(override val value: String) : TrackingEventName {
        FINISH_SUCCESS("video_finish_success"),
        FINISH_FAILURE("video_finish_failure"),
    }

    enum class Misc(override val value: String) : TrackingEventName {
        USER_AGENT_UPDATE_ERROR("user_agent_update_error"),
        PREFETCH_REQUEST_ERROR("prefetch_request_error"),
        CONFIG_REQUEST_ERROR("config_request_error"),
        INSTALL_REQUEST_ERROR("install_request_error"),
        IMPRESSION_RECORDED("impression_recorded"),
        UNSUPPORTED_OS_VERSION("unsupported_os_version"),
        TOO_MANY_EVENTS("too_many_events"),
    }

    enum class Impression(override val value: String) : TrackingEventName {
        IMPRESSION_TRACKER_FAILURE("imptracker_failure"),
    }

    companion object {
        private val allEvents: List<TrackingEventName> by lazy {
            arrayOf(
                Cache.entries.toTypedArray(),
                Show.entries.toTypedArray(),
                Click.entries.toTypedArray(),
                Consent.entries.toTypedArray(),
                Navigation.entries.toTypedArray(),
                Network.entries.toTypedArray(),
                Video.entries.toTypedArray(),
                Misc.entries.toTypedArray(),
            ).flatten()
        }

        internal fun fromValues(values: List<String>): List<TrackingEventName> = allEvents.filter { values.contains(it.value) }
    }
}

internal fun List<String>.asTrackingEventNames(): List<TrackingEventName> = TrackingEventName.fromValues(this)
