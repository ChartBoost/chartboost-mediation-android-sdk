package com.chartboost.sdk

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build.VERSION_CODES.P
import android.os.Looper
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.chartboost.sdk.events.StartError
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.privacy.model.*
import com.chartboost.sdk.privacy.model.CCPA.Companion.CCPA_STANDARD
import com.chartboost.sdk.privacy.model.COPPA.Companion.COPPA_STANDARD
import com.chartboost.sdk.privacy.model.GDPR.Companion.GDPR_STANDARD
import com.chartboost.sdk.privacy.model.LGPD.Companion.LGPD_STANDARD
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowNetworkCapabilities
import java.lang.Thread.sleep

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [P])
class ChartboostTest {
    // Instrumentation test with robolectric allow us to inject fake application and test new api
    private val app = getApplicationContext<Context>()

    @Test
    fun `startWithAppId should return error on first session if there's an error`() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        Chartboost.startWithAppId(app, "000000000000000000000000", "0000000000000000000000000000000000000000") {
            assertNotNull(it)
            assertFalse(Chartboost.isSdkStarted())
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun startWithAppIdTestConnectionInvalidCredentials() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)

        val connectivityManager = getApplicationContext<Context>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadowOf(connectivityManager).setNetworkCapabilities(connectivityManager.activeNetwork, networkCapabilities)

        Chartboost.startWithAppId(app, "000000000000000000000000", "0000000000000000000000000000000000000000") {
            assertNotNull(it)
            assertEquals(it?.code, StartError.Code.INTERNAL)
            assertFalse(Chartboost.isSdkStarted())
        }
        sleep(1000)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun startWithAppIdTestNoConnectionInvalidCredentials() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)

        Chartboost.startWithAppId(app, "000000000000000000000000", "0000000000000000000000000000000000000000") {
            assertNotNull(it)
            assertEquals(it?.code, StartError.Code.INTERNAL)
            assertFalse(Chartboost.isSdkStarted())
        }
        sleep(1000)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `returns stored GDPR consent`() {
        Chartboost.addDataUseConsent(app, GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL))

        val gdprConsent = Chartboost.getDataUseConsent(app, GDPR_STANDARD)

        assertNotNull(gdprConsent)
        assertEquals(GDPR_STANDARD, gdprConsent?.privacyStandard)
        assertEquals("1", gdprConsent?.consent)
    }

    @Test
    fun `returns stored LGPD consent`() {
        Chartboost.addDataUseConsent(app, LGPD(true))

        val lgpdConsent = Chartboost.getDataUseConsent(app, LGPD_STANDARD)

        assertNotNull(lgpdConsent)
        assertEquals(LGPD_STANDARD, lgpdConsent?.privacyStandard)
        assertEquals(true, lgpdConsent?.consent)
    }

    @Test
    fun `returns stored LGPD no consent`() {
        Chartboost.addDataUseConsent(app, LGPD(false))

        val lgpdConsent = Chartboost.getDataUseConsent(app, LGPD_STANDARD)

        assertNotNull(lgpdConsent)
        assertEquals(LGPD_STANDARD, lgpdConsent?.privacyStandard)
        assertEquals(false, lgpdConsent?.consent)
    }

    @Test
    fun `returns stored CCPA consent`() {
        Chartboost.addDataUseConsent(app, CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE))

        val ccpaConsent = Chartboost.getDataUseConsent(app, CCPA_STANDARD)

        assertNotNull(ccpaConsent)
        assertEquals(CCPA_STANDARD, ccpaConsent?.privacyStandard)
        assertEquals("1YN-", ccpaConsent?.consent)
    }

    @Test
    fun `returns stored COPPA consent`() {
        Chartboost.addDataUseConsent(app, COPPA(true))
        val coppaConsent = Chartboost.getDataUseConsent(app, COPPA_STANDARD)

        assertNotNull(coppaConsent)

        assertEquals(COPPA_STANDARD, coppaConsent?.privacyStandard)
        assertEquals(true, coppaConsent?.consent)
    }

    @Test
    fun `returns stored custom consent`() {
        Chartboost.addDataUseConsent(app, Custom("test", "test"))
        val customConsent = Chartboost.getDataUseConsent(app, "test")

        assertNotNull(customConsent)

        assertEquals("test", customConsent?.privacyStandard)
        assertEquals("test", customConsent?.consent)
    }

    @Test
    fun `does not store invalid consents`() {
        val invalidConsent = InvalidConsent("invalid", "invalid")

        Chartboost.addDataUseConsent(app, invalidConsent)

        assertNull(Chartboost.getDataUseConsent(app, "invalid"))
    }

    @Test
    fun `removes stored consent when clearDataUseConsent is called`() {
        Chartboost.addDataUseConsent(app, COPPA(true))
        val consentAdded = Chartboost.getDataUseConsent(app, COPPA_STANDARD)
        assertNotNull(consentAdded)
        assertEquals(COPPA_STANDARD, consentAdded!!.privacyStandard)
        assertEquals(true, consentAdded.consent)

        Chartboost.clearDataUseConsent(app, COPPA_STANDARD)

        assertNull(Chartboost.getDataUseConsent(app, COPPA_STANDARD))
    }

    @Test
    fun `sets the logging level to ALL`() {
        Chartboost.setLoggingLevel(LoggingLevel.ALL)
        assertEquals(LoggingLevel.ALL, com.chartboost.sdk.internal.logging.Logger.level)
    }

    @Test
    fun `sets the logging level to NONE`() {
        Chartboost.setLoggingLevel(LoggingLevel.NONE)
        assertEquals(LoggingLevel.NONE, com.chartboost.sdk.internal.logging.Logger.level)
    }

    @Test
    fun `get token null before init`() {
        val token = Chartboost.getBidderToken()
        assertNull(token)
    }

    @Ignore("HB-8129")
    @Test
    fun `get token after init`() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        // There is a problem when all the test suit is run all at once. After first successful
        // init in the test next test keeps the ChartboostDependencyContainer state
        ChartboostDependencyContainer.start("000000000000000000000000", "0000000000000000000000000000000000000000")
        Chartboost.startWithAppId(app, "000000000000000000000000", "0000000000000000000000000000000000000000") {}
        sleep(1000)
        ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSDKInitialized = true
        val token = Chartboost.getBidderToken()
        assertNotNull(token)
        val decoded = Base64Wrapper().decode(token!!)
        val json = JSONObject(decoded)
        assertEquals("", json.getString("appSetId"))
        assertEquals(0, json.getInt("appSetIdScope"))
        assertEquals("com.chartboost.sdk.test", json.getString("package"))
        assertEquals("1.0", json.getString("token_version"))
    }

    class InvalidConsent(override val privacyStandard: String, override val consent: Any) : DataUseConsent
}
