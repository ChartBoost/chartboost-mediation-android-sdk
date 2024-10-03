package com.chartboost.sdk.view

import android.Manifest
import android.content.Intent
import android.os.Build
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.internal.Libraries.CBConstants
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment.getApplication
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CBImpressionActivityTest {
    private var TEST_APP_ID = "4f7b433509b6025804000002"
    private var TEST_SIGNATURE_ID = "dd2d41b69ac01b80f443f5b6cf06096d457f82bd"
    private val application = getApplication()

    @Before
    fun setup() {
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.grantPermissions(Manifest.permission.INTERNET)
        shadowApp.grantPermissions(Manifest.permission.ACCESS_NETWORK_STATE)
        Chartboost.startWithAppId(application, TEST_APP_ID, TEST_SIGNATURE_ID) {}
    }

    @Test
    fun `create activity`() {
        val i = Intent(application, CBImpressionActivity::class.java)
        i.putExtra(CBConstants.CBIMPRESSIONACTIVITY_IDENTIFIER, true)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        Robolectric.buildActivity(CBImpressionActivity::class.java, i).use { controller ->
            controller.setup() // Moves Activity to RESUMED state
            val activity: CBImpressionActivity = controller.get()
            activity.getActivity()
            assertEquals(activity, activity.getActivity())
        }
    }

    @Test
    fun `configuration change`() {
        Robolectric.buildActivity(CBImpressionActivity::class.java).use { controller ->
            controller.setup()
            controller.configurationChange()
            val activity: CBImpressionActivity = controller.get()
            activity.getActivity()
            assertEquals(activity, activity.getActivity())
        }
    }
}
