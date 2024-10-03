package com.chartboost.sdk.internal.Libraries

enum class Orientation {
    /**
     * default portrait orientation on a portrait device
     */
    PORTRAIT,

    /**
     * default landscape orientation on a landscape device
     */
    LANDSCAPE,

    /**
     * reverse portrait orientation on a portrait device
     */
    PORTRAIT_REVERSE,

    /**
     * reverse landscape orientation on a landscape device
     */
    LANDSCAPE_REVERSE,

    /**
     * left portrait orientation on a landscape device (alias for PORTRAIT_REVERSE)
     */
    PORTRAIT_LEFT,

    /**
     * right portrait orientation on a landscape device (alias for PORTRAIT)
     */
    PORTRAIT_RIGHT,

    /**
     * left landscape orientation on a portrait device (alias for LANDSCAPE)
     */
    LANDSCAPE_LEFT,

    /**
     * right landscape orientation on a portrait device (alias for LANDSCAPE_REVERSE)
     */
    LANDSCAPE_RIGHT,
}
