package com.chartboost.sdk.internal.Libraries

import android.content.Context
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.utils.Base64Wrapper
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

private const val VERSION_TOKEN = "1.0"

internal class BidderTokenGenerator(
    private val context: Context,
    private val base64Wrapper: Base64Wrapper,
    private val identity: CBIdentity,
    private val sdkConfiguration: AtomicReference<SdkConfiguration>,
    private val openMeasurementManager: OpenMeasurementManager,
) {
    fun generateBidderToken(): String {
        val identityFields = identity.toIdentityBodyFields()
        val sdkConfig = sdkConfiguration.get()
        val json =
            JSONObject().apply {
                put("token_version", VERSION_TOKEN)
                put("appSetId", identityFields.setId ?: "")
                put("appSetIdScope", identityFields.setIdScope ?: 0)
                put("package", context.packageName)
                if (sdkConfig?.omSdkConfig?.isEnabled == true) {
                    openMeasurementManager.getOmidPartner()?.let { partner ->
                        put("omidpn", partner.name)
                        put("omidpv", partner.version)
                    }
                }
            }
        return base64Wrapper.encode(json.toString())
    }
}
