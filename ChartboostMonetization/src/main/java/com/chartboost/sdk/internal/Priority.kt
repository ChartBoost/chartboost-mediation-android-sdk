package com.chartboost.sdk.internal

enum class Priority(val value: Int) {
    /**
     * Critical request, start processing immediately
     */
    IMMEDIATE(0),

    /**
     * High impact request, start as soon as possible
     */
    HIGH(1),

    /**
     * Medium impact request
     */
    NORMAL(2),

    /**
     * Low impact request
     */
    LOW(3),
}
