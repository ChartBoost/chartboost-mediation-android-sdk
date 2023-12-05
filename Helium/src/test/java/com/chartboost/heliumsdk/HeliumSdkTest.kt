/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Looper
import com.chartboost.heliumsdk.common.CMTestUtilitiesTest
import com.chartboost.heliumsdk.common.CMTestUtilitiesTest.setFinalStatic
import com.chartboost.heliumsdk.controllers.PrivacyController
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking
import com.chartboost.heliumsdk.network.ChartboostMediationNetworkingTest
import com.chartboost.heliumsdk.network.Endpoints
import com.chartboost.heliumsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.LogController
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class HeliumSdkTest {
    @MockK
    private lateinit var mockedContext: Context

    @MockK
    private lateinit var mockedSharedPreferences: SharedPreferences

    @MockK
    private lateinit var mockedPrivacyController: PrivacyController

    private val mockWebServer = MockWebServer()

    @Before
    fun setUp() {
        setFinalStatic(
            Build.VERSION::class.java.getField("RELEASE"),
            ChartboostMediationNetworkingTest.BUILD_RELEASE
        )

        val url = mockWebServer.url("").toString().let {
            it.take(it.length - 1)
        }

        Endpoints.SDK_DOMAIN = url
        Endpoints.RTB_DOMAIN = url

        mockkObject(Environment)
        every { Environment.sessionId } returns ChartboostMediationNetworkingTest.SESSION_ID

        MockKAnnotations.init(this)
        // Setup mocks that will be used in our tests.
        mockedContext = mockk<Context>()
        mockedPrivacyController = mockk<PrivacyController>()
        mockedSharedPreferences = mockk<SharedPreferences>()

        // Our LogController is static and needs to be mocked for captures.
        mockkStatic("com.chartboost.heliumsdk.utils.LogController")

        // Handle behaviours for our mocks.
        every { mockedContext.applicationContext } returns RuntimeEnvironment.getApplication().applicationContext
        every { mockedContext.getSharedPreferences(any(), any()) } returns mockedSharedPreferences
        every { mockedSharedPreferences.getString(any(), any()) } returns ""
        every { mockedSharedPreferences.edit() }

        // We may be hitting various levels of logging during the tests. Let's justRun.
        mockkObject(LogController)
        every { LogController.d(any()) } just Runs
        every { LogController.e(any()) } just Runs
        every { LogController.i(any()) } just Runs
        every { LogController.v(any()) } just Runs
        every { LogController.w(any()) } just Runs

        ShadowLooper.runUiThreadTasks()
    }

    @Test
    @Ignore
    fun `start when Helium SDK is initialized twice should trigger an error when in progress`() = runBlocking {
        // Create a mocked HeliumSdkListener and a slot to check for errors.
        val mockInitListener = mockk<HeliumSdk.HeliumSdkListener>()
        val listError = mutableListOf<Error>()
        val context = RuntimeEnvironment.getApplication().baseContext

        MockResponse()
            .setResponseCode(200)
            .setHeader(
                ChartboostMediationNetworking.INIT_HASH_HEADER_KEY,
                ChartboostMediationNetworkingTest.INIT_HASH_NEW
            )
            .setBody(NetworkTestJsonObjects.HTTP_200_SDK_INIT_FAILURE.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }

        // Capture the Error so that we can later verify.
        justRun { mockInitListener.didInitialize(capture(listError)) }

        // Start the HeliumSDK
        withContext(IO) {
            HeliumSdk.start(context, "app_id", "app_signature", null, mockInitListener)
            delay(1000L)
        }

        // Verify that no error has been captured and there are no errors.
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(listError.isEmpty())
        verify(exactly = 0) { mockInitListener.didInitialize(any()) }

        // Start the Helium SDK again
        withContext(IO) {
            HeliumSdk.start(context, "app_id", "app_signature", null, mockInitListener)
            delay(1000L)
        }

        // Verify that our error has been captured and the mock delegate is triggered.
        // This is needed so that we can capture the Error.
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, listError.size)
        verify(exactly = 2) { mockInitListener.didInitialize(any()) }
        confirmVerified(mockInitListener)
    }

    @Test
    fun `start when Helium SDK is initialized should trigger config listener`() {
        // Create a mocked HeliumSdkListener and a slot to check for errors.
        val mockInitListener = mockk<HeliumSdk.HeliumSdkListener>()
        val slotError = slot<Error>()
        val context = RuntimeEnvironment.getApplication().baseContext
        // Capture the Error so that we can later verify.
        justRun { mockInitListener.didInitialize(capture(slotError)) }

        // Start the HeliumSDK
        HeliumSdk.start(context, "app_id", "app_signature", null, mockInitListener)

        shadowOf(Looper.getMainLooper()).idle()
        // Verify that no error has been captured and there are no errors.
        assertFalse(slotError.isCaptured)
        verify(exactly = 0) { mockInitListener.didInitialize(any()) }
        verify(exactly = 1) { LogController.d(any()) }
    }

    @Test
    fun `start when application context is passed should not trigger errors`() {
        // Create a mocked HeliumSdkListener and a slot to check for errors.
        val mockInitListener = mockk<HeliumSdk.HeliumSdkListener>()

        // Start the HeliumSDK
        HeliumSdk.start(
            mockedContext.applicationContext,
            "app_id",
            "app_signature",
            null,
            mockInitListener
        )

        // Verify no errors were detected.
        verify(exactly = 0) { mockInitListener.didInitialize(any()) }
    }

    @Test
    fun `getVersion when SDK is not initialized should return version`() {
        // Assert that the getVersion method is the correct current version.
        assertEquals(BuildConfig.CHARTBOOST_MEDIATION_VERSION, HeliumSdk.getVersion())
    }

    @Test
    fun `getAppId when Helium is initialized should return app id`() {
        val appID = "appid12345"

        // Start the HeliumSdk
        HeliumSdk.start(
            RuntimeEnvironment.getApplication().baseContext,
            appID,
            "app_signature",
            null,
            object: HeliumSdk.HeliumSdkListener {
                override fun didInitialize(error: Error?) {
                    assertEquals(appID, HeliumSdk.getAppId())
                }
            }
        )
    }

    @Test
    fun `getAppSignature when Helium is init should return app signature`() {
        val appSig = "abcdefghojklmnopqrstuvwxyz01234567"

        // Start the HeliumSdk
        HeliumSdk.start(RuntimeEnvironment.getApplication().baseContext, "app_id", appSig, null, object: HeliumSdk.HeliumSdkListener {
            override fun didInitialize(error: Error?) {
                assertEquals(appSig, HeliumSdk.getAppSignature())
            }
        })
    }

    @After
    fun tearDown() {
        // Reset the internal singleton for other tests.
        CMTestUtilitiesTest.resetChartboostMediationInternal()
        clearAllMocks()
        mockWebServer.shutdown()
    }
}
