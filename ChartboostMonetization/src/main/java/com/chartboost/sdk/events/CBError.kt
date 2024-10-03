package com.chartboost.sdk.events

/**
 * CBError
 * The base class from which all Chartboost errors inherit from.
 * Error objects are passed as parameters to some callbacks to indicate that an operation
 * has failed and provide context on why.
 */
sealed interface CBError {
    /**
     * The system exception that triggered the CBError, if any.
     */
    val exception: Exception?
}
