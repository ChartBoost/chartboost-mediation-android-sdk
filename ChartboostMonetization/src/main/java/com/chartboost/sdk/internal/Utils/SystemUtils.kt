package com.chartboost.sdk.internal.utils

/** Timestamp in milliseconds **/
internal typealias TimeStamp = Long

/** Timestamp in seconds **/
internal typealias TimeStampSeconds = Long

internal fun now(): TimeStamp = System.currentTimeMillis()

internal fun TimeStamp.asSeconds(): TimeStampSeconds = this / 1000
