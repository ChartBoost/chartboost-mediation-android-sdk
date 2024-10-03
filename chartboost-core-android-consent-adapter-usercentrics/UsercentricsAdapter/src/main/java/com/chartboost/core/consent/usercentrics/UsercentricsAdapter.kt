/*
 * Copyright 2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.core.consent.usercentrics

import android.app.Activity
import android.content.Context
import com.chartboost.core.ChartboostCoreLogger
import com.chartboost.core.Utils
import com.chartboost.core.consent.ConsentAdapter
import com.chartboost.core.consent.ConsentAdapterListener
import com.chartboost.core.consent.ConsentDialogType
import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentKeys
import com.chartboost.core.consent.ConsentSource
import com.chartboost.core.consent.ConsentStatus
import com.chartboost.core.consent.ConsentValue
import com.chartboost.core.consent.ConsentValues
import com.chartboost.core.error.ChartboostCoreError
import com.chartboost.core.error.ChartboostCoreException
import com.chartboost.core.initialization.Module
import com.chartboost.core.initialization.ModuleConfiguration
import com.usercentrics.ccpa.CCPAData
import com.usercentrics.sdk.BannerSettings
import com.usercentrics.sdk.Usercentrics
import com.usercentrics.sdk.UsercentricsBanner
import com.usercentrics.sdk.UsercentricsEvent
import com.usercentrics.sdk.UsercentricsOptions
import com.usercentrics.sdk.UsercentricsReadyStatus
import com.usercentrics.sdk.UsercentricsServiceConsent
import com.usercentrics.sdk.models.common.UsercentricsLoggerLevel
import com.usercentrics.sdk.models.settings.UsercentricsConsentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UsercentricsAdapter() : ConsentAdapter, Module {

    companion object {
        const val CONSENT_MEDIATION: String = "consentMediation"
        const val CHARTBOOST_CORE_DPS_KEY: String = "coreDpsName"
        const val DEFAULT_CHARTBOOST_CORE_DPS = "ChartboostCore"
        const val DEFAULT_LANGUAGE_KEY: String = "defaultLanguage"
        const val LOGGER_LEVEL_KEY: String = "loggerLevel"
        const val OPTIONS_KEY: String = "options"
        const val PARTNER_ID_MAP_KEY: String = "partnerIdMap"
        const val RULE_SET_ID_KEY: String = "ruleSetId"
        const val SETTINGS_ID_KEY: String = "settingsId"
        const val TIMEOUT_MILLIS_KEY: String = "timeoutMillis"
        const val VERSION_KEY: String = "version"
        const val moduleId = "usercentrics"
        const val moduleVersion = BuildConfig.CHARTBOOST_CORE_USERCENTRICS_ADAPTER_VERSION

        /**
         * The default Usercentrics template ID to Chartboost partner ID map.
         */
        private val DEFAULT_TEMPLATE_ID_TO_PARTNER_ID =
            mapOf(
                "J64M6DKwx" to "adcolony",
                "r7rvuoyDz" to "admob",
                "IUyljv4X5" to "amazon_aps",
                "fHczTMzX8" to "applovin",
                "IEbRp3saT" to "chartboost",
                "H17alcVo_iZ7" to "fyber",
                "S1_9Vsuj-Q" to "google_googlebidding",
                "ROCBK21nx" to "hyprmx",
                "ykdq8J5a9MExGT" to "inmobi",
                "9dchbL797" to "ironsource",
                "VPSyZyTbYPSHpF" to "mobilefuse",
                "ax0Nljnj2szF_r" to "facebook",
                "E6AgqirYV" to "mintegral",
                "HWSNU_Ll1" to "pangle",
                "B1DLe54jui-X" to "tapjoy",
                "hpb62D82I" to "unity",
                "5bv4OvSwoXKh-G" to "verve",
                "jk3jF2tpw" to "vungle",
                "EMD3qUMa8" to "vungle",
            )

        /**
         * Use this to change the look and feel of the Usercentrics consent dialogs.
         * See https://docs.usercentrics.com/cmp_in_app_sdk/latest/features/customization/ for more
         * information.
         */
        @JvmStatic
        var bannerSettings: BannerSettings? = null
    }

    constructor(
        options: UsercentricsOptions,
        templateIdToPartnerIdMap: Map<String, String> = mapOf(),
    ) : this() {
        this@UsercentricsAdapter.options = options
        this@UsercentricsAdapter.templateIdToPartnerIdMap.putAll(templateIdToPartnerIdMap)
    }

    override fun updateCredentials(context: Context, credentials: JSONObject) {
        chartboostCoreDpsName =
            credentials.optString(CHARTBOOST_CORE_DPS_KEY, DEFAULT_CHARTBOOST_CORE_DPS)
        val optionsJson = credentials.optJSONObject(OPTIONS_KEY) ?: JSONObject()
        val settingsId = optionsJson.optString(SETTINGS_ID_KEY, "")
        val defaultLanguage = optionsJson.optString(DEFAULT_LANGUAGE_KEY, "en")
        val version = optionsJson.optString(VERSION_KEY, "latest")
        val timeoutMillis = optionsJson.optLong(TIMEOUT_MILLIS_KEY, 5000)
        val loggerLevel =
            stringToUsercentricsLoggerLevel(optionsJson.optString(LOGGER_LEVEL_KEY, "debug"))
        val ruleSetId = optionsJson.optString(RULE_SET_ID_KEY, "")
        val consentMediation = optionsJson.optBoolean(CONSENT_MEDIATION, false)
        val partnerIdMapJsonObject = credentials.optJSONObject(PARTNER_ID_MAP_KEY)
        partnerIdMapJsonObject?.keys()?.forEach {
            templateIdToPartnerIdMap[it] = partnerIdMapJsonObject.optString(it)
        }

        options = UsercentricsOptions(
            settingsId = settingsId,
            defaultLanguage = defaultLanguage,
            version = version,
            timeoutMillis = timeoutMillis,
            loggerLevel = loggerLevel,
            ruleSetId = ruleSetId,
            consentMediation = consentMediation,
        )
    }

    override val moduleId: String = Companion.moduleId

    override val moduleVersion: String = Companion.moduleVersion

    override var shouldCollectConsent: Boolean = true
        private set

    override val consents: Map<ConsentKey, ConsentValue>
        get() = mutableConsents

    private val mutableConsents = mutableMapOf<ConsentKey, ConsentValue>()

    /**
     * This map is just to keep track of partner consents so we don't have to go from consent
     * status and String. All of these should also live in consents.
     */
    private val partnerConsentStatus: MutableMap<String, ConsentStatus> = mutableMapOf()

    override val sharedPreferencesIabStrings: MutableMap<String, String> = mutableMapOf()
    override val sharedPreferenceChangeListener: ConsentAdapter.IabSharedPreferencesListener =
        ConsentAdapter.IabSharedPreferencesListener(sharedPreferencesIabStrings)

    override var listener: ConsentAdapterListener? = null
        set(value) {
            field = value
            sharedPreferenceChangeListener.listener = value
        }

    /*
     * The name of the Usercentrics Data Processing Service (DPS) defined in the Usercentrics
     * dashboard for the Chartboost Core SDK.
     */
    var chartboostCoreDpsName: String = DEFAULT_CHARTBOOST_CORE_DPS

    /**
     * Options to initialize Usercentrics.
     */
    var options: UsercentricsOptions? = null

    private val templateIdToPartnerIdMap: MutableMap<String, String> = mutableMapOf<String, String>().apply {
        putAll(DEFAULT_TEMPLATE_ID_TO_PARTNER_ID)
    }

    override suspend fun showConsentDialog(
        activity: Activity, dialogType: ConsentDialogType
    ): Result<Unit> {
        return executeWhenUsercentricsInitialized(activity.applicationContext) {
            val banner = UsercentricsBanner(activity, bannerSettings)
            when (dialogType) {
                ConsentDialogType.CONCISE -> {
                    banner.showFirstLayer { userResponse ->
                        ChartboostCoreLogger.d("1st layer response: $userResponse")
                    }
                    Result.success(Unit)
                }

                ConsentDialogType.DETAILED -> {
                    banner.showSecondLayer { userResponse ->
                        ChartboostCoreLogger.d("2nd layer response: $userResponse")
                    }
                    Result.success(Unit)
                }

                else -> {
                    ChartboostCoreLogger.d("Unexpected consent dialog type: $dialogType")
                    Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.DialogShowError))
                }
            }
        }
    }

    override suspend fun grantConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        return executeWhenUsercentricsInitialized(context) {
            Usercentrics.instance.acceptAll(
                consentStatusSourceToUsercentricsConsentType(
                    statusSource
                )
            )
            fetchConsentInfo(
                context, NotificationType.DIFFERENT_FROM_CURRENT_VALUE, consents, partnerConsentStatus,
            )
        }
    }

    override suspend fun denyConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        return executeWhenUsercentricsInitialized(context) {
            Usercentrics.instance.denyAll(
                consentStatusSourceToUsercentricsConsentType(
                    statusSource
                )
            )
            fetchConsentInfo(
                context, NotificationType.DIFFERENT_FROM_CURRENT_VALUE, consents, partnerConsentStatus,
            )
        }
    }

    override suspend fun resetConsent(context: Context): Result<Unit> {
        val options = options ?: UsercentricsOptions()
        val oldConsents = mutableConsents.toMap()
        mutableConsents.clear()
        resetUsercentrics()
        initializeUsercentrics(context, options)
        return fetchConsentInfo(
            context, NotificationType.DIFFERENT_FROM_CACHED_VALUE, oldConsents, partnerConsentStatus,
        )
    }

    private suspend fun resetUsercentrics() {
        return suspendCoroutine {
            fun resumeOnce(result: Result<Unit>) {
                it.resumeWith(result)
            }
            Usercentrics.isReady({
                Usercentrics.instance.clearUserSession({
                    resumeOnce(
                        Result.success(Unit)
                    )
                },
                    {
                        ChartboostCoreLogger.w("Unable to clear user session. Unknown reason why.")
                        resumeOnce(Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.Unknown)))
                    })
            }, {
                ChartboostCoreLogger.w("Failed to get Usercentrics instance when clearing consent.")
                resumeOnce(Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.InitializationError)))
            })
        }
    }

    private fun consentStatusSourceToUsercentricsConsentType(statusSource: ConsentSource): UsercentricsConsentType {
        return when(statusSource) {
            ConsentSource.USER -> UsercentricsConsentType.EXPLICIT
            ConsentSource.DEVELOPER -> UsercentricsConsentType.IMPLICIT
            else -> UsercentricsConsentType.IMPLICIT
        }
    }

    override suspend fun initialize(context: Context, moduleConfiguration: ModuleConfiguration): Result<Unit> {
        val options = options
            ?: return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.InitializationError))
        initializeUsercentrics(context, options)

        // This waits for Usercentrics.isReady()
        return fetchConsentInfo(context, NotificationType.NEVER, consents, partnerConsentStatus)
    }

    private fun initializeUsercentrics(context: Context, options: UsercentricsOptions) {
        Usercentrics.initialize(context.applicationContext, options)

        Usercentrics.isReady({
            UsercentricsEvent.onConsentUpdated {
                CoroutineScope(Main).launch {
                    fetchConsentInfo(
                        context,
                        NotificationType.DIFFERENT_FROM_CURRENT_VALUE,
                        consents,
                        partnerConsentStatus,
                    )
                }
            }
            startObservingSharedPreferencesIabStrings(context)
            mutableConsents.putAll(sharedPreferencesIabStrings)
        }, {
            ChartboostCoreLogger.d("Unable to attach onConsentUpdated listener")
        })
    }

    private suspend fun executeWhenUsercentricsInitialized(
        context: Context,
        dispatcher: CoroutineDispatcher = Main,
        block: suspend (usercentricsReadyStatus: UsercentricsReadyStatus) -> Result<Unit>
    ): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            fun resumeOnce(result: Result<Unit>) {
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            Usercentrics.isReady({ readyStatus ->
                CoroutineScope(dispatcher).launch(CoroutineExceptionHandler { _, exception ->
                    ChartboostCoreLogger.w("$exception when executing usercentrics action.")
                    resumeOnce(Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.Unknown)))
                }) {
                    resumeOnce(block(readyStatus))
                }
            }, {
                options?.let { options ->
                    try {
                        initializeUsercentrics(context, options)
                    } catch (e: AssertionError) {
                        ChartboostCoreLogger.e("Likely using both a ruleSetID and a templateID which is not allowed. $e")
                        resumeOnce(
                            Result.failure(
                                ChartboostCoreException(
                                    ChartboostCoreError.ConsentError.InitializationError
                                )
                            )
                        )
                        return@let
                    }
                    Usercentrics.isReady({ readyStatus ->
                        CoroutineScope(dispatcher).launch {
                            resumeOnce(block(readyStatus))
                        }
                    }, { error ->
                        ChartboostCoreLogger.d("$error when retrying initialization. Clearing consents")
                        resetConsentsAndNotify()
                        resumeOnce(
                            Result.failure(
                                ChartboostCoreException(
                                    ChartboostCoreError.ConsentError.InitializationError
                                )
                            )
                        )
                    })
                } ?: run {
                    if (continuation.isActive) {
                        continuation.resume(
                            Result.failure(
                                ChartboostCoreException(
                                    ChartboostCoreError.ConsentError.InitializationError
                                )
                            )
                        )
                    }
                }
            })
        }
    }

    private suspend fun fetchConsentInfo(
        context: Context,
        notify: NotificationType,
        oldConsents: Map<ConsentKey, ConsentValue?>,
        oldPartnerConsents: Map<String, ConsentStatus>,
    ): Result<Unit> {
        return executeWhenUsercentricsInitialized(context) { usercentricsReadyStatus ->
            updateConsents(
                usercentricsReadyStatus, notify, oldConsents, oldPartnerConsents
            )
            Result.success(Unit)
        }
    }

    private suspend fun updateConsents(
        usercentricsReadyStatus: UsercentricsReadyStatus,
        notify: NotificationType,
        oldConsents: Map<ConsentKey, ConsentValue?>,
        oldPartnerConsents: Map<String, ConsentStatus>,
    ) {
        shouldCollectConsent = usercentricsReadyStatus.shouldCollectConsent

        updateTcf(notify, oldConsents)

        val uspData = Usercentrics.instance.getUSPData()
        updateCcpaOptIn(uspData, notify, oldConsents)
        updateUsp(uspData.uspString, notify, oldConsents)

        updatePartnerConsents(usercentricsReadyStatus.consents, notify, oldPartnerConsents)
    }

    private fun updateUsp(
        newUspString: String,
        notify: NotificationType,
        cachedConsents: Map<ConsentKey, ConsentValue?>
    ) {
        val nullableNewUspString = newUspString.ifEmpty { null }
        ChartboostCoreLogger.d("Setting USP to $nullableNewUspString")
        val previousUsp = consents[ConsentKeys.USP]?.ifEmpty { null }
        if (nullableNewUspString.isNullOrEmpty()) {
            mutableConsents.remove(ConsentKeys.USP)
        } else {
            mutableConsents[ConsentKeys.USP] = newUspString
        }
        when (notify) {
            NotificationType.DIFFERENT_FROM_CURRENT_VALUE -> if (previousUsp != nullableNewUspString) Utils.safeExecute {
                listener?.onConsentChange(
                    ConsentKeys.USP,
                )
            }

            NotificationType.DIFFERENT_FROM_CACHED_VALUE -> if (cachedConsents[ConsentKeys.USP] != nullableNewUspString) Utils.safeExecute {
                listener?.onConsentChange(
                    ConsentKeys.USP,
                )
            }

            else -> Unit
        }
    }

    private fun updateCcpaOptIn(
        ccpaData: CCPAData,
        notify: NotificationType,
        cachedConsents: Map<ConsentKey, ConsentValue?>
    ) {
        val newCcpaOptIn = when (ccpaData.optedOut) {
            true -> ConsentValues.DENIED
            false -> ConsentValues.GRANTED
            else -> null
        }
        val previousCcpaOptIn = consents[ConsentKeys.CCPA_OPT_IN]
        newCcpaOptIn?.let {
            mutableConsents[ConsentKeys.CCPA_OPT_IN] = it
        } ?: mutableConsents.remove(ConsentKeys.CCPA_OPT_IN)
        ChartboostCoreLogger.d("Setting CCPA opt in to $newCcpaOptIn")
        when (notify) {
            NotificationType.DIFFERENT_FROM_CURRENT_VALUE -> if (previousCcpaOptIn != newCcpaOptIn) Utils.safeExecute {
                listener?.onConsentChange(
                    ConsentKeys.CCPA_OPT_IN,
                )
            }

            NotificationType.DIFFERENT_FROM_CACHED_VALUE -> if (cachedConsents[ConsentKeys.CCPA_OPT_IN] != newCcpaOptIn) Utils.safeExecute {
                listener?.onConsentChange(
                    ConsentKeys.CCPA_OPT_IN,
                )
            }

            else -> Unit
        }
    }

    private suspend fun updateTcf(
        notify: NotificationType,
        cachedConsents: Map<ConsentKey, ConsentValue?>,
    ) {
        return suspendCancellableCoroutine { continuation ->
            Usercentrics.instance.getTCFData {
                val previousTcfString = consents[ConsentKeys.TCF]
                val newTcfString = it.tcString.ifEmpty { null }

                ChartboostCoreLogger.d("Setting TCF to $newTcfString")
                if (newTcfString.isNullOrEmpty()) {
                    mutableConsents.remove(ConsentKeys.TCF)
                } else {
                    mutableConsents[ConsentKeys.TCF] = newTcfString
                }

                when (notify) {
                    NotificationType.DIFFERENT_FROM_CURRENT_VALUE -> if (newTcfString != previousTcfString) Utils.safeExecute {
                        listener?.onConsentChange(
                            ConsentKeys.TCF,
                        )
                    }

                    NotificationType.DIFFERENT_FROM_CACHED_VALUE -> if (cachedConsents[ConsentKeys.TCF] != newTcfString) Utils.safeExecute {
                        listener?.onConsentChange(
                            ConsentKeys.TCF,
                        )
                    }

                    else -> Unit
                }

                continuation.resume(Unit)
            }
        }
    }

    private fun updatePartnerConsents(
        consents: List<UsercentricsServiceConsent>,
        notify: NotificationType,
        oldPartnerConsents: Map<String, ConsentStatus>,
    ) {
        consents.forEach { consent ->
            val partnerId = templateIdToPartnerIdMap[consent.templateId] ?: consent.templateId
            val previousConsentStatus = partnerConsentStatus[partnerId] ?: ConsentStatus.UNKNOWN
            val newConsentStatus = toConsentStatus(consent.status)
            partnerConsentStatus[partnerId] = newConsentStatus
            mutableConsents[partnerId] = newConsentStatus.toString()
            when (notify) {
                NotificationType.DIFFERENT_FROM_CURRENT_VALUE -> if (previousConsentStatus != newConsentStatus) Utils.safeExecute {
                    listener?.onConsentChange(partnerId)
                }

                NotificationType.DIFFERENT_FROM_CACHED_VALUE -> if ((oldPartnerConsents[partnerId] ?: ConsentStatus.UNKNOWN) != newConsentStatus) Utils.safeExecute {
                    listener?.onConsentChange(partnerId)
                }

                else -> Unit
            }
        }
    }

    private fun toConsentStatus(status: Boolean): ConsentStatus =
        if (status) ConsentStatus.GRANTED else ConsentStatus.DENIED

    private fun stringToUsercentricsLoggerLevel(loggerLevel: String?): UsercentricsLoggerLevel {
        return try {
            UsercentricsLoggerLevel.valueOf(loggerLevel?.uppercase() ?: "")
        } catch (e: IllegalArgumentException) {
            UsercentricsLoggerLevel.DEBUG
        }
    }

    private fun resetConsentsAndNotify() {
        consents.forEach {
            Utils.safeExecute {
                listener?.onConsentChange(it.key)
            }
        }
        mutableConsents.clear()
        partnerConsentStatus.clear()
    }

    /**
     * When to fire a notification.
     */
    private enum class NotificationType {
        /**
         * Fires the notification if the new and current are different.
         */
        DIFFERENT_FROM_CURRENT_VALUE,

        /**
         * Only fire the notification if the value has changed from the previous saved value and
         * is also different from the current value.
         */
        DIFFERENT_FROM_CACHED_VALUE,

        /**
         * Do not fire the listener.
         */
        NEVER,
    }
}
