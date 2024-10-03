package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Model.CBError.Click
import com.chartboost.sdk.internal.Model.ImpressionMediaType
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.internal.Networking.requests.ClickRequest
import com.chartboost.sdk.internal.Networking.requests.ClickRequestCallback
import com.chartboost.sdk.internal.Networking.requests.models.ClickParams
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import org.json.JSONObject

internal class ImpressionClick(
    private val adUnit: AdUnit,
    private val urlResolver: UrlResolver,
    private val intentResolver: IntentResolver,
    private val clickRequest: ClickRequest,
    private val clickTracking: ClickTracking,
    private val mediaType: ImpressionMediaType,
    private val impressionCallback: ImpressionClickCallback,
    private val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    private val adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
) : ImpressionClickable, ClickTracking by clickTracking {
    override var click = false

    // This flag value is sent back to the server with requests. In case we use url from the AdGet
    // and we use deepLink the flag is assigned to true. When we use normal url from AdGet value is
    // false and when we process link from the template the value is null and not attached to the requests
    private var retargetReinstall: Boolean? = null
    private var shouldDismissAfterClick = false

    override fun submitClickRequest(
        location: String,
        videoPosition: Float?,
        videoDuration: Float?,
    ) {
        val params =
            ClickParams(
                location,
                adUnit.adId,
                adUnit.to,
                adUnit.cgn,
                adUnit.creative,
                videoPosition,
                videoDuration,
                mediaType,
                retargetReinstall,
            )

        clickRequest.execute(
            object : ClickRequestCallback {
                override fun onClickRequestSuccess(clickJson: JSONObject?) {
                    Logger.e("onClickRequestSuccess ${clickJson?.toString() ?: ""}")
                }

                override fun onClickRequestFailure(errorMsg: String?) {
                    Logger.e("onClickRequestFailure $errorMsg")
                }
            },
            params,
        )
    }

    override fun callImpressionClickSuccessCallback() {
        adUnitRendererImpressionCallback.onImpressionClicked(adUnit.impressionId)
        if (shouldDismissAfterClick) {
            impressionCallback.callDismissAfterClick()
        }
    }

    override fun callImpressionClickFailureCallback(
        url: String?,
        error: Click,
    ) {
        adUnitRendererImpressionCallback.onImpressionClickedFailed(adUnit.impressionId, url, error)
    }

    /**
     * This is most common use case OpenUrl template event which contains the url
     */
    override fun onOpenURL(cbUrl: CBUrl) {
        impressionClickTriggered(cbUrl.url, cbUrl.shouldDismiss)
    }

    override fun clickTriggeredBeforeLoadFinished(cbUrl: CBUrl) {
        impressionClickTriggeredBeforeLoadFinished(cbUrl.url)
    }

    /**
     * This is covers the case of a URL needing to be opened WITHOUT triggering a click event
     */
    override fun onOpenNonClickURL(cbUrl: CBUrl) {
        impressionNonClickUrlTriggered(cbUrl.url)
    }

    /**
     * This logic comes only from CLICK template event and combines the url from the AdGet
     */
    override fun onClick(
        shouldDismiss: Boolean?,
        impressionState: ImpressionState,
    ): Boolean {
        shouldDismiss?.let {
            shouldDismissAfterClick = it
        }

        // already closed or not yet open
        if (impressionState != ImpressionState.DISPLAYED) {
            return false
        }

        var url = adUnit.link
        val deepLink = adUnit.deepLink

        if (intentResolver.canOpenDeeplink(deepLink)) {
            url = deepLink
            retargetReinstall = true
        } else {
            retargetReinstall = false
        }

        if (click) {
            return false
        }

        click = true
        impressionCallback.setImpressionClosed(false)
        impressionClickTriggered(url, shouldDismissAfterClick)
        return true
    }

    private fun impressionClickTriggered(
        url: String?,
        shouldDismiss: Boolean?,
    ) {
        openMeasurementImpressionCallback.onImpressionNotifyClick()
        shouldDismiss?.let {
            shouldDismissAfterClick = it
        }
        urlResolver.resolve(
            url,
            adUnit.clkp,
            clickTracking,
        )?.let { error -> impressionCallback.failure(url, error) } ?: impressionCallback.success(url)
    }

    private fun impressionNonClickUrlTriggered(url: String?) {
        urlResolver.resolve(
            url,
            adUnit.clkp,
            clickTracking,
        )
    }

    private fun impressionClickTriggeredBeforeLoadFinished(url: String?) {
        impressionCallback.failure(url, Click.LOAD_NOT_FINISHED)
    }

    private fun ImpressionClickCallback?.success(url: String?) {
        notify {
            callImpressionClickSuccessCallback()
            trackNavigationSuccess("Url impression callback success: $url")
        }
    }

    private fun ImpressionClickCallback?.failure(
        url: String?,
        error: Click,
    ) {
        notify {
            callImpressionClickFailureCallback(url, error)
            trackNavigationFailure("Impression click callback for: $url failed with error: $error")
        }
    }

    private fun ImpressionClickCallback?.notify(block: ImpressionClickCallback.() -> Unit) {
        this?.run {
            setImpressionClick(false)
            block()
        } ?: Logger.e("Impression callback is null")
    }
}
