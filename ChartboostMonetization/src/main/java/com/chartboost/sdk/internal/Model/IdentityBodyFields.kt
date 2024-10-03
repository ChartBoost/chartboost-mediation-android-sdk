package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.internal.identity.TrackingState

data class IdentityBodyFields(
    val trackingState: TrackingState = TrackingState.TRACKING_UNKNOWN,
    val identifiers: String? = null,
    val uuid: String? = null,
    val gaid: String? = null,
    val setId: String? = null,
    val setIdScope: Int? = null,
)
