package com.chartboost.sdk.internal.utils

val Exception?.errorMessage: String get() = this?.message ?: "Unknown error"
