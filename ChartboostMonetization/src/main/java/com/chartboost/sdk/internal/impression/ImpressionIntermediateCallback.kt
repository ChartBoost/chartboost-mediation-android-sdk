package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.Model.ImpressionState

/**
 * This interface currently connect 3 aspects of the impression: view, dismiss and click
 * TODO Due to further refactor
 */
internal interface ImpressionIntermediateCallback {
    fun destroyImpression()

    fun setShowProcessed(showProcessed: Boolean)

    fun isVisible(): Boolean

    fun setImpressionState(state: ImpressionState)

    fun callOnClose()

    fun callImpressionDismissCallback()
}
