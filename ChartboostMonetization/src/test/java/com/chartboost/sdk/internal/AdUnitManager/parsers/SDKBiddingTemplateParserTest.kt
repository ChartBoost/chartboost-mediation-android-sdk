package com.chartboost.sdk.internal.AdUnitManager.parsers

import com.chartboost.sdk.test.TempDirectory
import com.chartboost.sdk.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class SDKBiddingTemplateParserTest {
    private val adm = "admdjasfejfiewjpfwljdewjfwepofjwepofewpoijerijferijferijferijfeijferijfepr"
    private val params = "{\"encoding\": \"base64\",\"isNativePlayer\": true, \"ShowCloseButton\": true, \"AdType\": \"Interstitial\"}"
    private val cacheDir = File(TempDirectory().directory, "cache")
    private val internalBaseDir = File(cacheDir, ".chartboost")
    private var file = File(internalBaseDir, "html/testtemplate")
    private val parser = SDKBiddingTemplateParser()

    @After
    fun teardown() {
        file.delete()
    }

    @Test
    fun `parse template for bidding`() {
        TestUtils.writeStringToFile(file, templateMraid)
        val template = parser.parse(file, params, adm)
        assertNotNull(template)
        assertTrue(template!!.contains(params))
        assertTrue(template.contains(adm))
    }

    @Test
    fun `parse template for bidding missing bidding params`() {
        TestUtils.writeStringToFile(file, templateMraidMissingBidding)
        val template = parser.parse(file, params, adm)
        assertNotNull(template)
        assertFalse(template!!.contains(params))
        assertFalse(template.contains(adm))
    }

    @Test
    fun `parse template for bidding empty template`() {
        val template = parser.parse(file, params, adm)
        assertNull(template)
    }
}

private const val templateMraidMissingBidding =
    "<!doctype html><html><head id=\"chartboost-main-head\"><title>Chartboost</title><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"user-scalable=no,width=device-width,initial-scale=1\"><script>var Chartboost = window.Chartboost || {};\n" +
        "      // We do this in oder to avoid any error caused by putting the app in the background\n" +
        "      // before the JS has been loaded. This will be overwritten later by the real function\n" +
        "      Chartboost.EventHandler = {\n" +
        "        handleNativeEvent: function () {},\n" +
        "      };\n" +
        "      Chartboost.Params = {\n" +
        "        AdType: \"{{ ad_type }}\",\n" +
        "        ShowCloseButton: \"{{ show_close_button }}\",\n" +
        "        CloseButtonCorner: \"{% close_button_corner %}\",\n" +
        "        deviceId: \"{% device_id %}\",\n" +
        "        encoding: \"{% encoding %}\",\n" +
        "        Crid: \"{% crid %}\",\n" +
        "        BidderId: \"{% bidder_id %}\",\n" +
        "        PublisherAppId: \"{% publisher_app_id %}\",\n" +
        "        // WARNING: This will be in the form of [\"moat\"]\n" +
        "        // therefore it needs to be wrapped in single quotes\n" +
        "        CertificationProviders: '{% certification_providers %}',\n" +
        "        AdDomain: \"{% ad_domain %}\",\n" +
        "        Geo: \"{% geo %}\",\n" +
        "        sdkVersion: \"{% sdk_version %}\",\n" +
        "        isMuted: \"{% is_muted %}\",\n" +
        "        isBanner: \"{% is_banner %}\",\n" +
        "        inlinedAssets: \"{% inlined_assets %}\",\n" +
        "        impressionId: \"{% impression_id %}\",\n" +
        "        templateSettings: \"{% template_settings %}\",\n" +
        "        GoogleFamilyApp: \"{% google_family_app %}\",\n" +
        "        LeftNotchPinholePosition: \"{% left_notch_pinhole_position %}\",\n" +
        "        RightNotchPinholePosition: \"{% right_notch_pinhole_position %}\",\n" +
        "        CentreNotchPinholePosition: \"{% centre_notch_pinhole_position %}\",\n" +
        "        isNativeAd: \"{% is_native_ad %}\",\n" +
        "      };</script><script>(()"

private const val templateMraid =
    "<!doctype html><html><head id=\"chartboost-main-head\"><title>Chartboost</title><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"user-scalable=no,width=device-width,initial-scale=1\"><script>var Chartboost = window.Chartboost || {};\n" +
        "      // We do this in oder to avoid any error caused by putting the app in the background\n" +
        "      // before the JS has been loaded. This will be overwritten later by the real function\n" +
        "      Chartboost.EventHandler = {\n" +
        "        handleNativeEvent: function () {},\n" +
        "      };\n" +
        "      Chartboost.BiddingParams = \"{% params %}\";\n" +
        "      Chartboost.Params = {\n" +
        "        AdType: \"{{ ad_type }}\",\n" +
        "        ShowCloseButton: \"{{ show_close_button }}\",\n" +
        "        CloseButtonCorner: \"{% close_button_corner %}\",\n" +
        "        deviceId: \"{% device_id %}\",\n" +
        "        encoding: \"{% encoding %}\",\n" +
        "        Crid: \"{% crid %}\",\n" +
        "        BidderId: \"{% bidder_id %}\",\n" +
        "        PublisherAppId: \"{% publisher_app_id %}\",\n" +
        "        // WARNING: This will be in the form of [\"moat\"]\n" +
        "        // therefore it needs to be wrapped in single quotes\n" +
        "        CertificationProviders: '{% certification_providers %}',\n" +
        "        AdDomain: \"{% ad_domain %}\",\n" +
        "        Geo: \"{% geo %}\",\n" +
        "        sdkVersion: \"{% sdk_version %}\",\n" +
        "        isMuted: \"{% is_muted %}\",\n" +
        "        adm: \"{% adm %}\",\n" +
        "        isBanner: \"{% is_banner %}\",\n" +
        "        inlinedAssets: \"{% inlined_assets %}\",\n" +
        "        impressionId: \"{% impression_id %}\",\n" +
        "        templateSettings: \"{% template_settings %}\",\n" +
        "        GoogleFamilyApp: \"{% google_family_app %}\",\n" +
        "        LeftNotchPinholePosition: \"{% left_notch_pinhole_position %}\",\n" +
        "        RightNotchPinholePosition: \"{% right_notch_pinhole_position %}\",\n" +
        "        CentreNotchPinholePosition: \"{% centre_notch_pinhole_position %}\",\n" +
        "        isNativeAd: \"{% is_native_ad %}\",\n" +
        "      };</script><script>(()"
