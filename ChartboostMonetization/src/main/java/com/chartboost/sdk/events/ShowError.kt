package com.chartboost.sdk.events

/**
 * ShowError
 * An error object passed in show-related callbacks.
 */
class ShowError internal constructor(
    val code: Code,
    override val exception: Exception? = null,
) : CBError {
    /**
     * Error code that indicates the failure reason.
     */
    enum class Code(val errorCode: Int) {
        INTERNAL(0),
        SESSION_NOT_STARTED(7),
        AD_ALREADY_VISIBLE(8),
        INTERNET_UNAVAILABLE(25),
        PRESENTATION_FAILURE(33),
        NO_CACHED_AD(34),
        BANNER_DISABLED(36),
        BANNER_VIEW_IS_DETACHED(37),
    }

    override fun toString(): String {
        return "Chartboost ShowError: ${code.name} with exception $exception"
    }
}
