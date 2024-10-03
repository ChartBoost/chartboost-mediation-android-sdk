package com.chartboost.sdk.internal.AdUnitManager.loaders

import android.os.Build
import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.parsers.OpenRTBAdUnitParser
import com.chartboost.sdk.internal.AssetLoader.AssetDownloadCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

internal class OrtbLoader(
    val adType: AdType,
    val downloader: Downloader,
    private val openRTBAdUnitParser: OpenRTBAdUnitParser,
    private val jsonFactory: (String) -> JSONObject = ::JSONObject,
    private val androidVersion: () -> Int = { Build.VERSION.SDK_INT },
    eventTracker: EventTrackerExtensions,
) : AdLoader, EventTrackerExtensions by eventTracker {
    override fun loadAd(
        params: LoadParams,
        callback: LoadResult.() -> Unit,
    ) {
        if (androidVersion() < Build.VERSION_CODES.LOLLIPOP) {
            callback.reportWrongAndroidVersion(params)
            return
        }

        if (!isDataCorrect(params)) {
            callback.reportIncorrectData(params)
            return
        }

        val openRTBAdUnit: AdUnit =
            try {
                val json = params.appRequest.bidResponse?.let(jsonFactory)
                openRTBAdUnitParser.parse(adType, json)
            } catch (e: JSONException) {
                callback.reportJSONParsingError(params, e)
                return
            }
        startCache(params, openRTBAdUnit, callback)
    }

    private fun (LoadResult.() -> Unit).reportWrongAndroidVersion(params: LoadParams) {
        invoke(
            LoadResult(
                params.appRequest,
                error =
                    CBError(
                        CBError.Internal.UNSUPPORTED_OS_VERSION,
                        "Unsupported Android version ${Build.VERSION.SDK_INT}",
                    ),
            ),
        )
    }

    private fun (LoadResult.() -> Unit).reportIncorrectData(params: LoadParams) {
        trackCriticalError(
            name = TrackingEventName.Cache.BID_RESPONSE_PARSING_ERROR,
            location = params.appRequest.location,
            bidResponse = params.appRequest.bidResponse ?: "",
            error = "Invalid bid response",
        )
        invoke(
            LoadResult(
                params.appRequest,
                error =
                    CBError(
                        CBError.Internal.UNEXPECTED_RESPONSE,
                        "Error parsing response",
                    ),
            ),
        )
    }

    private fun (LoadResult.() -> Unit).reportJSONParsingError(
        params: LoadParams,
        e: Exception,
    ) {
        trackCriticalError(
            name = TrackingEventName.Cache.BID_RESPONSE_PARSING_ERROR,
            location = params.appRequest.location,
            bidResponse = params.appRequest.bidResponse ?: "",
            error = e.toString(),
        )
        invoke(
            LoadResult(
                params.appRequest,
                error =
                    CBError(
                        CBError.Internal.INVALID_RESPONSE,
                        "Error parsing response",
                    ),
            ),
        )
    }

    private fun startCache(
        loaderParams: LoadParams,
        openRTBAdUnit: AdUnit,
        callback: (result: LoadResult) -> Unit,
    ) {
        downloadAssets(downloader, openRTBAdUnit) { success: Boolean ->
            if (success) {
                callback.reportSuccess(loaderParams, openRTBAdUnit)
            } else {
                callback.reportAssetDownloadError(loaderParams)
            }
        }
    }

    private fun ((LoadResult) -> Unit).reportSuccess(
        loaderParams: LoadParams,
        openRTBAdUnit: AdUnit,
    ) {
        invoke(
            LoadResult(
                loaderParams.appRequest,
                openRTBAdUnit,
                null,
            ),
        )
    }

    private fun ((LoadResult) -> Unit).reportAssetDownloadError(loaderParams: LoadParams) {
        trackCriticalError(
            name = TrackingEventName.Cache.ASSET_DOWNLOAD_ERROR,
            location = loaderParams.appRequest.location,
            bidResponse = loaderParams.appRequest.bidResponse ?: "",
            error = CBError.Impression.ASSETS_DOWNLOAD_FAILURE.name,
        )
        invoke(
            LoadResult(
                loaderParams.appRequest,
                error =
                    CBError(
                        CBError.Internal.INVALID_RESPONSE,
                        "Error parsing response",
                    ),
            ),
        )
    }

    // TODO This should be refactored reuse the same code as ad downloader via AssetDownloader
    //  in the main AdUnitload class
    private fun downloadAssets(
        downloader: Downloader,
        openRTB: AdUnit,
        callback: AssetDownloadCallback,
    ) {
        openRTB.assets.let {
            val remainingDownloads = AtomicInteger()
            downloader.resume()
            downloader.downloadAssets(
                Priority.HIGH,
                it,
                remainingDownloads,
                callback,
                adType.name,
            )
        }
    }

    private fun trackCriticalError(
        name: TrackingEventName,
        location: String,
        bidResponse: String,
        error: String,
    ) {
        CriticalEvent(
            name,
            JSONObject().getTrackingErrorWithResponseJson(
                error = error,
                response = bidResponse,
            ),
            adType.name,
            location,
        ).track()
    }

    private fun isDataCorrect(params: LoadParams): Boolean {
        return params.appRequest.location.isNotEmpty() && params.appRequest.bidResponse?.isNotEmpty() == true
    }
}
