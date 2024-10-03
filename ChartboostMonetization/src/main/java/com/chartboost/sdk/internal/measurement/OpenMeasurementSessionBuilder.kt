package com.chartboost.sdk.internal.measurement

import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.Model.VerificationModel
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.logging.Logger
import com.iab.omid.library.chartboost.adsession.AdEvents
import com.iab.omid.library.chartboost.adsession.AdSession
import com.iab.omid.library.chartboost.adsession.AdSessionConfiguration
import com.iab.omid.library.chartboost.adsession.AdSessionContext
import com.iab.omid.library.chartboost.adsession.CreativeType
import com.iab.omid.library.chartboost.adsession.ImpressionType
import com.iab.omid.library.chartboost.adsession.Owner
import com.iab.omid.library.chartboost.adsession.Partner
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import com.iab.omid.library.chartboost.adsession.media.MediaEvents
import java.net.URL

class OpenMeasurementSessionBuilder {
    /**
     * createOmSession - prepare and build om session with proper ad attributes
     */
    fun createOmSession(
        webView: CBWebView,
        mtype: MediaTypeOM,
        omidPartner: Partner?,
        omidJsServiceContent: String?,
        verificationScriptResourcesList: List<VerificationScriptResource>,
        isValidationEnabled: Boolean,
        verificationListConfig: List<VerificationModel>,
    ): OMSessionHolder? {
        try {
            AdSession.createAdSession(
                buildAdSessionVideoConfig(mtype),
                getOmSessionContext(
                    omidPartner,
                    omidJsServiceContent,
                    verificationScriptResourcesList,
                    isValidationEnabled,
                    verificationListConfig,
                    mtype,
                    webView,
                ),
            ).apply {
                registerAdView(webView)
            }.also {
                return OMSessionHolder(
                    it,
                    AdEvents.createAdEvents(it),
                    createMediaEvents(mtype, it),
                )
            }
        } catch (e: Exception) {
            Logger.e("OMSDK create session exception", e)
        }
        return null
    }

    private fun createMediaEvents(
        mtype: MediaTypeOM,
        adSession: AdSession,
    ): MediaEvents? {
        return if (mtype == MediaTypeOM.HTML) {
            null
        } else {
            MediaEvents.createMediaEvents(adSession)
        }
    }

    private fun getOmSessionContext(
        omidPartner: Partner?,
        omidJsServiceContent: String?,
        verificationScriptResourcesList: List<VerificationScriptResource>,
        isValidationEnabled: Boolean,
        verificationListConfig: List<VerificationModel>,
        mtype: MediaTypeOM,
        webview: CBWebView,
    ): AdSessionContext? {
        return if (mtype == MediaTypeOM.HTML) {
            buildHtmlContext(omidPartner, webview)
        } else {
            buildNativeContext(
                omidPartner,
                omidJsServiceContent,
                verificationScriptResourcesList,
                isValidationEnabled,
                verificationListConfig,
            )
        }
    }

    private fun buildNativeContext(
        omidPartner: Partner?,
        omidJsServiceContent: String?,
        verificationScriptResourcesList: List<VerificationScriptResource>,
        isValidationEnabled: Boolean,
        verificationListConfig: List<VerificationModel>,
    ): AdSessionContext? {
        val customReferenceData = null
        val contentUrl = null
        return try {
            AdSessionContext.createNativeAdSessionContext(
                omidPartner,
                omidJsServiceContent,
                addValidationVerificationResource(
                    verificationScriptResourcesList,
                    verificationListConfig,
                    isValidationEnabled,
                ),
                contentUrl,
                customReferenceData,
            )
        } catch (e: IllegalArgumentException) {
            Logger.d("buildNativeContext error", e)
            null
        }
    }

    private fun buildHtmlContext(
        omidPartner: Partner?,
        webView: CBWebView?,
    ): AdSessionContext? {
        val customReferenceData = null
        val contentUrl = null
        return try {
            AdSessionContext.createHtmlAdSessionContext(
                omidPartner,
                webView,
                contentUrl,
                customReferenceData,
            )
        } catch (e: IllegalArgumentException) {
            Logger.d("buildHtmlContext error", e)
            null
        }
    }

    private fun addValidationVerificationResource(
        resources: List<VerificationScriptResource>,
        verificationListConfig: List<VerificationModel>,
        isValidationEnabled: Boolean,
    ): List<VerificationScriptResource> {
        val verificationScriptResources = mutableListOf<VerificationScriptResource>()
        if (isValidationEnabled) {
            verificationScriptResources.addAll(
                convertVerificationListFromConfigToOmResources(verificationListConfig),
            )
        }
        verificationScriptResources.addAll(resources)
        return verificationScriptResources
    }

    private fun convertVerificationListFromConfigToOmResources(
        verificationListConfig: List<VerificationModel>,
    ): List<VerificationScriptResource> {
        return try {
            verificationListConfig.map { verification ->
                stringUrlToURL(verification.url).let { url ->
                    VerificationScriptResource.createVerificationScriptResourceWithParameters(
                        verification.vendor,
                        url,
                        verification.params,
                    )
                }
            }
        } catch (e: Exception) {
            Logger.d("buildVerificationResources error", e)
            emptyList()
        }
    }

    private fun stringUrlToURL(url: String): URL? =
        try {
            URL(url)
        } catch (e: Exception) {
            Logger.d("buildVerificationResources invalid url", e)
            null
        }

    private fun buildAdSessionVideoConfig(mtype: MediaTypeOM): AdSessionConfiguration? {
        return try {
            AdSessionConfiguration.createAdSessionConfiguration(
                mtypeToCreativeType(mtype),
                ImpressionType.BEGIN_TO_RENDER,
                Owner.NATIVE,
                mtypeToOwner(mtype),
                false,
            )
        } catch (e: IllegalArgumentException) {
            Logger.d("buildAdSessionVideoConfig error", e)
            null
        }
    }

    private fun mtypeToCreativeType(mtype: MediaTypeOM): CreativeType {
        return when (mtype) {
            MediaTypeOM.UNKNOWN -> CreativeType.NATIVE_DISPLAY
            MediaTypeOM.HTML -> CreativeType.HTML_DISPLAY
            MediaTypeOM.VIDEO -> CreativeType.VIDEO
            MediaTypeOM.AUDIO -> CreativeType.AUDIO
            MediaTypeOM.NATIVE -> CreativeType.NATIVE_DISPLAY
        }
    }

    private fun mtypeToOwner(mtype: MediaTypeOM): Owner {
        return when (mtype) {
            MediaTypeOM.UNKNOWN -> Owner.NATIVE
            MediaTypeOM.HTML -> Owner.NONE
            MediaTypeOM.VIDEO -> Owner.NATIVE
            MediaTypeOM.AUDIO -> Owner.NATIVE
            MediaTypeOM.NATIVE -> Owner.NATIVE
        }
    }

    data class OMSessionHolder(
        var omSession: AdSession?,
        var omAdEvents: AdEvents?,
        var mediaEvents: MediaEvents?,
    )
}
