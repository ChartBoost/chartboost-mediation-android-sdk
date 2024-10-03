package com.chartboost.sdk.internal.AdUnitManager.data

import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.parsers.RenderingEngine
import com.chartboost.sdk.internal.Libraries.CBConstants.API_ENDPOINT
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Libraries.safePut
import com.chartboost.sdk.internal.Model.Asset
import com.chartboost.sdk.internal.clickthrough.ClickPreference

private const val AD_MARKUP_WRAPPER = "Wrapper"
private const val AD_MARKUP_INLINE = "Inline"
private const val VAST_XML = "<VAST "

internal data class AdUnit(
    var name: String = "",
    var adId: String = "",
    val baseUrl: String = API_ENDPOINT,
    var impressionId: String = "",
    val infoIcon: InfoIcon = InfoIcon(),
    var cgn: String = "",
    var creative: String = "",
    var mediaType: String = "",
    val assets: MutableMap<String, Asset> = HashMap(),
    var videoUrl: String = "",
    var videoFilename: String = "",
    var link: String = "",
    var deepLink: String = "",
    var to: String = "",
    var rewardAmount: Int = 0,
    var rewardCurrency: String = "",
    var template: String = "",
    var body: Asset = Asset("", "", ""),
    val parameters: MutableMap<String, String> = HashMap(),
    val renderingEngine: RenderingEngine = RenderingEngine.UNKNOWN,
    val scripts: List<String> = emptyList(),
    val events: MutableMap<String, List<String>> = HashMap(),
    val adm: String = "",
    val templateParams: String = "",
    val mtype: MediaTypeOM = MediaTypeOM.UNKNOWN,
    val clkp: ClickPreference = ClickPreference.CLICK_PREFERENCE_EMBEDDED,
    val decodedAdm: String = "",
) {
    val isPrecacheVideoAd: Boolean = videoUrl.isNotEmpty() && videoFilename.isNotEmpty()

    fun getAdMarkup(): String {
        if (decodedAdm.isEmpty()) {
            return ""
        }

        return if (decodedAdm.contains(VAST_XML, ignoreCase = true)) {
            AD_MARKUP_WRAPPER
        } else {
            AD_MARKUP_INLINE
        }
    }

    fun getParametersAsString(): String {
        return CBJSON.jsonObject().apply {
            joinMaps().forEach { (key, value) -> safePut(key, value) }
        }.toString()
    }

    private fun joinMaps(): Map<String, String> {
        return parameters + assets.map { (key, asset) -> key to "${asset.directory}/${asset.filename}" }
    }
}
