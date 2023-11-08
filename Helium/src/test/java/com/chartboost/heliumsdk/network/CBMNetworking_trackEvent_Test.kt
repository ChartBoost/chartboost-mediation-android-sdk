/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.heliumsdk.domain.EventResult
import com.chartboost.heliumsdk.domain.Metrics
import com.chartboost.heliumsdk.domain.MetricsError
import com.chartboost.heliumsdk.network.ChartboostMediationNetworkingTest.Companion.LOAD_ID
import com.chartboost.heliumsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.heliumsdk.network.model.MetricsData
import com.chartboost.heliumsdk.network.model.MetricsRequestBody
import com.chartboost.heliumsdk.network.testutils.NetworkTestJsonObjects
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert

fun ChartboostMediationNetworkingTest.`verify trackEvent for initialization success`() = runTest {
    val expectedResponseHttpCode = 200
    MockResponse()
        .setResponseCode(expectedResponseHttpCode)
        .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
        .let {
            mockWebServer.enqueue(it)
        }

    val response = ChartboostMediationNetworking.trackEvent(
        event = Endpoints.Sdk.Event.INITIALIZATION,
        loadId = LOAD_ID,
        metricsRequestBody = MetricsRequestBody(
            auctionId = null,
            result = EventResult.SdkInitializationResult.InitResult2A.initResultCode,
            metrics = mutableSetOf<MetricsData>().apply {
                add(
                    MetricsData(
                        Metrics(
                            partner = "chartboost",
                            event = Endpoints.Sdk.Event.INITIALIZATION
                        ).apply {
                            start = 1681763389528L
                            end = 1681763390021L
                            duration = 493L
                            isSuccess = true
                            partnerSdkVersion = "9.2.1"
                            partnerAdapterVersion = "4.9.2.1.1"
                        }
                    )
                )
            }
        )
    )

    val request = mockWebServer.takeRequest()

    val expectedUrl = Endpoints.Sdk.Event.INITIALIZATION.endpoint
    val expectedRequestJson =
        NetworkTestJsonObjects.TRACK_EVENT_INITIALIZATION_SUCCESS_REQUEST.minifiedJsonString

    val actualUrl = request.requestUrl.toString()

    Assert.assertEquals(expectedUrl, actualUrl)
    Assert.assertEquals(
        ChartboostMediationNetworkingTest.SESSION_ID,
        request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(
        LOAD_ID,
        request.getHeader(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

    Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
    val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
    Assert.assertEquals(expectedResponseHttpCode, httpCode)
}

fun ChartboostMediationNetworkingTest.`verify trackEvent for initialization with error`() = runTest {
    val expectedResponseHttpCode = 200
    MockResponse()
        .setResponseCode(expectedResponseHttpCode)
        .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
        .let {
            mockWebServer.enqueue(it)
        }

    val metricsError = MetricsError.JsonParseError(
        chartboostMediationError = ChartboostMediationError.CM_INITIALIZATION_FAILURE_INTERNAL_ERROR,
        exception = Exception(),
        exceptionMessage = "Expected start of the object '{', but had 'EOF' instead at path: $",
        malformedJson = "\\a{\"}"
    )

    val response = ChartboostMediationNetworking.trackEvent(
        event = Endpoints.Sdk.Event.INITIALIZATION,
        loadId = LOAD_ID,
        metricsRequestBody = MetricsRequestBody(
            auctionId = null,
            result = EventResult.SdkInitializationResult.InitResult2B(metricsError).initResultCode,
            metrics = mutableSetOf<MetricsData>().apply {
                add(
                    MetricsData(
                        Metrics(
                            partner = "chartboost",
                            event = Endpoints.Sdk.Event.INITIALIZATION
                        ).apply {
                            start = 1681763389528L
                            end = 1681763390021L
                            duration = 493L
                            isSuccess = true
                            partnerSdkVersion = "9.2.1"
                            partnerAdapterVersion = "4.9.2.1.1"
                        }
                    )
                )
            },
            error = metricsError
        )
    )

    val request = mockWebServer.takeRequest()

    val expectedUrl = Endpoints.Sdk.Event.INITIALIZATION.endpoint
    val expectedRequestJson =
        NetworkTestJsonObjects.TRACK_EVENT_INITIALIZATION_SUCCESS_WITH_ERROR_REQUEST.minifiedJsonString

    val actualUrl = request.requestUrl.toString()

    Assert.assertEquals(expectedUrl, actualUrl)
    Assert.assertEquals(
        ChartboostMediationNetworkingTest.SESSION_ID,
        request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(
        LOAD_ID,
        request.getHeader(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

    Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
    val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
    Assert.assertEquals(expectedResponseHttpCode, httpCode)
}

fun ChartboostMediationNetworkingTest.`verify trackEvent for initialization failure`() = runTest {
    val expectedResponseHttpCode = 200
    MockResponse()
        .setResponseCode(expectedResponseHttpCode)
        .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
        .let {
            mockWebServer.enqueue(it)
        }

    val metricsError = MetricsError.JsonParseError(
        chartboostMediationError = ChartboostMediationError.CM_INITIALIZATION_FAILURE_INTERNAL_ERROR,
        exception = Exception(),
        exceptionMessage = "Expected start of the object '{', but had 'EOF' instead at path: $",
        malformedJson = "\\a{\"}"
    )

    val response = ChartboostMediationNetworking.trackEvent(
        event = Endpoints.Sdk.Event.INITIALIZATION,
        loadId = LOAD_ID,
        metricsRequestBody = MetricsRequestBody(
            auctionId = null,
            result = EventResult.SdkInitializationResult.InitResult1B(metricsError).initResultCode,
            metrics = emptySet(),
            error = metricsError
        )
    )

    val request = mockWebServer.takeRequest()

    val expectedUrl = Endpoints.Sdk.Event.INITIALIZATION.endpoint
    Assert.assertEquals(
        ChartboostMediationNetworkingTest.SESSION_ID,
        request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(
        LOAD_ID,
        request.getHeader(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY).toString()
    )
    val expectedRequestJson =
        NetworkTestJsonObjects.TRACK_EVENT_INITIALIZATION_FAILURE_REQUEST.minifiedJsonString

    val actualUrl = request.requestUrl.toString()

    Assert.assertEquals(expectedUrl, actualUrl)
    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

    Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
    val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
    Assert.assertEquals(expectedResponseHttpCode, httpCode)
}
