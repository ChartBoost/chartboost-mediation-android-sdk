package com.chartboost.sdk.internal.AdUnitManager.parsers

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon
import com.chartboost.sdk.internal.Libraries.CBConstants.API_ENDPOINT
import com.chartboost.sdk.internal.Model.Asset
import com.chartboost.sdk.internal.Model.asList
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.ClickPreference
import com.chartboost.sdk.internal.utils.Base64Wrapper
import org.json.JSONException
import org.json.JSONObject

private const val EXTRA_PARAM_ENCODING = "{% encoding %}"
private const val EXTRA_PARAM_ADM = "{% adm %}"
private const val EXTRA_PARAM_ADTYPE = "{{ ad_type }}"
private const val EXTRA_PARAM_CLOSE_BTN = "{{ show_close_button }}"
private const val EXTRA_PARAM_PREROLL_POPUP = "{{ preroll_popup }}"
private const val EXTRA_PARAM_REWARD_TOASTER_ENABLED = "{{ post_video_reward_toaster_enabled }}"
private const val EXTRA_PARAM_REWARD_IS_BANNER = "{% is_banner %}"
private const val EMPTY_VALUE = ""
private const val PARAM_ENABLED_FALSE = "false"
private const val PARAM_ENABLED_TRUE = "true"
private const val PARAM_ENCODING_BASE64 = "base64"
private const val BODY_KEY = "body"
private const val IMPTRACKER_KEY = "imptrackers"
private const val ASSET_DIRECTORY = "html"

const val AD_TYPE_OPEN_RTB_INTERSTITIAL = "8"
const val AD_TYPE_OPEN_RTB_REWARDED = "9"
const val AD_TYPE_OPEN_RTB_BANNER = "10"

internal class OpenRTBAdUnitParser(
    private val base64Wrapper: Base64Wrapper,
) {
    @Throws(JSONException::class)
    fun parse(
        adType: AdType,
        response: JSONObject?,
    ): AdUnit {
        response ?: throw JSONException("Missing response")
        parseOpenRtbResponse(response).apply {
            val bid = seatbidList.firstSeatbid().bidList.firstBid()
            val ext = bid.ext
            val body = assets.firstAsset()
            val assets = assetsAsMap.apply { put(BODY_KEY, body) }
            val videoUrl = ext.videoUrl
            val videoFilename = retrieveFilenameFromVideoUrl(videoUrl)
            val events = mutableMapOf<String, List<String>>().apply { put(IMPTRACKER_KEY, ext.imptrackers) }
            val parameters = mutableMapOf<String, String>().apply { assignExtraParameters(bid, adType) }
            return AdUnit(
                name = "",
                adId = ext.adId,
                baseUrl = ext.baseUrl,
                impressionId = ext.impressionid,
                infoIcon = ext.infoIcon,
                cgn = ext.cgn,
                creative = EMPTY_VALUE,
                mediaType = ext.crtype,
                assets = assets,
                videoUrl = videoUrl,
                videoFilename = videoFilename,
                link = "",
                deepLink = "",
                to = "",
                rewardAmount = 0,
                rewardCurrency = "",
                template = "dummy_template",
                body = body,
                parameters = parameters,
                renderingEngine = ext.renderEngine,
                scripts = ext.scripts,
                events = events,
                adm = bid.adm,
                templateParams = ext.params,
                mtype = parseMtype(bid.mtype),
                clkp = ClickPreference.fromValue(ext.clkp),
                decodedAdm = base64Wrapper.decode(bid.adm),
            )
        }
    }

    private fun MutableMap<String, String>.assignExtraParameters(
        bid: BidModel,
        adType: AdType,
    ) {
        this[EXTRA_PARAM_ENCODING] = PARAM_ENCODING_BASE64
        this[EXTRA_PARAM_ADM] = bid.adm
        this[EXTRA_PARAM_ADTYPE] = createAdOpenRTBType(adType)
        this[EXTRA_PARAM_CLOSE_BTN] = adType.asCloseBtnParam()
        this[EXTRA_PARAM_PREROLL_POPUP] = PARAM_ENABLED_FALSE
        this[EXTRA_PARAM_REWARD_TOASTER_ENABLED] = PARAM_ENABLED_FALSE
        if (adType == AdType.Banner) {
            this[EXTRA_PARAM_REWARD_IS_BANNER] = PARAM_ENABLED_TRUE
        }
    }

    private fun AdType.asCloseBtnParam(): String =
        when (this) {
            AdType.Interstitial -> PARAM_ENABLED_TRUE
            AdType.Rewarded, AdType.Banner -> PARAM_ENABLED_FALSE
        }

    private fun createAdOpenRTBType(adType: AdType): String {
        return when (adType) {
            AdType.Banner -> AD_TYPE_OPEN_RTB_BANNER
            AdType.Interstitial -> AD_TYPE_OPEN_RTB_INTERSTITIAL
            AdType.Rewarded -> AD_TYPE_OPEN_RTB_REWARDED
        }
    }

    private fun List<Asset>.firstAsset(): Asset = firstOrNull() ?: Asset("", "", "")

    private fun List<SeatbidModel>.firstSeatbid(): SeatbidModel = firstOrNull() ?: SeatbidModel()

    private fun List<BidModel>.firstBid(): BidModel = firstOrNull() ?: BidModel()

    @Throws(JSONException::class)
    private fun parseOpenRtbResponse(response: JSONObject): OpenRTBModel {
        val assets = mutableListOf<Asset>()
        val seatBidArray = response.optJSONArray("seatbid")
        var extModel = ExtensionModel()
        val bidModelList = mutableListOf<BidModel>()
        val seatbidModelList = mutableListOf<SeatbidModel>()
        seatBidArray?.asList<JSONObject>()?.forEach { seatBid ->
            val seat = seatBid.optString("seat")
            val bidArray = seatBid.optJSONArray("bid")
            bidArray?.asList<JSONObject>()?.forEach { bid ->
                bid.optJSONObject("ext")?.let {
                    extModel = buildExtensionModel(it)
                    extModel.template.extractTemplateAsset()?.let { asset ->
                        assets.add(asset)
                    }
                }
                bidModelList.add(buildBidModel(bid, extModel))
            }
            seatbidModelList.add(SeatbidModel(seat, bidModelList))
        }
        return buildOpenRTB(response, seatbidModelList, assets)
    }

    private fun String?.extractTemplateAsset(): Asset? {
        return if (isNullOrEmpty()) {
            null
        } else {
            val filename = substring(lastIndexOf('/') + 1)
            Asset(ASSET_DIRECTORY, filename, this)
        }
    }

    @Throws(JSONException::class)
    private fun buildExtensionModel(ext: JSONObject): ExtensionModel {
        return ExtensionModel(
            ext.optString("impressionid"),
            ext.optString("crtype"),
            ext.optString("adId"),
            ext.optString("cgn"),
            ext.getString("template"),
            ext.optString("videoUrl"),
            ext.optJSONArray("imptrackers")?.asList() ?: emptyList(),
            ext.optString("params"),
            ext.optInt(CLKP_JSON_FIELD),
            ext.optString(BASE_URL_JSON_FIELD),
            ext.optJSONObject(INFO_ICON_JSON_FIELD)?.let(::buildInfoIcon) ?: InfoIcon(),
            RenderingEngine.fromValue(ext.optString(RENDERING_ENGINE_JSON_FIELD)),
            ext.optJSONArray(SCRIPTS_JSON_FIELD)?.asList() ?: emptyList(),
        )
    }

    @Throws(JSONException::class)
    private fun buildBidModel(
        bid: JSONObject,
        ext: ExtensionModel,
    ): BidModel {
        return BidModel(
            bid.getString("id"),
            bid.getString("impid"),
            bid.getDouble("price"),
            bid.optString("burl"),
            bid.optString("crid"),
            bid.optString("adm"),
            bid.optInt("mtype"),
            ext,
        )
    }

    @Throws(JSONException::class)
    private fun buildOpenRTB(
        response: JSONObject,
        seatbid: List<SeatbidModel>,
        assets: List<Asset>,
    ): OpenRTBModel {
        return OpenRTBModel(
            response.getString("id"),
            response.optString("nbr"),
            response.optString("cur", "USD"),
            response.optString("bidid"),
            seatbid,
            assets,
        )
    }

    @Throws(JSONException::class)
    private fun buildSizeDoubleModel(sizedouble: JSONObject): InfoIcon.DoubleSize {
        return InfoIcon.DoubleSize(
            width = sizedouble.optDouble("w"),
            height = sizedouble.optDouble("h"),
        )
    }

    @Throws(JSONException::class)
    private fun buildInfoIcon(infoIcon: JSONObject): InfoIcon {
        return InfoIcon(
            imageUrl = infoIcon.optString("imageurl"),
            clickthroughUrl = infoIcon.optString("clickthroughurl"),
            position = InfoIcon.Position.parse(infoIcon.optInt("position")),
            margin = infoIcon.optJSONObject("margin")?.let(::buildSizeDoubleModel) ?: InfoIcon.DoubleSize(),
            padding = infoIcon.optJSONObject("padding")?.let(::buildSizeDoubleModel) ?: InfoIcon.DoubleSize(),
            size = infoIcon.optJSONObject("size")?.let(::buildSizeDoubleModel) ?: InfoIcon.DoubleSize(),
        )
    }

    fun isOpenRTB(response: JSONObject): Boolean =
        try {
            val openRTBModel = parseOpenRtbResponse(response)
            openRTBModel.seatbidList.isNotEmpty()
        } catch (e: Exception) {
            false
        }

    data class OpenRTBModel(
        var id: String = "",
        var nbr: String = "",
        var currency: String = "USD",
        var bidId: String = "",
        var seatbidList: List<SeatbidModel> = emptyList(),
        var assets: List<Asset> = emptyList(),
    ) {
        val assetsAsMap: MutableMap<String, Asset>
            get() = assets.associateBy { it.filename }.toMutableMap()
    }

    data class BidModel(
        val id: String = "",
        val impid: String = "",
        val price: Double = 0.0,
        val burl: String = "",
        val crid: String = "",
        val adm: String = "",
        val mtype: Int = 0,
        val ext: ExtensionModel = ExtensionModel(),
    )

    data class ExtensionModel(
        val impressionid: String = "",
        val crtype: String = "",
        val adId: String = "",
        val cgn: String = "",
        val template: String = "",
        val videoUrl: String = "",
        val imptrackers: List<String> = emptyList(),
        val params: String = "",
        val clkp: Int = ClickPreference.CLICK_PREFERENCE_EMBEDDED.value,
        val baseUrl: String = API_ENDPOINT,
        val infoIcon: InfoIcon = InfoIcon(),
        val renderEngine: RenderingEngine = RenderingEngine.UNKNOWN,
        val scripts: List<String> = emptyList(),
    )

    data class SeatbidModel(
        val seat: String = "",
        val bidList: List<BidModel> = emptyList(),
    )
}
