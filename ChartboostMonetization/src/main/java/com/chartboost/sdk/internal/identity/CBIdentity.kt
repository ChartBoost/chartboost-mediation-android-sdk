package com.chartboost.sdk.internal.identity

import android.content.Context
import com.chartboost.sdk.OpenForTesting
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.External.Android
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Model.IdentityBodyFields
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.google.android.gms.appset.AppSetIdInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private const val ZEROED_UUID = "000000000"
private const val GAID_KEY = "gaid"
private const val UUID_KEY = "uuid"
private const val APPSETID_KEY = "appsetid"

/**
 * Values representing the state of the Google Play Services / Amazon advertising ID.
 * Indicates if tracking is enabled, disabled, or an unknown status, which
 * probably indicates that the Play services are not available on the user's device.
 */
enum class TrackingState(val value: Int) {
    TRACKING_UNKNOWN(-1),
    TRACKING_ENABLED(0),
    TRACKING_LIMITED(1),
}

@OpenForTesting
internal class CBIdentity(
    private val context: Context,
    private val android: Android,
    private val ifa: IFA,
    private val base64Wrapper: Base64Wrapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Stores the set ID once we've retrieved it
     */
    private val setId by lazy { AtomicReference<String?>(null) }
    private val setIdScope by lazy { AtomicInteger() }
    private val identityBodyFields by lazy { AtomicReference<IdentityBodyFields?>(null) }

    // Avoids launching the same job again if it's already running
    @Volatile
    private var identityJob: Job? = null

    init {
        // Try to compute identity as soon as the object is created
        launchIdentityJob()
    }

    /**
     * Return identity as basic fields used in requests
     */
    fun toIdentityBodyFields(): IdentityBodyFields {
        identityJob ?: launchIdentityJob()
        return identityBodyFields.get() ?: getIdentity(context)
    }

    private fun launchIdentityJob() {
        // Catch OOM when trying to create a new thread
        // see https://chartboost.atlassian.net/browse/MO-6074 for more details
        try {
            identityJob =
                CoroutineScope(ioDispatcher).launch {
                    computeIdentity()
                    identityJob = null
                }
        } catch (t: Throwable) {
            Logger.e("Error launching identity job", t)
        }
    }

    private fun computeIdentity() {
        requestSetIdLocal()
        identityBodyFields.set(getIdentity(context))
    }

    /**
     * Retrieve AppSetId in a non-blocking way to avoid potential ANR
     */
    private fun requestSetIdLocal() {
        try {
            if (appSetExists()) {
                android.getAppSetIdTask(context)?.addOnSuccessListener { appSetInfo: AppSetIdInfo? ->
                    onAppSetIdNetworkCallSuccess(appSetInfo)
                }
            } else {
                Logger.e("AppSetId dependency not present")
            }
        } catch (e: Exception) {
            Logger.e("Error requesting AppSetId", e)
        }
    }

    private fun getIdentity(context: Context): IdentityBodyFields {
        return try {
            val idsHolder = ifa.getAdvertisingIdHolder()
            Logger.e("IFA: $idsHolder")
            val adsId = idsHolder.advertisingID
            val advertisingIDState = idsHolder.advertisingIDState
            val isTrackingLimited = advertisingIDState == TrackingState.TRACKING_LIMITED
            val uuid = ifa.getLocalAdvertisingId(context, isTrackingLimited)
            val uuidToSet = if (adsId != null) ZEROED_UUID else uuid

            if (SandboxBridgeSettings.isSandboxMode) {
                SandboxBridgeSettings.setAdId(adsId)
                SandboxBridgeSettings.setUUID(uuidToSet)
            }

            IdentityBodyFields(
                advertisingIDState,
                buildIdentityJsonBase64(adsId, uuidToSet),
                uuidToSet,
                adsId,
                setId.get(),
                setIdScope.get(),
            )
        } catch (e: Exception) {
            e.message?.let { Logger.e(it) }
            IdentityBodyFields()
        }
    }

    private fun buildIdentityJsonBase64(
        advertisingID: String?,
        uuid: String?,
    ): String {
        val obj = JSONObject()
        advertisingID?.let { CBJSON.put(obj, GAID_KEY, it) } ?: uuid?.let {
            CBJSON.put(
                obj,
                UUID_KEY,
                it,
            )
        }
        setId.get()?.let { CBJSON.put(obj, APPSETID_KEY, it) }
        return base64Wrapper.encode(obj.toString())
    }

    // Removed all the sync code cause ultimately it is handled inside the Task itself
    private fun onAppSetIdNetworkCallSuccess(appSetInfo: AppSetIdInfo?) {
        appSetInfo?.let {
            setId.set(appSetInfo.id)
            setIdScope.set(appSetInfo.scope)
        }
    }

    private fun appSetExists(): Boolean {
        return try {
            Class.forName("com.google.android.gms.appset.AppSet")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
