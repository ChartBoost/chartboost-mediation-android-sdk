package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.PlayServices.BaseTest
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Model.SdkConfiguration.PrivacyStandardsConfig
import com.chartboost.sdk.internal.Networking.AdParameters
import com.chartboost.sdk.internal.WebView.UserAgentHelper.webViewUserAgentValue
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.utils.DeviceInfo
import com.chartboost.sdk.privacy.model.CCPA
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.privacy.model.Custom
import com.chartboost.sdk.privacy.model.DataUseConsent
import com.chartboost.sdk.privacy.model.GDPR
import com.chartboost.sdk.privacy.model.GDPR.GDPR_CONSENT.Companion.fromValue
import com.chartboost.sdk.privacy.model.LGPD
import com.chartboost.sdk.test.TestContainer
import com.chartboost.sdk.test.TestContainerBuilder
import com.chartboost.sdk.test.TestContainerControl
import com.chartboost.sdk.test.TestDisplayMetrics
import com.chartboost.sdk.test.TestUtils
import com.iab.omid.library.chartboost.adsession.Partner
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.Locale
import java.util.Random

class OpenRTBTest : BaseTest() {
    private val openMeasurementManagerMock = mockk<OpenMeasurementManager>()

    @Before
    fun setup() {
        ChartboostDependencyContainer.start("appid", "signature")
        every { openMeasurementManagerMock.isOmSdkEnabled() } returns true
        every { openMeasurementManagerMock.getOmidPartner() } returns
            Partner.createPartner(
                "chartboost",
                "9.3.0",
            )
    }

    private fun webViewTestContainer(control: TestContainerControl): TestContainer {
        return TestContainerBuilder(control).build()
    }

    @Test
    fun appendRequestBodyOpenRTBParams() {
        appendRequestBodyOpenRTBParams(true)
        every { openMeasurementManagerMock.isOmSdkEnabled() } returns false
        appendRequestBodyOpenRTBParams(false)
    }

    private fun appendRequestBodyOpenRTBParams(isOmEnabled: Boolean) {
        val seeds = intArrayOf(1, 4352, 56747, 24124, 34523)
        val control = TestContainerControl.defaultNative()
        val height = 50
        val width = 320
        val location = "a location"
        val impDepth = 1
        for (seed in seeds) {
            val r = Random(seed.toLong())
            val portrait = r.nextBoolean()
            val density = r.nextFloat()
            val displayMetrics =
                (if (portrait) TestDisplayMetrics.portrait() else TestDisplayMetrics.landscape())
                    .withDensity(density)
                    .withDensityDpi(r.nextInt(1000) + 1)
            control.setDisplayMetrics(displayMetrics)
            try {
                webViewTestContainer(control).use { tc ->
                    val consentList: MutableList<DataUseConsent> = ArrayList()
                    val coppa = COPPA(true)
                    consentList.add(coppa)
                    consentList.add(LGPD(true))
                    consentList.add(CCPA(CCPA.CCPA_CONSENT.OPT_OUT_SALE))
                    consentList.add(Custom("test_privacy", "test_consent"))
                    val configMock = mockk<PrivacyStandardsConfig>()
                    val privacyBodyFields =
                        PrivacyBodyFields(0, consentList, 0, 1, JSONObject(), "-1")

                    Mockito.lenient().`when`(tc.privacyApi.toPrivacyBodyFields())
                        .thenReturn(privacyBodyFields)

//                    val privacyMock = relaxedMockk<PrivacyApi>()
//                    every { privacyMock.toPrivacyBodyFields() } returns privacyBodyFields
//                    tc.privacyApi = privacyMock

                    Mockito.`when`(
                        tc.session.getSessionImpressionsCounter(
                            ArgumentMatchers.any(
                                AdType::class.java,
                            ),
                        ),
                    ).thenReturn(1)

//                    val sessionMock = relaxedMockk<Session>()
//                    every { sessionMock.getSessionImpressionsCounter(any<AdType>()) } returns 1
//                    tc.session = sessionMock

                    val requestBodyFields = tc.requestBodyBuilder.build()
                    val openRTBRequestModel =
                        OpenRTBRequestModel(
                            requestBodyFields,
                            AdParameters(AdType.Banner, height, width, location, impDepth),
                            openMeasurementManagerMock,
                        )
                    val body = openRTBRequestModel.jsonRepresentation
                    MatcherAssert.assertThat(
                        TestUtils.toStringList(body.names()),
                        Matchers.containsInAnyOrder(
                            "id", "test", "cur", "at", "device", "imp", "app", "regs", "user",
                        ),
                    )
                    Assert.assertEquals(body["id"], JSONObject.NULL)
                    Assert.assertEquals(body["test"], JSONObject.NULL)
                    Assert.assertEquals(body["cur"].toString(), JSONArray().put("USD").toString())
                    Assert.assertEquals(body["at"], 2)

                    // Device
                    val device = body["device"] as JSONObject
                    Assert.assertEquals(device["make"], "unknown")
                    Assert.assertEquals(
                        device["devicetype"],
                        DeviceInfo.getOpenRTBDeviceType(tc.applicationContext),
                    )
                    Assert.assertEquals(device["w"], 768)
                    Assert.assertEquals(device["h"], 1024)
                    Assert.assertEquals(device["ifa"], requestBodyFields.identityBodyFields.gaid)
                    // osv not added because Build.VERSION.RELEASE is null in unit tests
                    Assert.assertEquals(
                        device["lmt"],
                        requestBodyFields.identityBodyFields.trackingState.value,
                    )
                    Assert.assertEquals(
                        device["connectiontype"],
                        requestBodyFields.reachabilityBodyFields.openRTBConnectionType.value,
                    )
                    Assert.assertEquals(device["os"], "Android")
                    val geo = JSONObject()
                    geo.put("lat", JSONObject.NULL)
                    geo.put("lon", JSONObject.NULL)
                    geo.put("country", Locale.getDefault().country)
                    geo.put("type", 2) // IP inferred
                    Assert.assertEquals(device["geo"].toString(), geo.toString())
                    Assert.assertEquals(device["ip"], JSONObject.NULL)
                    Assert.assertEquals(device["language"], Locale.getDefault().language)
                    Assert.assertEquals(device["ua"], webViewUserAgentValue)
                    Assert.assertEquals(device["model"], requestBodyFields.REQUEST_PARAM_MODEL)
                    Assert.assertEquals(device["carrier"], tc.control.constants.carrierName)
                    val extDevice = JSONObject()
                    extDevice.put("appsetid", "a setId")
                    extDevice.put("appsetidscope", 1)
                    if (isOmEnabled) {
                        extDevice.put("omidpn", "chartboost")
                        extDevice.put("omidpv", "9.3.0")
                    }
                    Assert.assertEquals(device["ext"].toString(), extDevice.toString())

                    // Imp
                    val imp = body.getJSONArray("imp")
                    val impression = imp[0] as JSONObject
                    val banner = impression["banner"] as JSONObject
                    Assert.assertEquals(banner["h"], height)
                    Assert.assertEquals(banner["w"], width)
                    Assert.assertEquals(banner["btype"], JSONObject.NULL)
                    Assert.assertEquals(banner["battr"], JSONObject.NULL)
                    Assert.assertEquals(banner["pos"], JSONObject.NULL)
                    Assert.assertEquals(banner["topframe"], JSONObject.NULL)
                    Assert.assertEquals(banner["api"], JSONObject.NULL)
                    val ext = JSONObject()
                    ext.put("placementtype", "banner")
                    ext.put("playableonly", JSONObject.NULL)
                    ext.put("allowscustomclosebutton", JSONObject.NULL)
                    Assert.assertEquals(banner["ext"].toString(), ext.toString())
                    Assert.assertEquals(impression["instl"], 0)
                    Assert.assertEquals(impression["tagid"], location)
                    Assert.assertEquals(impression["displaymanager"], "Chartboost-Android-SDK")
                    Assert.assertEquals(impression["displaymanagerver"], CBConstants.SDK_VERSION)
                    Assert.assertEquals(impression["bidfloor"], JSONObject.NULL)
                    Assert.assertEquals(impression["bidfloorcur"], "USD")
                    Assert.assertEquals(impression["secure"], 1)

                    // App
                    val app = body["app"] as JSONObject
                    Assert.assertEquals(app["id"], tc.appId)
                    Assert.assertEquals(app["name"], JSONObject.NULL)
                    Assert.assertEquals(app["bundle"], tc.applicationContext.packageName)
                    Assert.assertEquals(app["storeurl"], JSONObject.NULL)
                    val publisher = JSONObject()
                    CBJSON.put(publisher, "id", JSONObject.NULL)
                    CBJSON.put(publisher, "name", JSONObject.NULL)
                    Assert.assertEquals(app["publisher"].toString(), publisher.toString())
                    Assert.assertEquals(app["cat"], JSONObject.NULL)

                    // Regs
                    val regs = body.getJSONObject("regs")
                    Assert.assertEquals(regs["coppa"], 1)
                    Assert.assertEquals(0, (regs["ext"] as JSONObject)["gdpr"])
                    Assert.assertEquals("1YY-", (regs["ext"] as JSONObject)["us_privacy"])
                    Assert.assertEquals("test_consent", (regs["ext"] as JSONObject)["test_privacy"])
                    Assert.assertEquals(
                        "",
                        (regs["ext"] as JSONObject).optString("coppa"),
                    ) // coppa should not be included in the regs.ext since it has its own field directly in the regs
                    Assert.assertEquals(true, (regs["ext"] as JSONObject)["lgpd"])

                    // User
                    val user = body.getJSONObject("user")
                    Assert.assertEquals(user["id"], JSONObject.NULL)
                    Assert.assertEquals((user["ext"] as JSONObject)["consent"], 0)
                }
            } catch (e: JSONException) {
                Assert.fail("Should not have thrown any exception but found " + e.message)
            }
        }
    }

    @Test
    fun appendRequestBodyOpenRTBConsentGDPRDifferentParamsNonBehavioral() {
        appendRequestBodyOpenRTBConsent(GDPR.GDPR_CONSENT.NON_BEHAVIORAL.value, 1)
    }

    @Test
    fun appendRequestBodyOpenRTBConsentGDPRDifferentParamsBehavioral() {
        appendRequestBodyOpenRTBConsent(GDPR.GDPR_CONSENT.BEHAVIORAL.value, 1)
    }

    @Test(expected = NullPointerException::class)
    fun appendRequestBodyOpenRTBConsentGDPRDifferentParamsUnknown() {
        appendRequestBodyOpenRTBConsent("-1", 0)
    }

    fun appendRequestBodyOpenRTBConsent(
        consent: String,
        expectedConsent: Int,
    ) {
        val control = TestContainerControl.defaultNative()
        try {
            webViewTestContainer(control).use { tc ->
                val gdpr: DataUseConsent =
                    GDPR(
                        fromValue(consent)!!,
                    )
                val ccpa: DataUseConsent = CCPA(CCPA.CCPA_CONSENT.OPT_OUT_SALE)
                val coppa: DataUseConsent = COPPA(true)
                val lgpd: DataUseConsent = LGPD(true)
                val custom: DataUseConsent = Custom("test_privacy", "test_consent")
                val consentsList: MutableList<DataUseConsent> = ArrayList()
                consentsList.add(ccpa)
                consentsList.add(coppa)
                consentsList.add(lgpd)
                consentsList.add(custom)

                Mockito.`when`(tc.privacyApi.toPrivacyBodyFields()).thenReturn(
                    PrivacyBodyFields(
                        expectedConsent,
                        consentsList,
                        expectedConsent,
                        1,
                        JSONObject(),
                        "",
                    ),
                )

//                val privacyMock = relaxedMockk<PrivacyApi>()
//                every { privacyMock.toPrivacyBodyFields() } returns PrivacyBodyFields(
//                    expectedConsent,
//                    consentsList,
//                    expectedConsent,
//                    1,
//                    JSONObject(),
//                    "",
//                )
//                tc.privacyApi = privacyMock

                if (consent != "-1") {
                    tc.privacyApi.putPrivacyStandard(gdpr)
                } else {
                    tc.privacyApi.removePrivacyStandard(GDPR.GDPR_STANDARD)
                }
                tc.privacyApi.putPrivacyStandard(ccpa)
                tc.privacyApi.putPrivacyStandard(coppa)
                tc.privacyApi.putPrivacyStandard(lgpd)
                tc.privacyApi.putPrivacyStandard(custom)
                val openRTBRequestModel =
                    OpenRTBRequestModel(
                        tc.requestBodyBuilder.build(),
                        AdParameters(AdType.Banner, 100, 100, "test", 1),
                        openMeasurementManagerMock,
                    )
                val body = openRTBRequestModel.jsonRepresentation
                MatcherAssert.assertThat(
                    TestUtils.toStringList(body.names()),
                    Matchers.containsInAnyOrder(
                        "id", "test", "cur", "at", "device", "imp", "app", "regs", "user",
                    ),
                )

                // Regs
                val regs = body.getJSONObject("regs")
                Assert.assertEquals(regs["coppa"], 1)
                Assert.assertEquals(expectedConsent, (regs["ext"] as JSONObject)["gdpr"])
                Assert.assertEquals((regs["ext"] as JSONObject)["us_privacy"], "1YY-")
                Assert.assertEquals((regs["ext"] as JSONObject)["test_privacy"], "test_consent")
                Assert.assertEquals((regs["ext"] as JSONObject)["lgpd"], true)
                val user = body.getJSONObject("user")
                Assert.assertNotNull(user)
                val extUser = user.getJSONObject("ext")
                Assert.assertNotNull(extUser)
                val impdepth = extUser.getInt("impdepth")
                Assert.assertEquals(1, impdepth.toLong())
            }
        } catch (e: JSONException) {
            Assert.fail("Should not have thrown any exception but found " + e.message)
        }
    }
}
