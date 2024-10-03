package com.chartboost.sdk

enum class LoggingLevel {
    /** Nothing will be logged  */
    NONE,

    /** The default setting: suspected integration errors will be logged (in debug builds only)  */
    INTEGRATION,

    /** Suspected integration errors as well as diagnostic errors and messages will be logged (in debug and release builds)  */
    ALL,
}
