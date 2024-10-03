package com.chartboost.sdk.internal.initialization

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkRequest.Companion.API_ENDPOINT_INSTALL
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.defaultUrl
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.test.TestUtils
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InitInstallRequestTest {
    private val networkServiceMock = relaxedMockk<CBNetworkService>()
    private val requestBodyBuilder = relaxedMockk<RequestBodyBuilder>()
    private val eventTrackerMock: EventTrackerExtensions = relaxedMockk()
    private val endpointRepositoryMock: EndpointRepository =
        mockk {
            every { getEndPointUrl(any()) } answers { firstArg<EndpointRepository.EndPoint>().defaultUrl }
        }

    private val initConfigRequest =
        InitInstallRequest(
            networkService = networkServiceMock,
            requestBodyBuilder = requestBodyBuilder,
            eventTracker = eventTrackerMock,
            endpointRepository = endpointRepositoryMock,
        )

    @Test
    fun executeTest() {
        val requestSlot = CapturingSlot<CBNetworkRequest<Any>>()
        every { requestBodyBuilder.build() } returns TestUtils.createTestBodyFields()
        initConfigRequest.execute()

        verify { networkServiceMock.submit(capture(requestSlot)) }

        val request = requestSlot.captured
        assertNotNull(request)
        assertEquals(
            CBConstants.API_ENDPOINT + API_ENDPOINT_INSTALL,
            request.uri,
        )
        assertEquals(CBNetworkRequest.Dispatch.UI, request.dispatch)
        assertEquals(CBNetworkRequest.Method.POST, request.method)
        assertEquals(CBNetworkRequest.Status.QUEUED, request.status.get())
        assertEquals(Priority.NORMAL, request.priority)
        assertNull(request.outputFile)
    }

    @Test
    fun onSuccessTest() {
        val requestMock = mockk<CBRequest>()
        val response = JSONObject()
        val configJson = JSONObject()
        response.put("response", configJson)
        initConfigRequest.execute()
        initConfigRequest.onSuccess(requestMock, response)
    }

    @Test
    fun onFailureTest() {
        val requestMock = mockk<CBRequest>()
        val errorMock = CBError(CBError.Internal.HTTP_NOT_FOUND, "test")
        initConfigRequest.execute()
        initConfigRequest.onFailure(requestMock, errorMock)
    }
}
