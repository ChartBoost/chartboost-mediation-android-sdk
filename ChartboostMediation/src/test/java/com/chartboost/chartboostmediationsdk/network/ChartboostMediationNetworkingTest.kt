/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import android.content.Context
import android.content.SharedPreferences
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.controllers.PartnerController
import com.chartboost.chartboostmediationsdk.controllers.PrivacyController
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.core.ChartboostCore
import com.chartboost.core.environment.AnalyticsEnvironment
import com.chartboost.core.environment.VendorIdScope
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdClient
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
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
    @ExperimentalCoroutinesApi
    fun setup() {
        Endpoints.SDK_HOSTNAME = getMockServerUrl()

        val url =
            mockWebServer.url("").toString()

        setMockEnvironment()

        Dispatchers.setMain(UnconfinedTestDispatcher())

        mockkStatic(AppSet::class)
        val mockAppSetIdInfo: AppSetIdInfo = mockk()
        val mockAppSetIdInfoTask: Task<AppSetIdInfo> = mockk()
        val mockAppSetIdClient: AppSetIdClient = mockk()

        mockkObject(Endpoints.Auction.AUCTION_NONTRACKING)
        mockkObject(Endpoints.Event)
        mockkObject(Endpoints.Event.BANNER_SIZE)
        mockkObject(Endpoints.Event.CLICK)
        mockkObject(Endpoints.Event.CONFIG)
        mockkObject(Endpoints.Event.END_QUEUE)
        mockkObject(Endpoints.Event.EXPIRATION)
        mockkObject(Endpoints.Event.HELIUM_IMPRESSION)
        mockkObject(Endpoints.Event.INITIALIZATION)
        mockkObject(Endpoints.Event.LOAD)
        mockkObject(Endpoints.Event.PARTNER_IMPRESSION)
        mockkObject(Endpoints.Event.PREBID)
        mockkObject(Endpoints.Event.REWARD)
        mockkObject(Endpoints.Event.START_QUEUE)
        mockkObject(Endpoints.Event.WINNER)
        mockkObject(Endpoints.Sdk.SDK_INIT)

        every { mockAppSetIdInfo.scope } returns AppSetIdInfo.SCOPE_DEVELOPER
        every { mockAppSetIdInfo.id } returns APP_SET_ID
        every { mockAppSetIdInfoTask.isComplete } returns true
        every { mockAppSetIdInfoTask.isSuccessful } returns true
        every { mockAppSetIdInfoTask.result } returns mockAppSetIdInfo
        every { mockAppSetIdClient.appSetIdInfo } returns mockAppSetIdInfoTask
        every { AppSet.getClient(any()) } returns mockAppSetIdClient

        coEvery { Endpoints.Auction.AUCTION_NONTRACKING.endpoint } returns url + "${Endpoints.Auction.AUCTION_NONTRACKING.version}/auctions"
        coEvery { Endpoints.Event.BANNER_SIZE.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.BANNER_SIZE,
            )
        coEvery { Endpoints.Event.CLICK.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.CLICK,
            )
        coEvery { Endpoints.Event.CONFIG.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.CONFIG,
            )
        coEvery { Endpoints.Event.END_QUEUE.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.END_QUEUE,
            )
        coEvery { Endpoints.Event.EXPIRATION.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.EXPIRATION,
            )
        coEvery { Endpoints.Event.HELIUM_IMPRESSION.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.HELIUM_IMPRESSION,
            )
        coEvery { Endpoints.Event.INITIALIZATION.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.INITIALIZATION,
            )
        coEvery { Endpoints.Event.LOAD.endpoint } returns mockedEventUrl(url, Endpoints.Event.LOAD)
        coEvery { Endpoints.Event.PARTNER_IMPRESSION.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.PARTNER_IMPRESSION,
            )
        coEvery { Endpoints.Event.PREBID.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.PREBID,
            )
        coEvery { Endpoints.Event.REWARD.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.REWARD,
            )
        coEvery { Endpoints.Event.START_QUEUE.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.START_QUEUE,
            )
        coEvery { Endpoints.Event.WINNER.endpoint } returns
            mockedEventUrl(
                url,
                Endpoints.Event.WINNER,
            )
        coEvery { Endpoints.Sdk.SDK_INIT.endpoint } returns
            "${url}${Endpoints.Sdk.SDK_INIT.version}/${Endpoints.Sdk.SDK_INIT.name.lowercase()}"

        ChartboostMediationSdk.chartboostMediationInternal.testMode = true
        ChartboostMediationSdk.chartboostMediationInternal.appId = APP_ID
    }

    /*
        Chartboost Mediation Impression
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
    fun `trackClick`() = `verify trackClick`()

    @Test
    fun `trackReward`() = `verify trackReward`()

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
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

    fun setupBannerAdLoadParams(): AdLoadParams =
        AdLoadParams(
            adIdentifier = AdIdentifier(2, "AllNonProBanner (Banner)"),
            keywords = Keywords(),
            loadId = BANNER_LOAD_ID,
            bannerSize = ChartboostMediationBannerAdView.ChartboostMediationBannerSize.STANDARD,
            adInteractionListener = mockk(),
            partnerSettings = mockk(),
        )

    fun setupInterstitialAdLoadParams(): AdLoadParams =
        AdLoadParams(
            adIdentifier = AdIdentifier(0, "AllNonProInterstitial"),
            keywords = Keywords(),
            loadId = INTERSTITIAL_LOAD_ID,
            bannerSize = null,
            adInteractionListener = mockk(),
            partnerSettings = mockk(),
        )

    fun setupRewardedAdLoadParams(): AdLoadParams =
        AdLoadParams(
            adIdentifier = AdIdentifier(1, "AllNonProRewarded"),
            keywords = Keywords(),
            loadId = REWARDED_LOAD_ID,
            bannerSize = null,
            adInteractionListener = mockk(),
            partnerSettings = mockk(),
        )

    fun setupPrivacyController(): PrivacyController {
        val mockedContext = mockk<Context>()
        val mockedSharedPreferences = mockk<SharedPreferences>()
        val mockedEditor = mockk<SharedPreferences.Editor>()
        every { mockedContext.getSharedPreferences(any(), any()) } returns mockedSharedPreferences
        every { mockedContext.packageName } returns "com.chartboost.chartboostmediationsdk"
        every { mockedSharedPreferences.getString(any(), any()) } returns ""
        every { mockedSharedPreferences.getInt(any(), any()) } returns 0
        every { mockedSharedPreferences.edit() } returns mockedEditor
        every { mockedEditor.apply() } just Runs

        return PrivacyController(mockedContext)
    }

    fun setupPartnerController(): PartnerController =
        PartnerController().apply {
            adapters =
                mutableMapOf<String, PartnerAdapter>().also {
                    it["admob"] = mockk()
                }
            initStatuses =
                mutableMapOf<String, PartnerController.PartnerInitializationStatus>().also {
                    it["admob"] = PartnerController.PartnerInitializationStatus.INITIALIZED
                }
        }

    fun setupBidTokens(): Map<String, Map<String, String>> =
        mutableMapOf<String, Map<String, String>>().also {
            it["admob"] = mapOf()
        }

    fun getMockServerUrl(): String = mockWebServer.hostName

    companion object {
        const val APP_ID = "appidabc123"
        const val APP_SET_ID = "appsetidabc123"
        const val AUCTION_ID = "auctionidabc123"
        const val INIT_HASH_NEW = "inithashold123"
        const val INIT_HASH_OLD = "inithashnew123"
        const val SESSION_ID = "sessionidabc123"
        const val LOAD_ID = "loadidabc123"
        const val QUEUE_ID = "queueidabc123"
        const val BUILD_RELEASE = "33"

        const val BANNER_LOAD_ID = "bannerloadid123"
        const val INTERSTITIAL_LOAD_ID = "interstitialloadid123"
        const val REWARDED_LOAD_ID = "rewardedloadid123"

        const val BANNER_IMPRESSION_DEPTH = 1
        const val INTERSTITIAL_IMPRESSION_DEPTH = 2
        const val REWARDED_IMPRESSION_DEPTH = 3

        const val RATE_LIMIT_HEADER_VALUE = "ratelimitnew123"

        fun setMockEnvironment() {
            mockkObject(ChartboostCore)
            mockkStatic(ChartboostCore::class)
            val mockEnvironment: AnalyticsEnvironment = mockk()
            every { ChartboostCore.analyticsEnvironment } returns mockEnvironment
            every { mockEnvironment.appSessionIdentifier } returns SESSION_ID
            every { mockEnvironment.appSessionDurationSeconds } returns 1.0
            every { mockEnvironment.screenWidthPixels } returns 600
            every { mockEnvironment.screenHeightPixels } returns 800
            every { mockEnvironment.screenScale } returns 1.0f
            every { mockEnvironment.bundleIdentifier } returns "bundle.id"
            every { mockEnvironment.appVersion } returns "1.0.0"
            every { mockEnvironment.deviceMake } returns "gradle"
            every { mockEnvironment.deviceModel } returns "robolectric"
            every { mockEnvironment.isUserUnderage } returns false
            every { mockEnvironment.osName } returns "android"
            every { mockEnvironment.osVersion } returns "10.0"
            every { mockEnvironment.playerIdentifier } returns "playerId"
            every { mockEnvironment.frameworkName } returns null
            every { mockEnvironment.frameworkVersion } returns null
            coEvery { mockEnvironment.getUserAgent() } returns "useragent"
            coEvery { mockEnvironment.getLimitAdTrackingEnabled() } returns false
            coEvery { mockEnvironment.getVendorIdentifier() } returns APP_SET_ID
            coEvery { mockEnvironment.getVendorIdentifierScope() } returns VendorIdScope.APPLICATION
            coEvery { mockEnvironment.getAdvertisingIdentifier() } returns "ifa"
        }
    }

    private fun mockedEventUrl(
        url: String,
        endpoint: Endpoints.Event,
    ) = "${url}${endpoint.version}/event/${endpoint.name.lowercase()}"
}
