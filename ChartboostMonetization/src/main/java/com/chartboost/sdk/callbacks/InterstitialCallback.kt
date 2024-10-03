package com.chartboost.sdk.callbacks

/**
 * InterstitialCallback
 * Provides methods to receive notifications related to the behavior of an interstitial ad.
 * In a typical integration, you would implement `willShowAd`, `didShowAd`, and `didDismissAd`,
 * pausing and resuming ongoing processes (e.g., gameplay, video) accordingly.
 */
interface InterstitialCallback : DismissibleAdCallback
