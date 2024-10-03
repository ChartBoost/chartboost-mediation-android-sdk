package com.chartboost.sdk.internal.video.player.exoplayer

import com.google.android.exoplayer2.ExoPlayer

internal fun ExoPlayer.width(): Int = videoFormat?.width ?: 1

internal fun ExoPlayer.height(): Int = videoFormat?.height ?: 1
