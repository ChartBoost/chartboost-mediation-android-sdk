package com.chartboost.sdk.internal.Networking.requests.models

class CompleteParamsModel(
    val location: String,
    val adId: String,
    val cgn: String,
    val rewardAmount: Int,
    val rewardCurrency: String,
    val videoPostion: Float?,
    val videoDuration: Float?,
)
