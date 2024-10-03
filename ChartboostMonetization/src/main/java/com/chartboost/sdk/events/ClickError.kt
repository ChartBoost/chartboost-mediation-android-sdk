package com.chartboost.sdk.events

/**
 * ClickError
 * An error object passed in click-related callbacks.
 */
class ClickError internal constructor(
    val code: Code,
    override val exception: Exception? = null,
) : CBError {
    /**
     * Error code that indicates the failure reason.
     */
    enum class Code(val errorCode: Int) {
        INTERNAL(0),
        URI_INVALID(1),
        URI_UNRECOGNIZED(2),
    }

    override fun toString(): String {
        return "Chartboost ClickError: ${code.name} with exception $exception"
    }
}
