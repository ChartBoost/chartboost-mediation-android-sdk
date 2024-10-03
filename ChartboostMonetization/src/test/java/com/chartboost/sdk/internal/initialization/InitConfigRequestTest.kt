package com.chartboost.sdk.internal.initialization

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.RequestBodyBuilder
import com.chartboost.sdk.internal.Networking.CBNetworkRequest
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.defaultUrl
import com.chartboost.sdk.internal.Networking.requests.CBRequest
import com.chartboost.sdk.internal.Priority
import com.chartboost.sdk.test.TestUtils
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

class InitConfigRequestTest {
    private val networkServiceMock = relaxedMockk<CBNetworkService>()
    private val requestBodyBuilder = relaxedMockk<RequestBodyBuilder>()
    private val callbackMock = relaxedMockk<ConfigRequestCallback>()
    private val endpointRepositoryMock =
        mockk<EndpointRepository> {
            every { getEndPointUrl(any()) } answers { firstArg<EndpointRepository.EndPoint>().defaultUrl }
        }

    private lateinit var initConfigRequest: InitConfigRequest

    private val eventTrackerMock: EventTrackerExtensions = relaxedMockk()

    @Before
    fun setup() {
        initConfigRequest =
            InitConfigRequest(
                networkService = networkServiceMock,
                requestBodyBuilder = requestBodyBuilder,
                eventTracker = eventTrackerMock,
                endpointRepository = endpointRepositoryMock,
            )
    }

    @Test
    fun `execute request`() {
        val requestSlot = CapturingSlot<CBNetworkRequest<Any>>()
        every { requestBodyBuilder.build() } returns TestUtils.createTestBodyFields()

        initConfigRequest.execute(callbackMock)

        verify { networkServiceMock.submit(capture(requestSlot)) }
        val request = requestSlot.captured
        request.shouldNotBeNull()
        request.uri shouldBe EndpointRepository.EndPoint.CONFIG.defaultUrl.toString()
        assertEquals(CBNetworkRequest.Dispatch.UI, request.dispatch)
        assertEquals(CBNetworkRequest.Method.POST, request.method)
        assertEquals(CBNetworkRequest.Status.QUEUED, request.status.get())
        assertEquals(Priority.HIGH, request.priority)
        assertNull(request.outputFile)
    }

    @Test
    fun `on request success`() {
        val requestMock = mockk<CBRequest>()
        val response = JSONObject()
        val configJson = JSONObject()
        response.put("response", configJson)
        initConfigRequest.execute(callbackMock)
        initConfigRequest.onSuccess(requestMock, response)
        verify(exactly = 1) { callbackMock.onConfigRequestSuccess(any()) }
    }

    @Test
    fun `on request failure`() {
        val requestMock = mockk<CBRequest>()
        val errorMock = CBError(CBError.Internal.HTTP_NOT_FOUND, "test error")
        initConfigRequest.execute(callbackMock)
        initConfigRequest.onFailure(requestMock, errorMock)
        verify(exactly = 1) { callbackMock.onConfigRequestFailure(errorMock.errorDesc) }
    }
}
