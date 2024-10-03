package com.chartboost.sdk.internal.Model

import com.chartboost.sdk.internal.measurement.VisibilityTracker
import org.json.JSONObject

private const val OM_ENABLED = "enabled"
private const val VERIFICATION_ENABLED = "verificationEnabled"
private const val VIEWABILITY_SETTINGS = "viewabilitySettings"
private const val MIN_VISIBLE_DIPS = "minVisibleDips"
private const val MIN_VISIBLE_DURATION_MS = "minVisibleDurationMs"
private const val VISIBILITY_CHECK_INTERVAL_MS = "visibilityCheckIntervalMs"
private const val TRAVERSAL_LIMIT = "traversalLimit"

data class OmSdkModel(
    var isEnabled: Boolean = false,
    var verificationEnabled: Boolean = false,
    var minVisibleDips: Int = VisibilityTracker.MIN_VISIBLE_DIPS,
    var minVisibleDurationMs: Int = VisibilityTracker.MIN_VISIBLE_DURATION_MS,
    var visibilityCheckIntervalMs: Long = VisibilityTracker.VISIBILITY_CHECK_INTERVAL_MS,
    var traversalLimit: Int = VisibilityTracker.TRAVERSAL_LIMIT,
    var verificationList: List<VerificationModel>? = null,
)

fun jsonToOmSdkModel(config: JSONObject): OmSdkModel {
    val verificationList = config.asVerificationModelList()
    return config.optJSONObject(VIEWABILITY_SETTINGS)?.let {
        OmSdkModel(
            isEnabled = config.optBoolean(OM_ENABLED, false),
            verificationEnabled = config.optBoolean(VERIFICATION_ENABLED, false),
            minVisibleDips =
                it.optInt(
                    MIN_VISIBLE_DIPS,
                    VisibilityTracker.MIN_VISIBLE_DIPS,
                ),
            minVisibleDurationMs =
                it.optInt(
                    MIN_VISIBLE_DURATION_MS,
                    VisibilityTracker.MIN_VISIBLE_DURATION_MS,
                ),
            visibilityCheckIntervalMs =
                it.optLong(
                    VISIBILITY_CHECK_INTERVAL_MS,
                    VisibilityTracker.VISIBILITY_CHECK_INTERVAL_MS,
                ),
            traversalLimit =
                it.optInt(
                    TRAVERSAL_LIMIT,
                    VisibilityTracker.TRAVERSAL_LIMIT,
                ),
            verificationList = verificationList,
        )
    } ?: OmSdkModel(
        isEnabled = config.optBoolean(OM_ENABLED, false),
        verificationEnabled = config.optBoolean(VERIFICATION_ENABLED, false),
        verificationList = verificationList,
    )
}

internal fun JSONObject.asVerificationModelList(): List<VerificationModel> {
    return optJSONArray("verification")
        ?.asListSkipNull<JSONObject>()
        ?.mapNotNull {
            try {
                VerificationModel(
                    it.getString("url"),
                    it.getString("vendor"),
                    it.getString("params"),
                )
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
}

data class VerificationModel(
    val url: String,
    val vendor: String,
    val params: String,
)
