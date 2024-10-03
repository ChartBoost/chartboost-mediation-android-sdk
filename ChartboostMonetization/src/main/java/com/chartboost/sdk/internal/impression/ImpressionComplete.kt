package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Networking.requests.CompleteRequest
import com.chartboost.sdk.internal.Networking.requests.CompleteRequestCallback
import com.chartboost.sdk.internal.Networking.requests.models.CompleteParamsModel
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger
import org.json.JSONObject

internal class ImpressionComplete(
    private val adUnit: AdUnit,
    private val adType: AdType,
    private val completeRequest: CompleteRequest,
    private val adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
) : ImpressionCompletable {
    override fun sendVideoCompleteRequest(
        location: String,
        videoPosition: Float?,
        videoDuration: Float?,
    ) {
        val params =
            CompleteParamsModel(
                location,
                adUnit.adId,
                adUnit.cgn,
                adUnit.rewardAmount,
                adUnit.rewardCurrency,
                videoPosition,
                videoDuration,
            )
        completeRequest.execute(
            object : CompleteRequestCallback {
                override fun onCompleteRequestSuccess(completeJson: JSONObject?) {
                    Logger.e("onCompleteRequestSuccess $completeJson")
                }

                override fun onCompleteRequestFailure(errorMsg: String?) {
                    Logger.e("onCompleteRequestFailure $errorMsg")
                }
            },
            params,
        )
    }

    override fun notifyDidCompleteAd() {
        if (adType === AdType.Interstitial) {
            Logger.e("didCompleteInterstitial delegate used to be sent here")
        } else if (adType === AdType.Rewarded) {
            adUnitRendererImpressionCallback.onImpressionRewarded(
                adUnit.impressionId,
                adUnit.rewardAmount,
            )
        }
    }
}
