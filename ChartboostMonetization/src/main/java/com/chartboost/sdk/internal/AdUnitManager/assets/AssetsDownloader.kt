package com.chartboost.sdk.internal.AdUnitManager.assets

import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoaderCallback

internal interface AssetsDownloader {
    /**
     * Downloads assets required by the ad.
     * One asset can be used by many ads.
     * @param appRequest - Object with ad data
     * @param adTypeTraitsName - Readable name of the ad type can be interstitial, rewarded or banner.
     * @param assetDownloadedCallback - Callback indicates when asset download is completed
     * @param adUnitLoaderCallback - Callback when used informs that ad is cached and ready to use
     */
    fun downloadAdUnitAssets(
        appRequest: AppRequest,
        adTypeTraitsName: String,
        assetDownloadedCallback: AssetDownloadedCallback,
        adUnitLoaderCallback: AdUnitLoaderCallback,
    )
}
