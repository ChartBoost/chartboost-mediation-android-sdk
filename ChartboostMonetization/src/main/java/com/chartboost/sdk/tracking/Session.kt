package com.chartboost.sdk.tracking

import android.content.SharedPreferences
import com.chartboost.sdk.internal.Libraries.CBCrypto.getSha1Hex
import com.chartboost.sdk.internal.Model.SessionBodyFields
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.adType.toAdType
import java.util.UUID

private const val SESSION_COUNTER_KEY = "session_key"
const val ERROR_SESSION_LOAD = -1

/**
 * Class holds user sessions counter which will last until application is uninstalled.
 * Session is counter as new sdk initialisation which means counter will be incremented
 * every time that application is created until it is destroyed.
 */
class Session(private val mPrefs: SharedPreferences) {
    var sessionId: String? = null
        private set
    private var mSessionStartTime: Long = 0
    var sessionCounter = 0
        private set
    private var mSessionInterstitialImpressionCounter = 0
    private var mSessionRewardedImpressionCounter = 0
    private var mSessionBannerImpressionCounter = 0

    init {
        sessionCounter = loadSession()
    }

    private val sessionDuration: Long
        get() = System.currentTimeMillis() - mSessionStartTime

    fun getSessionImpressionsCounter(type: AdType?): Int {
        return when (type) {
            AdType.Interstitial -> mSessionInterstitialImpressionCounter
            AdType.Rewarded -> mSessionRewardedImpressionCounter
            AdType.Banner -> mSessionBannerImpressionCounter
            else -> 0
        }
    }

    fun getSessionImpressionsCounter(type: String): Int {
        val adType = type.toAdType()
        return getSessionImpressionsCounter(adType)
    }

    fun addSession() {
        sessionId = generateSessionId()
        mSessionStartTime = System.currentTimeMillis()
        mSessionInterstitialImpressionCounter = 0
        mSessionRewardedImpressionCounter = 0
        mSessionBannerImpressionCounter = 0
        sessionCounter++
        saveSession()
    }

    fun addSessionImpression(type: AdType) {
        when (type) {
            AdType.Interstitial -> mSessionInterstitialImpressionCounter++
            AdType.Rewarded -> mSessionRewardedImpressionCounter++
            AdType.Banner -> mSessionBannerImpressionCounter++
        }
    }

    fun toSessionBodyFields(): SessionBodyFields {
        return SessionBodyFields(
            sessionId,
            sessionDuration,
            sessionCounter,
            getSessionImpressionsCounter(AdType.Banner),
            getSessionImpressionsCounter(AdType.Rewarded),
            getSessionImpressionsCounter(AdType.Interstitial),
        )
    }

    private fun generateSessionId(): String = getSha1Hex(UUID.randomUUID().toString())

    /**
     * In the server side we should not see any sessions values = 0,
     * first valid sessions starts as 1
     *
     * @return
     */
    private fun loadSession(): Int {
        return mPrefs.getInt(SESSION_COUNTER_KEY, 0)
    }

    private fun saveSession() {
        mPrefs.edit()?.putInt(SESSION_COUNTER_KEY, sessionCounter)?.apply()
    }
}
