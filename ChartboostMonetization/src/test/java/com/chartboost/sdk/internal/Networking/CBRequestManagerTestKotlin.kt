package com.chartboost.sdk.internal.Networking

import com.chartboost.sdk.PlayServices.BaseTest
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.CBError.Internal
import com.chartboost.sdk.internal.Networking.CBRequestManagerTest.webViewSingleResponseTestContainer
import com.chartboost.sdk.internal.Networking.requests.CBRequest.CBAPINetworkResponseCallback
import com.chartboost.sdk.internal.Networking.requests.CBWebViewRequest
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.test.ReferenceResponse
import com.chartboost.sdk.tracking.EventTracker
import io.kotest.assertions.eq.eq
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CBRequestManagerTestKotlin : BaseTest() {
    private val eventTrackerMock =
        mockk<EventTracker>().apply {
            justRun { track(any()) }
        }

    @Before
    fun setup() {
        ChartboostDependencyContainer.start("test", "test")
    }

    /*
        If CBReachability indicates the network is unavailable, we should get a
        failure callback with error=INTERNET_UNAVAILABLE
     */
    @Test
    fun failToLoad_networkUnavailable_sendRequestCallback() {
        webViewSingleResponseTestContainer(ReferenceResponse.webviewV2InterstitialGetWithResults).use { tc ->
            every { tc.reachability.isNetworkAvailable } returns false
            val callback =
                mockk<CBAPINetworkResponseCallback>()
            val request =
                CBWebViewRequest(
                    EndpointRepository.EndPoint.INTERSTITIAL_GET.defaultValue,
                    tc.requestBodyBuilder.build(),
                    Priority.NORMAL,
                    callback,
                    eventTrackerMock,
                )
            tc.networkService.submit(request)

            val requestCaptor = CapturingSlot<CBWebViewRequest>()
            val errorCaptor = CapturingSlot<CBError>()
            // CBRequestManager.push no longer calls the failure callback in this case.
            // The NetworkDispatcher does it, so we need to run the runnables.
            tc.run()
            verify {
                callback.onFailure(
                    capture(requestCaptor),
                    capture(errorCaptor),
                )
            }
            eq(requestCaptor.captured, request)
            eq(errorCaptor.captured, Internal.INTERNET_UNAVAILABLE)
        }
    }
}
