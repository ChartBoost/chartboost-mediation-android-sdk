package com.chartboost.sdk.internal.di

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.gms.common.ShadowGoogleApiAvailability

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    shadows = [ShadowGoogleApiAvailability::class],
    sdk = [Build.VERSION_CODES.P],
)
class DependencyContainerInternalImplTest {
    val application: Application = ApplicationProvider.getApplicationContext()

    private lateinit var sut: DependencyContainer

    @Before
    fun setup() {
        sut = DependencyContainerInternalImpl()
    }

    @Test
    fun `the android component is available after initializing the container`() {
        sut.initialize(application)

        val androidComponent = sut.androidComponent

        assertNotNull(androidComponent)
    }

    @Test
    fun `initialized returns true after calling initialize()`() {
        assertFalse(sut.initialized)

        sut.initialize(application)

        assertTrue(sut.initialized)
    }
}
