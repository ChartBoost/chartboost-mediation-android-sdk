package com.chartboost.sdk.internal.AdUnitManager.loaders

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetDownloadedCallback
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetDownloadedResult
import com.chartboost.sdk.internal.AdUnitManager.assets.AssetsDownloader
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnitBannerData
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.data.toAdSize
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger.d
import com.chartboost.sdk.internal.logging.Logger.e
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.InfoEvent
import com.chartboost.sdk.tracking.TrackAd
import com.chartboost.sdk.tracking.TrackingEventName
import java.util.concurrent.atomic.AtomicBoolean

// TODO Big constructor, probably needs to be refactored further
internal class AdUnitLoader(
    private val adType: AdType,
    private val fileCache: FileCache,
    private val reachability: CBReachability,
    private val videoRepository: VideoRepository,
    private val assetsDownloader: AssetsDownloader,
    private val adLoader: AdLoader,
    private val ortbLoader: OrtbLoader,
    private val mediation: Mediation?,
    private val eventTracker: EventTrackerExtensions,
) : AdUnitLoaderCallback, AssetDownloadedCallback, EventTrackerExtensions by eventTracker {
    private var appRequestStored: AppRequest? = null
    private var callback: AdUnitLoaderAdCallback? = null
    private var bannerData: AdUnitBannerData? = null
    private val isLoading = AtomicBoolean(false)

    fun getAppRequest(): AppRequest? {
        return appRequestStored
    }

    fun load(
        location: String,
        callback: AdUnitLoaderAdCallback,
        bidResponse: String? = null,
        bannerData: AdUnitBannerData? = null,
    ) {
        if (isLoading.getAndSet(true)) {
            trackInfoEvent(TrackingEventName.Cache.IGNORED, location)
            return
        }

        // If downloaded ad is in ready but assets are missing/deleted go for a new caching request.
        appRequestStored?.let {
            it.adUnit?.let { adUnit ->
                if (!fileCache.isAssetsAvailable(adUnit)) {
                    clearTrackingReferences(it)
                    appRequestStored = null
                }
            }
        }

        // Reassign bid response in case next cache contains non-null response and app request is still valid
        appRequestStored?.bidResponse = bidResponse

        val appRequestLocal =
            appRequestStored ?: AppRequest(
                System.currentTimeMillis().toInt(),
                location,
                bidResponse,
            ).also {
                // Only assign new callback and banner when new AppRequest is newly created
                this.callback = callback
                this.bannerData = bannerData
                it.bannerData = bannerData
                appRequestStored = it
            }

        if (!reachability.isNetworkAvailable) {
            reportError(appRequestLocal, CBError.Impression.INTERNET_UNAVAILABLE_AT_CACHE)
            return
        }

        appRequestLocal.isTrackedCache = true

        if (appRequestLocal.adUnit == null) {
            trackInfoEvent(TrackingEventName.Cache.START, appRequestLocal.location)
            sendAdGetRequestSafe(appRequestLocal)
        } else {
            // already cached, send success callback but track cache ignore to avoid incorrect data
            onAdUnitCacheSuccess(appRequestLocal, TrackingEventName.Cache.IGNORED)
        }
    }

    /**
     * Public api function Chartboost.clearCache() uses removeAppRequest()
     * Usage of removeAppRequest() makes sdk ready to cache ad again because
     * function removes the adUnit from the appRequest.
     */
    fun removeAppRequest() {
        if (isLoading.get()) {
            // Don't clear while loading, could lead to unexpected behaviour
            return
        }
        appRequestStored?.let {
            clearTrackingReferences(it)
            it.adUnit = null
        }
        appRequestStored = null
    }

    override fun onAssetDownloaded(
        request: AppRequest,
        resultAsset: AssetDownloadedResult,
    ) {
        when (resultAsset) {
            AssetDownloadedResult.FAILURE -> onAssetDownloadedError(request)
            AssetDownloadedResult.READY_TO_SHOW ->
                d("onAssetDownloaded: Ready to show")

            AssetDownloadedResult.SUCCESS -> d("onAssetDownloaded: Success")
        }
    }

    override fun onAdUnitCacheSuccess(
        appRequest: AppRequest,
        trackingEventName: TrackingEventName,
    ) {
        callback?.onCacheSuccess(getImpressionIdFromAppRequest(appRequest), trackingEventName)
        isLoading.set(false)
    }

    /**
     * Here we do interstitial/get, rewarded/get requests and auction/sdk/banner
     * */
    private fun sendAdGetRequestSafe(appRequest: AppRequest) {
        try {
            sendAdGetRequest(appRequest)
        } catch (ex: Exception) {
            e("sendAdGetRequest", ex)
            onAdGetFailure(
                appRequest,
                CBError(CBError.Internal.MISCELLANEOUS, "error sending ad-get request"),
            )
        }
    }

    private fun sendAdGetRequest(appRequest: AppRequest) {
        val isCacheRequest = true
        var params =
            LoadParams(
                appRequest,
                isCacheRequest,
                bannerData?.bannerHeight,
                bannerData?.bannerWidth,
            )

        val (loadStrategy, adjustedParams) =
            AdUnitLoaderStrategy
                .validateFormat(appRequest, params, ::loadOpenRTBAd, ::loadAdGet)

        params = adjustedParams

        loadStrategy(appRequest, params)
    }

    private fun loadOpenRTBAd(
        appRequest: AppRequest,
        params: LoadParams,
    ) {
        ortbLoader.loadAd(params) {
            fold(
                isSuccess = {
                    appRequest.adUnit = adUnit
                    precacheVideoForOpenRTB(appRequest)
                    updateStateOnLoadSuccess(appRequest)
                    onAdUnitCacheSuccess(appRequest, TrackingEventName.Cache.FINISH_SUCCESS)
                },
                isError = {
                    // TODO Error is ignored in this case and never tracked, is this correct?
                    dismissAppRequest(appRequest)
                },
            )
        }
    }

    private fun loadAdGet(
        appRequest: AppRequest,
        params: LoadParams,
    ) {
        adLoader.loadAd(params) {
            fold(
                isSuccess = {
                    updateStateOnLoadSuccess(appRequest)
                    downloadAdUnitAssets(appRequest)
                },
                isError = { error ->
                    error.track(appRequest.location)
                    dismissAppRequest(appRequest)
                },
            )
        }
    }

    private fun LoadResult.dismissAppRequest(appRequest: AppRequest) {
        saveAdData(appRequest.location, null)
        onAdGetFailure(appRequest, error)
    }

    private fun LoadResult.updateStateOnLoadSuccess(appRequest: AppRequest) {
        saveAdData(appRequest.location, adUnit)
        appRequest.adUnit = adUnit
    }

    private fun downloadAdUnitAssets(appRequest: AppRequest) {
        assetsDownloader.downloadAdUnitAssets(
            appRequest,
            adType.name,
            this,
            this,
        )
    }

    private fun onAssetDownloadedError(appRequest: AppRequest) {
        reportError(appRequest, CBError.Impression.ASSETS_DOWNLOAD_FAILURE)
        removeAppRequest(appRequest)
    }

    private fun precacheVideoForOpenRTB(appRequest: AppRequest) {
        if (appRequest.adUnit?.isPrecacheVideoAd == true) {
            videoRepository.downloadVideoFile(
                appRequest.adUnit?.videoUrl ?: "",
                appRequest.adUnit?.videoFilename ?: "",
                false,
                null,
            )
        }
    }

    private fun removeAppRequest(appRequest: AppRequest) {
        clearTrackingReferences(appRequest)
        appRequest.adUnit = null
        isLoading.set(false)
    }

    private fun onAdGetFailure(
        appRequest: AppRequest,
        error: CBError?,
    ) {
        val impressionError = getImpressionErrorFromCBError(error)
        reportError(appRequest, impressionError)
        removeAppRequest(appRequest)
    }

    private fun getImpressionErrorFromCBError(error: CBError?): CBError.Impression {
        // Default error set to internal but error should never be null in this case
        var impressionError = CBError.Impression.INTERNAL
        if (error?.impressionError != null) {
            impressionError = error.impressionError
        }
        return impressionError
    }

    private fun clearTrackingReferences(appRequest: AppRequest) {
        eventTracker.clear(
            appRequest.adUnit?.name ?: "",
            appRequest.location,
        )
    }

    private fun postErrorByAdType(
        appRequest: AppRequest,
        error: CBError.Impression,
    ) {
        callback?.onAdFailToLoad(getImpressionIdFromAppRequest(appRequest), error)
    }

    private fun getImpressionIdFromAppRequest(appRequest: AppRequest): String? {
        return appRequest.adUnit?.impressionId
    }

    private fun reportError(
        appRequest: AppRequest,
        error: CBError.Impression,
    ) {
        isLoading.set(false)
        postErrorByAdType(appRequest, error)
        if (error == CBError.Impression.NO_AD_FOUND) return
        appRequest.let {
            e(
                "reportError: adTypeTraits: ${adType.name}" +
                    " reason: ${CBConstants.REASON_CACHE}  format: web" +
                    " error: $error" +
                    " adId: ${it.adUnit?.adId}" +
                    " appRequest.location: ${it.location}",
            )
        }
    }

    private fun saveAdData(
        location: String?,
        adUnit: AdUnit?,
    ) {
        TrackAd(
            location = location ?: "no location",
            adType = adType.name,
            adImpressionId = adUnit?.impressionId ?: "",
            adCreativeId = adUnit?.creative ?: "",
            adCreativeType = adUnit?.mediaType ?: "",
            adMarkup = adUnit?.getAdMarkup() ?: "",
            templateUrl = adUnit?.template ?: "",
            adSize = bannerData.toAdSize(),
        ).store()
    }

    private fun trackInfoEvent(
        eventName: TrackingEventName,
        location: String,
    ) {
        InfoEvent(
            eventName,
            "",
            adType.name,
            location,
            mediation,
        ).track()
    }

    private fun CBError.track(location: String) {
        when (type) {
            CBError.Internal.HTTP_NOT_FOUND,
            CBError.Internal.HTTP_NOT_OK,
            -> {
                trackAsErrorEvent(
                    name = TrackingEventName.Cache.SERVER_ERROR,
                    location = location,
                )
            }

            CBError.Internal.UNSUPPORTED_OS_VERSION -> {
                trackAsErrorEvent(
                    name = TrackingEventName.Misc.UNSUPPORTED_OS_VERSION,
                    location = location,
                )
            }

            else -> {
                trackAsErrorEvent(
                    name = TrackingEventName.Cache.REQUEST_ERROR,
                    location = location,
                )
            }
        }
    }

    private fun CBError.trackAsErrorEvent(
        name: TrackingEventName,
        location: String,
    ) {
        ErrorEvent(
            name,
            message ?: "",
            adType.name,
            location,
            mediation,
        ).track()
    }
}
