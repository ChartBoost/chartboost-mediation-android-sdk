package com.chartboost.sdk.events

/**
 * StartError
 * An error object passed in SDK start completion callbacks.
 */
class StartError internal constructor(val code: Code, override val exception: Exception? = null) :
    CBError {
        /**
         * Error code that indicates the failure reason.
         */
        enum class Code(val errorCode: Int) {
            INVALID_CREDENTIALS(0),
            NETWORK_FAILURE(1),
            SERVER_ERROR(2),
            INTERNAL(3),
        }

        override fun toString(): String {
            return "Chartboost StartError: ${code.name} with exception $exception"
        }
    }
