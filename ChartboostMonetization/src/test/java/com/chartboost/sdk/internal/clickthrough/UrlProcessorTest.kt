package com.chartboost.sdk.internal.clickthrough

import io.kotest.matchers.shouldBe
import org.junit.Test

class UrlProcessorTest {
    @Test
    fun `when calling convertMarketToHttps() and URL is not a market URL, it should return the same URL`() {
        val urlArgs =
            UrlArgs(
                url = "https://some.host.com",
                clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            )
        urlArgs.convertMarketToHttps() shouldBe urlArgs
    }

    @Test
    fun `when calling convertMarketToHttps and URL is a market URL with different format, it should return the same URL`() {
        val urlArgs =
            UrlArgs(
                url = "market://appid?id=aaa.bbb.ccc",
                clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            )
        urlArgs.convertMarketToHttps() shouldBe urlArgs
    }

    @Test
    fun `when calling convertMarketToHttps and URL is a valid market URL, it should return the equivalent HTTPS URL`() {
        val appId = "com.outfit7.gingersbirthdayfree"
        val urlArgs =
            UrlArgs(
                url = "market://details?id=$appId",
                clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            )
        urlArgs.convertMarketToHttps() shouldBe
            UrlArgs(
                url = "https://play.google.com/store/apps/details?id=$appId",
                clickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED,
            )
    }
}
