package com.chartboost.sdk.internal.video

/**
 * Callback that handles notification for network temp file readiness
 */
fun interface AdUnitVideoPrecacheTemp {
    fun tempVideoFileIsReady(url: String)
}
