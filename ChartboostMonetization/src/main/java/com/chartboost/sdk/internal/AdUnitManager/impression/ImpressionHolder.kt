package com.chartboost.sdk.internal.AdUnitManager.impression

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.impression.CBImpression

/**
 * Data structure to handle impression creation
 */
internal data class ImpressionHolder(
    val impression: CBImpression?,
    val error: CBError.Impression?,
)
