package com.chartboost.sdk.internal.AdUnitManager.parsers

import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon
import com.chartboost.sdk.internal.Libraries.CBConstants.API_ENDPOINT
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.test.mockAndroidUri
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OpenRTBAdUnitParserTest {
    private val base64Wrapper = mockk<Base64Wrapper>()
    private val jsonInvalid = JSONObject("{\"testKey\":\"testValue\"}")

    private val adUnitInfoIcon =
        InfoIcon(
            imageUrl = "https://chartboost.com/some_icon.png",
            clickthroughUrl = "https://chartboost.com/opt_out.html",
            position = InfoIcon.Position.BOTTOM_RIGHT,
            margin =
                InfoIcon.DoubleSize(
                    width = 4.0,
                    height = 4.0,
                ),
            padding =
                InfoIcon.DoubleSize(
                    width = 5.0,
                    height = 5.0,
                ),
            size =
                InfoIcon.DoubleSize(
                    width = 16.0,
                    height = 16.0,
                ),
        )

    private val bannerAdUnitInfoIcon =
        InfoIcon(
            imageUrl = "https://chartboost.com/banner_some_icon.png",
            clickthroughUrl = "https://chartboost.com/banner_opt_out.html",
            position = InfoIcon.Position.TOP_RIGHT,
            margin =
                InfoIcon.DoubleSize(
                    width = 6.0,
                    height = 6.0,
                ),
            padding =
                InfoIcon.DoubleSize(
                    width = 7.0,
                    height = 7.0,
                ),
            size =
                InfoIcon.DoubleSize(
                    width = 17.0,
                    height = 17.0,
                ),
        )

    val json =
        JSONObject(
            """
        {
            "id": "cac2faea23eeecc135bf4e678116c0c339fa915f",
            "seatbid": [{
                "bid": [{
                    "id": "sbk-incubation-mode-bidid",
                    "impid": "sdk-incubation-mode-impid",
                    "price": 0.1,
                    "adid": "1",
                    "adm": "PGRpdiBpZD0iY29udGFpbmVyIiBzdHlsZT0id2lkdGg6MTAwJTsgaGVpZ2h0OjEwMCU7IGJhY2tncm91bmQtY29sb3I6IzAwMDAwMDsiPgogIDxhIGhyZWY9Imh0dHBzOi8vd3d3LmNoYXJ0Ym9vc3QuY29tIj4KICAgIDxpbWcKICAgICAgc3JjPSJodHRwczovL2EuY2hhcnRib29zdC5jb20vYmlkZGVyL2NyZWF0aXZlcy9kZWZhdWx0L2Jhbm5lci9hZC5qcGciCiAgICAgIHN0eWxlPSJkaXNwbGF5OmJsb2NrO2hlaWdodDoxMDAlO3dpZHRoOjEwMCUiCiAgICAgIGhlaWdodD0iNTAiCiAgICAgIHdpZHRoPSIzMjAiCiAgICAvPgo8L2E+CjwvZGl2Pg==",
                    "adomain": ["wwf2.ios.zynga.com"],
                    "bundle": "1196764367",
                    "cid": "94fe3fd0fb02c65ad2d415f1364824e55c0f3613",
                    "crid": "770458c0756801371a1422000a8873b9",
                    "ext": {
                        "baseurl": "$API_ENDPOINT",
                        "impressionid": "J6VSFc05OaZtNmFYI17we+tGZ9jKnWxXC7mT4DBDB7K1ReRf",
                        "infoicon": {
                            "position": "${adUnitInfoIcon.position.intValue}",
                            "margin": {
                                "w": "${adUnitInfoIcon.margin.width}",
                                "h": "${adUnitInfoIcon.margin.height}"
                            },
                            "padding": {
                                "w": "${adUnitInfoIcon.padding.width}",
                                "h": "${adUnitInfoIcon.padding.height}"
                            },
                            "imageurl": "${adUnitInfoIcon.imageUrl}",
                            "size": {
                                "w": "${adUnitInfoIcon.size.width}",
                                "h": "${adUnitInfoIcon.size.height}"
                            },
                            "clickthroughurl": "${adUnitInfoIcon.clickthroughUrl}"
                        },
                        "adId": "VyUdEMeZCkSbtF2ufD9uCJTXMriabw0M7BkceZGk6l854TPI780WjJIIA==",
                        "cgn": "1",
                        "crtype": "HTML5",
                        "template": "https://t.chartboost.com/base_templates/html/mraid-iframe-open-df82555530.html",
                        "trackingId": "sdk-incubation-trackingId",
                        "params": "{\"encoding\": \"base64\",\"isNativePlayer\": \"true\", \"ShowCloseButton\": \"true\", \"AdType\": \"Interstitial\"}",
                        "renderingengine": "html",
                        "scripts": [],
                        "videoUrl": "https://d1gnoa8d4rh1fn.cloudfront.net/19898/sniper3d_v_202010_104_en_portrait.mp4"
                    }
                }]
            }]
        }
        """.trim(),
        )

    val jsonBanner =
        JSONObject(
            """
        {
            "id": "sdk-test-mode-id",
            "seatbid": [{
                "bid": [{
                    "id": "sbk-incubation-mode-bidid",
                    "impid": "sdk-incubation-mode-impid",
                    "price": 0.1,
                    "adid": "1",
                    "adm": "PGRpdiBpZD0iY29udGFpbmVyIiBzdHlsZT0id2lkdGg6MTAwJTsgaGVpZ2h0OjEwMCU7IGJhY2tncm91bmQtY29sb3I6IzAwMDAwMDsiPgogIDxhIGhyZWY9Imh0dHBzOi8vd3d3LmNoYXJ0Ym9vc3QuY29tIj4KICAgIDxpbWcKICAgICAgc3JjPSJodHRwczovL2EuY2hhcnRib29zdC5jb20vYmlkZGVyL2NyZWF0aXZlcy9kZWZhdWx0L2Jhbm5lci9hZC5qcGciCiAgICAgIHN0eWxlPSJkaXNwbGF5OmJsb2NrO2hlaWdodDoxMDAlO3dpZHRoOjEwMCUiCiAgICAgIGhlaWdodD0iNTAiCiAgICAgIHdpZHRoPSIzMjAiCiAgICAvPgo8L2E+CjwvZGl2Pg==",
                    "adomain": ["wwf2.ios.zynga.com"],
                    "bundle": "1196764367",
                    "cid": "94fe3fd0fb02c65ad2d415f1364824e55c0f3613",
                    "crid": "770458c0756801371a1422000a8873b9",
                    "ext": {
                        "baseurl": "$API_ENDPOINT",
                        "impressionid": "J6VSFc05OaZtNmFYI17we+tGZ9jKnWxXC7mT4DBDB7K1ReRf",
                        "infoicon": {
                            "position": "${bannerAdUnitInfoIcon.position.intValue}",
                            "margin": {
                                "w": "${bannerAdUnitInfoIcon.margin.width}",
                                "h": "${bannerAdUnitInfoIcon.margin.height}"
                            },
                            "padding": {
                                "w": "${bannerAdUnitInfoIcon.padding.width}",
                                "h": "${bannerAdUnitInfoIcon.padding.height}"
                            },
                            "imageurl": "${bannerAdUnitInfoIcon.imageUrl}",
                            "size": {
                                "w": "${bannerAdUnitInfoIcon.size.width}",
                                "h": "${bannerAdUnitInfoIcon.size.height}"
                            },
                            "clickthroughurl": "${bannerAdUnitInfoIcon.clickthroughUrl}"
                        },
                        "adId": "1",
                        "cgn": "1",
                        "template": "https://t.chartboost.com/base_templates/html/mraid-iframe-open-b731aa95b3.html",
                        "trackingId": "sdk-banner-incubation-trackingId",
                        "renderingengine": "html",
                        "scripts": []
                    }
                }]
            }]
        }
        """.trim(),
        )

    private val openRTBAdUnitParser = OpenRTBAdUnitParser(base64Wrapper)

    @Before
    fun setup() {
        mockAndroidUri().run {
            every { pathSegments } returns emptyList()
        }
    }

    @Test
    fun `parse valid json`() {
        every { base64Wrapper.decode(any()) } returns "decoded adm"
        val adUnit = openRTBAdUnitParser.parse(AdType.Interstitial, json)
        assertNotNull(adUnit)
        assertEquals("", adUnit.name)
        assertEquals("VyUdEMeZCkSbtF2ufD9uCJTXMriabw0M7BkceZGk6l854TPI780WjJIIA==", adUnit.adId)
        assertEquals("J6VSFc05OaZtNmFYI17we+tGZ9jKnWxXC7mT4DBDB7K1ReRf", adUnit.impressionId)
        assertEquals("1", adUnit.cgn)
        assertEquals("", adUnit.creative)
        assertEquals("HTML5", adUnit.mediaType)
        assertEquals(2, adUnit.assets.size)
        assertEquals("https://d1gnoa8d4rh1fn.cloudfront.net/19898/sniper3d_v_202010_104_en_portrait.mp4", adUnit.videoUrl)
        assertEquals("", adUnit.videoFilename)
        assertEquals("", adUnit.link)
        assertEquals("", adUnit.deepLink)
        assertEquals("", adUnit.to)
        assertEquals(0, adUnit.rewardAmount)
        assertEquals("", adUnit.rewardCurrency)
        assertEquals("dummy_template", adUnit.template)
        assertEquals("https://t.chartboost.com/base_templates/html/mraid-iframe-open-df82555530.html", adUnit.body.url)
        assertEquals(6, adUnit.parameters.size)
        assertEquals(1, adUnit.events.size)
        assertEquals(
            "{\"encoding\": \"base64\",\"isNativePlayer\": \"true\", \"ShowCloseButton\": \"true\", \"AdType\": \"Interstitial\"}",
            adUnit.templateParams,
        )
        assertEquals("decoded adm", adUnit.decodedAdm)
    }

    @Test
    fun `parse valid banner json`() {
        every { base64Wrapper.decode(any()) } returns "decoded adm"
        val adUnit = openRTBAdUnitParser.parse(AdType.Banner, jsonBanner)
        assertNotNull(adUnit)
        assertEquals("", adUnit.name)
        assertEquals("1", adUnit.adId)
        assertEquals("https://live.chartboost.com", adUnit.baseUrl)
        assertEquals("J6VSFc05OaZtNmFYI17we+tGZ9jKnWxXC7mT4DBDB7K1ReRf", adUnit.impressionId)
        assertEquals(bannerAdUnitInfoIcon, adUnit.infoIcon)
        assertEquals("1", adUnit.cgn)
        assertEquals("", adUnit.creative)
        assertEquals("", adUnit.mediaType)
        assertEquals(2, adUnit.assets.size)
        assertEquals("", adUnit.videoUrl)
        assertEquals("", adUnit.videoFilename)
        assertEquals("", adUnit.link)
        assertEquals("", adUnit.deepLink)
        assertEquals("", adUnit.to)
        assertEquals(0, adUnit.rewardAmount)
        assertEquals("", adUnit.rewardCurrency)
        assertEquals("dummy_template", adUnit.template)
        assertEquals("https://t.chartboost.com/base_templates/html/mraid-iframe-open-b731aa95b3.html", adUnit.body.url)
        assertEquals(7, adUnit.parameters.size)
        assertEquals(1, adUnit.events.size)
        assertEquals("decoded adm", adUnit.decodedAdm)
    }

    @Test(expected = JSONException::class)
    fun `parse invalid data missing adid return empty adunit`() {
        openRTBAdUnitParser.parse(AdType.Interstitial, jsonInvalid)
    }

    @Test(expected = JSONException::class)
    fun `parse null returns empty ad unit`() {
        openRTBAdUnitParser.parse(AdType.Interstitial, null)
    }

    @Test
    fun `openRTB json is recognised properly`() {
        openRTBAdUnitParser.isOpenRTB(jsonBanner).shouldBeTrue()
    }

    @Test
    fun `invalid openRTB json is recognised properly`() {
        openRTBAdUnitParser.isOpenRTB(jsonInvalid).shouldBeFalse()
    }
}
