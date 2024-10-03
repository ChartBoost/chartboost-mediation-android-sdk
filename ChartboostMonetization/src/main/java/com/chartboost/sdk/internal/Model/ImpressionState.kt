package com.chartboost.sdk.internal.Model

enum class ImpressionState(val value: Int) {
    LOADING(0),
    LOADED(1),
    DISPLAYED(2),
    CACHED(3),
    DISMISSING(4),
    NONE(5),
}
