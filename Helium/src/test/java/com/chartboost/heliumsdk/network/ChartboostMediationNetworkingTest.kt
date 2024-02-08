/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.chartboost.heliumsdk.PartnerConsents
import com.chartboost.heliumsdk.ad.HeliumBannerAd
import com.chartboost.heliumsdk.controllers.PartnerController
import com.chartboost.heliumsdk.controllers.PrivacyController
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.utils.Environment
import io.mockk.*
import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ChartboostMediationNetworkingTest {
    val mockWebServer = MockWebServer()

    @Before
    fun setup() {
        setFinalStatic(Build.VERSION::class.java.getField("RELEASE"), BUILD_RELEASE)

        Endpoints.SDK_DOMAIN = getMockServerUrl()
        Endpoints.RTB_DOMAIN = getMockServerUrl()

        mockkObject(Environment)
        every { Environment.sessionId } returns SESSION_ID
    }

    /*
        Helium Impression
     */
    @Test
    fun trackChartboostImpression_success() = `verify result for chartboost ("helium") impression success`()

    @Test
    fun trackChartboostImpression_failure() = `verify result for chartboost ("helium") impression failure`()

    /*
        Partner Impression
     */
    @Test
    fun trackPartnerImpression_success() = `verify result for partner impression success`()

    @Test
    fun trackPartnerImpression_failure() = `verify result for partner impression failure`()

    /*
        App Config
     */
    @Test
    fun `getAppConfig new config success`() = `verify result for sdk_init success with new config`()

    @Test
    fun `getAppConfig no new config success`() = `verify result for sdk_init success with no new config`()

    @Test
    fun `getAppConfig json parsing failure`() = `verify result for sdk_init failure due to JSON parsing`()

    @Test
    fun `getAppConfig http code failure`() = `verify result for sdk_init failure due to http code`()

    /*
        Make Bid Request
     */
    @Test
    fun `makeBidRequest with banner bid info success`() = `verify banner bid request success`()

    @Test
    fun `makeBidRequest with banner bid info failure due to json failure`() = `verify banner bid request failure due to json parsing`()

    @Test
    fun `makeBidRequest with banner bid info failure due to http code`() = `verify banner bid request failure due to http code`()

    @Test
    fun `makeBidRequest with interstitial bid info success`() = `verify interstitial bid request success`()

    @Test
    fun `makeBidRequest with interstitial bid info failure due to json failure`() =
        `verify interstitial bid request failure due to json parsing`()

    @Test
    fun `makeBidRequest with interstitial bid info failure due to http code`() =
        `verify interstitial bid request failure due to http code`()

    @Test
    fun `makeBidRequest with rewarded bid info success`() = `verify rewarded bid request success`()

    @Test
    fun `makeBidRequest with rewarded bid info failure due to json failure`() = `verify rewarded bid request failure due to json parsing`()

    @Test
    fun `makeBidRequest with rewarded bid info failure due to http code`() = `verify rewarded bid request failure due to http code`()

    @Test
    fun `makeRewardedCallbackRequest with GET rewarded callback info success`() = `verify rewarded callback GET request success`()

    @Test
    fun `makeRewardedCallbackRequest with POST rewarded callback info success`() = `verify rewarded callback POST request success`()

    /*
        Log Auction Winner
     */
    @Test
    fun `logAuctionWinner success`() = `verify log auction winner request success`()

    /*
        Track Event
     */
    @Test
    fun `trackEvent for initialization success`() = `verify trackEvent for initialization success`()

    @Test
    fun `trackEvent for initialization success with error`() = `verify trackEvent for initialization with error`()

    @Test
    fun `trackEvent for initialization success failure`() = `verify trackEvent for initialization failure`()

    @Test
    fun `trackAdLoad`() = `verify trackAdLoad`()

    @Test
    fun `trackClick`() = `verify trackClick`()

    @Test
    fun `trackReward`() = `verify trackReward`()

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        clearAllMocks()
    }

    @Throws(Exception::class)
    fun setFinalStatic(
        field: Field,
        newValue: Any?,
    ) {
        field.isAccessible = true
        val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
        field.set(null, newValue)
    }

    fun setupBannerAdLoadParams(): AdLoadParams {
        return AdLoadParams(
            adIdentifier = AdIdentifier(2, "AllNonProBanner (Banner)"),
            keywords = Keywords(),
            loadId = BANNER_LOAD_ID,
            bannerSize = HeliumBannerAd.HeliumBannerSize.STANDARD,
            adInteractionListener = mockk(),
        )
    }

    fun setupInterstitialAdLoadParams(): AdLoadParams {
        return AdLoadParams(
            adIdentifier = AdIdentifier(0, "AllNonProInterstitial"),
            keywords = Keywords(),
            loadId = INTERSTITIAL_LOAD_ID,
            bannerSize = null,
            adInteractionListener = mockk(),
        )
    }

    fun setupRewardedAdLoadParams(): AdLoadParams {
        return AdLoadParams(
            adIdentifier = AdIdentifier(1, "AllNonProRewarded"),
            keywords = Keywords(),
            loadId = REWARDED_LOAD_ID,
            bannerSize = null,
            adInteractionListener = mockk(),
        )
    }

    fun setupPrivacyController(): PrivacyController {
        val mockedContext = mockk<Context>()
        val mockedSharedPreferences = mockk<SharedPreferences>()
        val mockedEditor = mockk<SharedPreferences.Editor>()
        val mockedPartnerConsents = mockk<PartnerConsents>()
        every { mockedContext.getSharedPreferences(any(), any()) } returns mockedSharedPreferences
        every { mockedSharedPreferences.getString(any(), any()) } returns ""
        every { mockedSharedPreferences.getInt(any(), any()) } returns -1
        every { mockedSharedPreferences.contains(any()) } returns false
        every { mockedSharedPreferences.edit() } returns mockedEditor
        every { mockedEditor.putInt(any(), any()) } returns mockedEditor
        every { mockedEditor.apply() } just Runs
        every { mockedPartnerConsents.addPartnerConsentsObserver(any()) } just Runs

        return PrivacyController(mockedContext, mockedPartnerConsents).apply {
            coppa = false
            gdpr = -1
            ccpaConsent = null
        }
    }

    fun setupPartnerController(): PartnerController {
        return PartnerController().apply {
            adapters =
                mutableMapOf<String, PartnerAdapter>().also {
                    it["admob"] = mockk()
                }
            initStatuses =
                mutableMapOf<String, PartnerController.PartnerInitializationStatus>().also {
                    it["admob"] = PartnerController.PartnerInitializationStatus.INITIALIZED
                }
        }
    }

    fun setupBidTokens(): Map<String, Map<String, String>> {
        return mutableMapOf<String, Map<String, String>>().also {
            it["admob"] = mapOf()
        }
    }

    fun getMockServerUrl(): String {
        return mockWebServer.url("").toString().let {
            it.take(it.length - 1)
        }
    }

    companion object {
        const val APP_ID = "appidabc123"
        const val APP_SET_ID = "appsetidabc123"
        const val AUCTION_ID = "auctionidabc123"
        const val INIT_HASH_NEW = "inithashold123"
        const val INIT_HASH_OLD = "inithashnew123"
        const val SESSION_ID = "sessionidabc123"
        const val LOAD_ID = "loadidabc123"
        const val BUILD_RELEASE = "33"

        const val BANNER_LOAD_ID = "bannerloadid123"
        const val INTERSTITIAL_LOAD_ID = "interstitialloadid123"
        const val REWARDED_LOAD_ID = "rewardedloadid123"

        const val BANNER_IMPRESSION_DEPTH = 1
        const val INTERSTITIAL_IMPRESSION_DEPTH = 2
        const val REWARDED_IMPRESSION_DEPTH = 3

        const val RATE_LIMIT_HEADER_VALUE = "ratelimitnew123"
    }
}
