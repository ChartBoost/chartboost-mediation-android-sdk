package com.chartboost.sdk.internal.measurement

import android.Manifest
import android.content.Context
import android.os.Build
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.Model.VerificationModel
import com.chartboost.sdk.internal.WebView.CBWebView
import com.iab.omid.library.chartboost.Omid
import com.iab.omid.library.chartboost.adsession.*
import io.mockk.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowViewConfiguration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], shadows = [ShadowViewConfiguration::class])
class OpenMeasurementSessionBuilderTest {
    private val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
    private val sessionBuilder = OpenMeasurementSessionBuilder()

    @Before
    fun setup() {
        val shadowApplication = Shadow.extract<ShadowApplication>(app)
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        Chartboost.startWithAppId(app, "test", "test") {}
        Thread.sleep(500)
    }

    @Ignore("HB-8129")
    @Test
    fun `create om session default`() {
        val webview = CBWebView(app.applicationContext)
        val mtype = MediaTypeOM.VIDEO
        val partner = mockk<Partner>()
        val omSdk = "omsdk string"
        val verificationList = emptyList<VerificationScriptResource>()
        val validationEnabled = true
        val verificationListConfig = emptyList<VerificationModel>()

        Omid.activate(app.applicationContext)
        val sessionHolder =
            sessionBuilder.createOmSession(
                webview,
                mtype,
                partner,
                omSdk,
                verificationList,
                validationEnabled,
                verificationListConfig,
            )
        assertNotNull(sessionHolder)
        assertNotNull(sessionHolder?.omSession)
        assertNotNull(sessionHolder?.omAdEvents)
    }

    @Ignore("HB-8129")
    @Test
    fun `create om session with verification list from config`() {
        val webview = CBWebView(app.applicationContext)
        val mtype = MediaTypeOM.VIDEO
        val partner = mockk<Partner>()
        val omSdk = "omsdk string"
        val verificationList = emptyList<VerificationScriptResource>()
        val validationEnabled = true
        val verificationListConfig =
            listOf<VerificationModel>(
                VerificationModel(
                    "https://test.com/test",
                    "testVendor",
                    "testParams",
                ),
            )

        Omid.activate(app.applicationContext)
        val sessionHolder =
            sessionBuilder.createOmSession(
                webview,
                mtype,
                partner,
                omSdk,
                verificationList,
                validationEnabled,
                verificationListConfig,
            )
        assertNotNull(sessionHolder)
        assertNotNull(sessionHolder?.omSession)
        assertNotNull(sessionHolder?.omAdEvents)
    }
}
