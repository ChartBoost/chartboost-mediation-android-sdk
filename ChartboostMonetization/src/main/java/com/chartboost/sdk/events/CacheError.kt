package com.chartboost.sdk.events

/**
 * CacheError
 * An error object passed in cache-related callbacks.
 */
class CacheError internal constructor(
    val code: Code,
    override val exception: Exception? = null,
) : CBError {
    /**
     * Error code that indicates the failure reason.
     */
    enum class Code(val errorCode: Int) {
        INTERNAL(0),
        INTERNET_UNAVAILABLE(1),
        NETWORK_FAILURE(5),
        NO_AD_FOUND(6),
        SESSION_NOT_STARTED(7),
        SERVER_ERROR(8),
        ASSET_DOWNLOAD_FAILURE(16),
        BANNER_DISABLED(36),
        BANNER_VIEW_IS_DETACHED(37),
    }

    override fun toString(): String {
        return "Chartboost CacheError: ${code.name} with exception $exception"
    }
}
