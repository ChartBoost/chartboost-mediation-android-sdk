/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import com.chartboost.chartboostmediationsdk.controllers.PrivacyController
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworkingTest
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.chartboostmediationsdk.utils.LogController
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class ChartboostMediationSdkTest {
    @MockK
    private lateinit var mockedContext: Context

    @MockK
    private lateinit var mockedSharedPreferences: SharedPreferences

    @MockK
    private lateinit var mockedPrivacyController: PrivacyController

    private var testCoroutineScheduler = TestCoroutineScheduler()

    private var testIoDispatcher = StandardTestDispatcher(testCoroutineScheduler)

    private val mockWebServer = MockWebServer()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        ChartboostMediationSdk.chartboostMediationInternal = ChartboostMediationInternal(ioDispatcher = testIoDispatcher)
        ChartboostMediationNetworking.ioDispatcher = testIoDispatcher

        val url =
            mockWebServer.url("").toString()

        Endpoints.SDK_HOSTNAME = mockWebServer.hostName

        mockkObject(Endpoints.Sdk.SDK_INIT)
        mockkObject(Endpoints.Event.INITIALIZATION)

        MockKAnnotations.init(this)
        // Setup mocks that will be used in our tests.
        mockedContext = mockk<Context>()
        mockedPrivacyController = mockk<PrivacyController>()
        mockedSharedPreferences = mockk<SharedPreferences>()

        // Our LogController is static and needs to be mocked for captures.
        mockkStatic("com.chartboost.chartboostmediationsdk.utils.LogController")

        // Handle behaviours for our mocks.
        every { mockedContext.applicationContext } returns RuntimeEnvironment.getApplication().applicationContext
        every { mockedContext.getSharedPreferences(any(), any()) } returns mockedSharedPreferences
        every { mockedSharedPreferences.getString(any(), any()) } returns ""
        every { mockedSharedPreferences.edit() }

        coEvery { Endpoints.Sdk.SDK_INIT.endpoint } returns
            url + "${Endpoints.Sdk.SDK_INIT.version}/${Endpoints.Sdk.SDK_INIT.name.lowercase()}"
        coEvery { Endpoints.Event.INITIALIZATION.endpoint } returns
            url + "${Endpoints.Event.INITIALIZATION.version}/${Endpoints.Event.INITIALIZATION.name.lowercase()}"

        // We may be hitting various levels of logging during the tests. Let's justRun.
        mockkObject(LogController)
        every { LogController.d(any()) } just Runs
        every { LogController.e(any()) } just Runs
        every { LogController.i(any()) } just Runs
        every { LogController.v(any()) } just Runs
        every { LogController.w(any()) } just Runs

        ShadowLooper.runUiThreadTasks()
        Dispatchers.setMain(StandardTestDispatcher(testCoroutineScheduler))

        MockResponse()
            .setResponseCode(200)
            .setHeader(
                ChartboostMediationNetworking.INIT_HASH_HEADER_KEY,
                ChartboostMediationNetworkingTest.INIT_HASH_NEW,
            ).setBody(NetworkTestJsonObjects.HTTP_200_SDK_INIT_SUCCESS.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }
    }

    @Test
    fun `initialize when Chartboost Mediation SDK is initialized twice should log`() =
        runTest {
            val context = RuntimeEnvironment.getApplication().baseContext

            var result1: Result<Unit>? = null
            var result2: Result<Unit>? = null
            // Start the Chartboost Mediation SDK
            result1 = ChartboostMediationSdk.initialize(context, "app_id")
            result2 = ChartboostMediationSdk.initialize(context, "app_id")

            // Verify that no error has been captured and there are no errors.
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            verify { LogController.d("Chartboost Mediation already initialized.") }
        }

    @Test
    fun `start when Chartboost Mediation SDK is initialized should return success result`() =
        runTest {
            // Create a mocked ChartboostMediationSdkListener and a slot to check for errors.
            val context = RuntimeEnvironment.getApplication().baseContext

            // Start the Chartboost Mediation SDK
            val result = ChartboostMediationSdk.initialize(context, "app_id")

            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(result.isSuccess)
            verify(exactly = 2) { LogController.d(any()) }
        }

    @Test
    fun `start when application context is passed should not trigger errors`() =
        runTest {
            // Start the Chartboost Mediation SDK
            val result =
                ChartboostMediationSdk.initialize(
                    mockedContext.applicationContext,
                    "app_id",
                )

            // Verify no errors were detected.
            assertTrue(result.isSuccess)
        }

    @Test
    fun `getVersion when SDK is not initialized should return version`() {
        // Assert that the getVersion method is the correct current version.
        assertEquals(BuildConfig.CHARTBOOST_MEDIATION_VERSION, ChartboostMediationSdk.getVersion())
    }

    @Test
    fun `getAppId when Chartboost Mediation is initialized should return app id`() =
        runTest {
            val appID = "appid12345"

            // Start the Chartboost Mediation Sdk
            ChartboostMediationSdk.initialize(
                RuntimeEnvironment.getApplication().baseContext,
                appID,
            )

            assertEquals(appID, ChartboostMediationSdk.getAppId())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        ChartboostMediationNetworking.ioDispatcher = Dispatchers.IO
        ChartboostMediationSdk.chartboostMediationInternal = ChartboostMediationInternal()
        Dispatchers.resetMain()
        clearAllMocks()
        mockWebServer.shutdown()
    }
}
