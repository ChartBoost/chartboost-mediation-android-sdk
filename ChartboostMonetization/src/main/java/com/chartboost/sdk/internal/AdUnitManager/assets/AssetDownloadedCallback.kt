package com.chartboost.sdk.internal.AdUnitManager.assets

import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest

internal interface AssetDownloadedCallback {
    fun onAssetDownloaded(
        request: AppRequest,
        resultAsset: AssetDownloadedResult,
    )
}
