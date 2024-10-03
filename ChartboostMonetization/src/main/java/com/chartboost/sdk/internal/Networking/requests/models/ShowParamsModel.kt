package com.chartboost.sdk.internal.Networking.requests.models

import com.chartboost.sdk.Mediation

class ShowParamsModel(
    val adId: String?,
    val location: String,
    val videoCached: Int,
    val adTypeName: String,
    val mediation: Mediation?,
)
