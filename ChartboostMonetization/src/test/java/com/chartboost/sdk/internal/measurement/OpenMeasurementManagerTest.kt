package com.chartboost.sdk.internal.measurement

import android.content.Context
import android.os.Build
import com.chartboost.sdk.internal.Model.OmSdkModel
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.utils.ResourceLoader
import com.chartboost.sdk.internal.utils.SharedPrefsHelper
import com.iab.omid.library.chartboost.Omid
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class OpenMeasurementManagerTest {
    private val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
    private val sharedPrefsHelperMock = mockk<SharedPrefsHelper>()
    private val resourceLoaderMock = mockk<ResourceLoader>()
    private val sdkConfigurationMock = mockk<SdkConfiguration>()
    private val atomicRef = AtomicReference(sdkConfigurationMock)
    private val openMeasurementManager =
        OpenMeasurementManager(app, sharedPrefsHelperMock, resourceLoaderMock, atomicRef, UnconfinedTestDispatcher())

    @Before
    fun setup() {
        every { sdkConfigurationMock.omSdkConfig } returns OmSdkModel(true)
    }

    @Test
    fun `initialize om sdk success`() {
        openMeasurementManager.initialize()
        assertTrue(Omid.isActive())
    }

    @Test
    fun `inject omid js into html success`() {
        every { sharedPrefsHelperMock.loadFromSharedPrefs(any()) } returns "data"
        val html = "test"
        openMeasurementManager.initialize()
        val output = openMeasurementManager.injectOmidJsIntoHtml(html)
        assertNotNull(output)
        assertEquals("<script type=\"text/javascript\">data</script>test", output)
    }

    @Test
    fun `inject omid js into html disabled`() {
        every { sdkConfigurationMock.omSdkConfig } returns OmSdkModel(false)
        every { sharedPrefsHelperMock.loadFromSharedPrefs(any()) } returns "data"
        val html = "test"
        openMeasurementManager.initialize()
        val output = openMeasurementManager.injectOmidJsIntoHtml(html)
        assertNotNull(output)
        assertEquals("test", output)
    }

    @Test
    fun getOmidPartner() {
        val partner = openMeasurementManager.getOmidPartner()
        assertNotNull(partner)
        assertEquals("Chartboost", partner?.name)
    }
}
