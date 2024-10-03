package com.chartboost.sdk.internal.clickthrough

private val MARKET_REGEX = "^market://details\\?id=(.*)$".toRegex()
private const val MARKET_WEB = "https://play.google.com/store/apps/details?id=%s"

internal fun UrlArgs.convertMarketToHttps(): UrlArgs = appIdFromMarket()?.let { id -> copy(url = MARKET_WEB.format(id)) } ?: this

private fun UrlArgs.appIdFromMarket(): String? = MARKET_REGEX.matchEntire(url)?.groupValues?.getOrNull(1)
