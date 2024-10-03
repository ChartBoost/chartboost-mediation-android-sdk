package com.chartboost.sdk.internal.AdUnitManager.parsers

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon
import com.chartboost.sdk.internal.Model.Asset
import com.chartboost.sdk.internal.Model.asList
import com.chartboost.sdk.internal.clickthrough.ClickPreference
import com.chartboost.sdk.internal.utils.Base64Wrapper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val ADID_JSON_FIELD = "ad_id"
private const val INFO_ICON_IMAGE_URL_JSON_FIELD = "imageurl"
private const val INFO_ICON_CLICKTHROUGH_URL_JSON_FIELD = "clickthroughUrl"
private const val INFO_ICON_POSITION_JSON_FIELD = "position"
private const val INFO_ICON_MARGIN_JSON_FIELD = "margin"
private const val INFO_ICON_PADDING_JSON_FIELD = "padding"
private const val INFO_ICON_SIZE_JSON_FIELD = "size"
private const val INFO_ICON_WIDTH_JSON_FIELD = "w"
private const val INFO_ICON_HEIGHT_JSON_FIELD = "h"
private const val CGN_JSON_FIELD = "cgn"
private const val CREATIVE_JSON_FIELD = "creative"
private const val DEEP_LINK_JSON_FIELD = "deep-link"
private const val LINK_JSON_FIELD = "link"
private const val TO_JSON_FIELD = "to"
private const val MEDIA_TYPE_JSON_FIELD = "media-type"
private const val NAME_JSON_FIELD = "name"
private const val WEBVIEW_JSON_FIELD = "webview"
private const val ELEMENTS_JSON_FIELD = "elements"
private const val EVENTS_ELEMENT = "events"
private const val TEMPLATE_ELEMENT = "template"
private const val PRECACHE_VIDEO_ELEMENT = "preCachedVideo"
private const val IMPRESSION_ID_ELEMENT = "impression_id"
private const val ADM_ELEMENT = "adm.js"
private const val REWARD_AMOUNT_ELEMENT = "reward_amount"
private const val REWARD_CURRENCY_ELEMENT = "reward_currency"
private const val HTML_ELEMENT = "html"
private const val BODY_ELEMENT = "body"
private const val NAME_ELEMENT = "name"
private const val TYPE_ELEMENT = "type"
private const val VALUE_ELEMENT = "value"
private const val PARAM_ELEMENT = "param"

internal const val BASE_URL_JSON_FIELD = "baseurl"
internal const val INFO_ICON_JSON_FIELD = "infoicon"
internal const val CLKP_JSON_FIELD = "clkp"
internal const val RENDERING_ENGINE_JSON_FIELD = "renderingengine"
internal const val SCRIPTS_JSON_FIELD = "scripts"

/**
 * Construct an AdUnit from a webview/v2
 * This class is not thread safe and you can only parse one response at a time
 */
internal class AdUnitParser(
    private val base64Wrapper: Base64Wrapper,
) {
    private var videoUrl = ""
    private var rewardAmount = 0
    private var rewardCurrency = ""
    private var impressionId = ""
    private var decodedAdm = ""

    @Throws(JSONException::class)
    fun parse(response: JSONObject?): AdUnit {
        response ?: throw JSONException("Missing response")
        with(response) {
            val assets = mutableMapOf<String, Asset>()
            val parameters = mutableMapOf<String, String>()
            var template: String
            getJSONObject(WEBVIEW_JSON_FIELD).let {
                parseElements(it.getJSONArray(ELEMENTS_JSON_FIELD), assets, parameters)
                template = it.getString(TEMPLATE_ELEMENT)
            }
            return AdUnit(
                name = optString(NAME_JSON_FIELD),
                adId = getString(ADID_JSON_FIELD),
                impressionId = impressionId,
                baseUrl = optString(BASE_URL_JSON_FIELD),
                infoIcon = parseInfoIcon(optJSONObject(INFO_ICON_JSON_FIELD)),
                cgn = getString(CGN_JSON_FIELD),
                creative = getString(CREATIVE_JSON_FIELD),
                mediaType = optString(MEDIA_TYPE_JSON_FIELD),
                assets = assets,
                videoUrl = videoUrl,
                videoFilename = retrieveFilenameFromVideoUrl(videoUrl),
                link = getString(LINK_JSON_FIELD),
                deepLink = optString(DEEP_LINK_JSON_FIELD),
                to = getString(TO_JSON_FIELD),
                rewardAmount = rewardAmount,
                rewardCurrency = rewardCurrency,
                template = template,
                body = assets[BODY_ELEMENT] ?: error("WebView AdUnit does not have a template html body asset"),
                parameters = parameters,
                renderingEngine = RenderingEngine.fromValue(optString(RENDERING_ENGINE_JSON_FIELD)),
                scripts = parseScripts(optJSONArray(SCRIPTS_JSON_FIELD)),
                events = parseEvents(optJSONObject(EVENTS_ELEMENT)),
                mtype = parseMtype(this.optInt("mtype")),
                clkp = ClickPreference.fromValue(optInt(CLKP_JSON_FIELD)),
                decodedAdm = decodedAdm,
            )
        }
    }

    /**
     * file parameters look like this.
     * The 'value' is the uri to be downloaded.
     * The 'name' goes in the formatted html.
     * {
     * "name": "0e0e81750d00e5e7721ddcf461baa06fbdd353e2.jpeg",
     * "type": "images",
     * "value": "https://a.chartboost.com/creatives/56f3d75bf6cd450398f699ec/0e0e81750d00e5e7721ddcf461baa06fbdd353e2.jpeg",
     * "param": "{% ad_landscape %}"
     * }
     *
     *
     * They don't always have a 'param' field, like so:
     * {
     * "name": "background3_portrait.jpg",
     * "type": "images",
     * "value": "https://img.serveroute.com/gow_mini_robotron_v1_5/img/Background/background3_portrait.jpg"
     * }
     *
     * @param elements
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun parseElements(
        elements: JSONArray,
        assets: MutableMap<String, Asset>,
        parameters: MutableMap<String, String>,
    ) {
        elements.asList<JSONObject>().forEach { element ->
            val elemName = element.getString(NAME_ELEMENT)
            val type = element.getString(TYPE_ELEMENT)
            val value = element.getString(VALUE_ELEMENT)
            var param = element.optString(PARAM_ELEMENT)

            when (type) {
                PRECACHE_VIDEO_ELEMENT -> {
                    videoUrl = value
                    return@forEach
                }
                PARAM_ELEMENT -> {
                    parameters[param] = value
                    when (elemName) {
                        REWARD_AMOUNT_ELEMENT -> parseReward(value)
                        REWARD_CURRENCY_ELEMENT -> rewardCurrency = value
                        IMPRESSION_ID_ELEMENT -> impressionId = value
                        ADM_ELEMENT -> decodedAdm = base64Wrapper.decode(value)
                    }
                    return@forEach
                }
                HTML_ELEMENT -> {
                    if (param.isEmpty()) {
                        param = BODY_ELEMENT
                    }
                }
                else -> {
                    if (param.isEmpty()) {
                        param = elemName
                    }
                }
            }
            assets[param] = Asset(type, elemName, value)
        }
    }

    private fun parseReward(value: String) {
        rewardAmount =
            try {
                value.toInt()
            } catch (e: NumberFormatException) {
                0
            }
    }

    private fun parseEvents(eventsJson: JSONObject?): MutableMap<String, List<String>> {
        val events: MutableMap<String, List<String>> = HashMap()
        eventsJson?.keys()?.forEach {
            val urlArray = eventsJson.getJSONArray(it)
            val urls: MutableList<String> = ArrayList()
            for (i in 0 until urlArray.length()) {
                urls.add(urlArray.getString(i))
            }
            events[it] = urls
        }
        return events
    }

    private fun parseInfoIcon(infoIconJsonObject: JSONObject?): InfoIcon {
        return infoIconJsonObject?.let { jsonObject ->
            InfoIcon(
                imageUrl = jsonObject.optString(INFO_ICON_IMAGE_URL_JSON_FIELD),
                clickthroughUrl = jsonObject.optString(INFO_ICON_CLICKTHROUGH_URL_JSON_FIELD),
                position =
                    InfoIcon.Position.parse(
                        jsonObject.optInt(INFO_ICON_POSITION_JSON_FIELD),
                    ),
                margin = parseInfoIconSize(jsonObject.optJSONObject(INFO_ICON_MARGIN_JSON_FIELD)),
                padding = parseInfoIconSize(jsonObject.optJSONObject(INFO_ICON_PADDING_JSON_FIELD)),
                size = parseInfoIconSize(jsonObject.optJSONObject(INFO_ICON_SIZE_JSON_FIELD)),
            )
        } ?: InfoIcon()
    }

    private fun parseScripts(scriptsJsonArray: JSONArray?): List<String> {
        return scriptsJsonArray?.asList() ?: emptyList()
    }

    private fun parseInfoIconSize(sizeJsonObject: JSONObject?): InfoIcon.DoubleSize {
        return sizeJsonObject?.let { sizeJson ->
            InfoIcon.DoubleSize(
                width = sizeJson.optDouble(INFO_ICON_WIDTH_JSON_FIELD),
                height = sizeJson.optDouble(INFO_ICON_HEIGHT_JSON_FIELD),
            )
        } ?: InfoIcon.DoubleSize()
    }
}

enum class MediaTypeOM(val intValue: Int) {
    UNKNOWN(0),
    HTML(1), // HTML_DISPLAY
    VIDEO(2), // VIDEO
    AUDIO(3), // AUDIO
    NATIVE(4), // NATIVE DISPLAY
}

internal enum class RenderingEngine(val value: String) {
    MRAID("mraid"),
    HTML("html"),
    VAST("vast"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromValue(value: String?): RenderingEngine {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
