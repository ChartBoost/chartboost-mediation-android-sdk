package com.chartboost.sdk.internal.measurement

import android.content.Context
import com.chartboost.sdk.BuildConfig
import com.chartboost.sdk.R
import com.chartboost.sdk.internal.Model.OmSdkModel
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Model.VerificationModel
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.ResourceLoader
import com.chartboost.sdk.internal.utils.SharedPrefsHelper
import com.iab.omid.library.chartboost.Omid
import com.iab.omid.library.chartboost.ScriptInjector
import com.iab.omid.library.chartboost.adsession.Partner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private const val OMID_JS = "com.chartboost.sdk.omidjs"

class OpenMeasurementManager(
    private val context: Context,
    private val sharedPrefsHelper: SharedPrefsHelper,
    private val resourcesLoader: ResourceLoader,
    private val sdkConfig: AtomicReference<SdkConfiguration>,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    /**
     * initializeWithContext : activates the OMID API.
     */
    fun initialize() {
        if (!isOmSdkEnabled()) {
            Logger.d("OMSDK initialize is disabled by the cb config!")
            return
        }

        if (isOmSdkActive()) {
            Logger.d("OMSDK initialize is already active!")
            return
        }

        try {
            CoroutineScope(mainDispatcher).launch {
                try {
                    Omid.activate(context)
                    Logger.d("OMSDK is initialized successfully!")
                } catch (ex: Exception) {
                    Logger.e("OMSDK initialization exception", ex)
                }
            }
        } catch (e: Exception) {
            Logger.e("Error launching om activate job", e)
        }
    }

    fun isOmSdkActive(): Boolean {
        return try {
            Omid.isActive()
        } catch (e: Exception) {
            Logger.d("OMSDK error when checking isActive", e)
            false
        }
    }

    fun isOmSdkEnabled(): Boolean {
        return sdkConfig.get()?.omSdkConfig?.isEnabled ?: false
    }

    fun isVerificationEnabled(): Boolean {
        return sdkConfig.get()?.omSdkConfig?.verificationEnabled ?: false
    }

    fun getVerificationListFromConfig(): List<VerificationModel> {
        return sdkConfig.get()?.omSdkConfig?.verificationList ?: emptyList()
    }

    fun getOmVisibilityTrackerConfig(): OmSdkModel {
        return sdkConfig.get()?.omSdkConfig ?: OmSdkModel()
    }

    /**
     * injectOmidJsIntoHtml - injects the Omid JS to the ad html
     * @param html - ad response html
     * @return - the html string included Omid JS
     */
    fun injectOmidJsIntoHtml(html: String): String {
        if (!isOmSdkEnabled()) {
            Logger.e("OMSDK injectOmidJsIntoHtml is disabled by the cb config!")
            return html
        }

        if (!Omid.isActive()) return html
        return try {
            ScriptInjector.injectScriptContentIntoHtml(getOmSdkJsLib(), html)
        } catch (ex: Exception) {
            Logger.e("OmidJS injection exception", ex)
            html
        }
    }

    fun getOmSdkJsLib(): String? {
        return getOmidJs(R.raw.omsdk_v1, OMID_JS)
    }

    /**
     * setOmidPartner - sets the Omid Partner with partner name and sdk version
     * @return - the Omid Partner as a Partner
     */
    fun getOmidPartner(): Partner? {
        return try {
            Partner.createPartner(omidPartnerName(), BuildConfig.SDK_VERSION)
        } catch (ex: Exception) {
            Logger.e("Omid Partner exception", ex)
            null
        }
    }

    /**
     * getOmidJs - gets the Omid JS resource as a string and cache it
     * @return - the Omid JS resource as a string
     */
    private fun getOmidJs(
        resourceId: Int,
        sharedPrefsKey: String,
    ): String? {
        return try {
            sharedPrefsHelper.loadFromSharedPrefs(sharedPrefsKey) ?: readAndSaveResourceFile(
                sharedPrefsKey,
                resourceId,
            )
        } catch (ex: Exception) {
            Logger.e("OmidJS exception", ex)
            null
        }
    }

    private fun readAndSaveResourceFile(
        sharedPrefsKey: String,
        resourceId: Int,
    ): String? {
        return try {
            resourcesLoader.readRawResourceFile(resourceId)?.let {
                sharedPrefsHelper.saveIntoSharedPrefs(sharedPrefsKey, it)
                it
            }
        } catch (ex: Exception) {
            Logger.e("OmidJS resource file exception", ex)
            null
        }
    }

    private fun omidPartnerName() = "Chartboost"
}
