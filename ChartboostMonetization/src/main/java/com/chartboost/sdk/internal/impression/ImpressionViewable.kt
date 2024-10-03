package com.chartboost.sdk.internal.impression

import android.view.ViewGroup
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.view.CBImpressionActivity

internal interface ImpressionViewable {
    var wasImpressionSignaled: Boolean

    fun getHostView(): ViewGroup?

    fun displayOnHostView(hostView: ViewGroup?)

    fun displayOnActivity(
        state: ImpressionState,
        activity: CBImpressionActivity,
    )

    fun setIsVisible(visible: Boolean)

    fun getIsVisible(): Boolean

    fun setIsShowProcessed(showProcessed: Boolean)

    fun getIsShowProcessed(): Boolean

    fun setIsVideoShowSent(showSent: Boolean)

    fun getIsVideoShowSent(): Boolean

    fun setImpressionClosed(impressionClose: Boolean)

    fun getImpressionClosed(): Boolean

    fun sendImpressionReadyToBeDisplayedCallback()

    fun shownFully()

    fun onFailure(error: CBError.Impression)

    fun onStart()

    fun onResume()

    fun onPause()

    fun closeImpression()
}
