package com.chartboost.sdk.internal.AdUnitManager.assets

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.loaders.AdUnitLoaderCallback
import com.chartboost.sdk.internal.AssetLoader.AssetDownloadCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.Factory
import com.chartboost.sdk.tracking.TrackingEventName
import java.util.concurrent.atomic.AtomicInteger

internal class AssetsDownloaderImpl(
    val downloader: Downloader,
    val timeSource: TimeSource,
    val videoRepository: VideoRepository,
    var adType: AdType,
    val mediation: Mediation?,
) : AssetsDownloader {
    override fun downloadAdUnitAssets(
        appRequest: AppRequest,
        adTypeTraitsName: String,
        assetDownloadedCallback: AssetDownloadedCallback,
        adUnitLoaderCallback: AdUnitLoaderCallback,
    ) {
        val adUnit: AdUnit = appRequest.adUnit ?: return
        val callback =
            AssetDownloadCallback { success: Boolean ->
                val resultAsset: AssetDownloadedResult =
                    when (success) {
                        true -> onAdUnitAssetDownloadSuccess(appRequest, adUnit, adUnitLoaderCallback)
                        false -> AssetDownloadedResult.FAILURE
                    }
                assetDownloadedCallback.onAssetDownloaded(appRequest, resultAsset)
            }

        // Sometimes when error happens, next ad could get stuck cause downloader was paused and never resumed
        downloader.resume()
        downloader.downloadAssets(
            Priority.NORMAL,
            adUnit.assets,
            AtomicInteger(),
            Factory.instance().intercept(callback),
            adTypeTraitsName,
        )
    }

    private fun onAdUnitAssetDownloadSuccess(
        appRequest: AppRequest,
        adUnit: AdUnit,
        callback: AdUnitLoaderCallback,
    ): AssetDownloadedResult {
        callback.onAdUnitCacheSuccess(appRequest, TrackingEventName.Cache.FINISH_SUCCESS)
        if (adUnit.isPrecacheVideoAd) {
            if (!videoRepository.isFileDownloadingOrDownloaded(adUnit.videoFilename)) {
                videoRepository.downloadVideoFile(
                    adUnit.videoUrl,
                    adUnit.videoFilename,
                    false,
                    null,
                )
            }
        } else {
            return AssetDownloadedResult.READY_TO_SHOW
        }
        return AssetDownloadedResult.SUCCESS
    }
}
