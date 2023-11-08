/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.domain.Bids
import com.chartboost.heliumsdk.domain.BidsResponse
import com.chartboost.heliumsdk.network.model.BidRequestBody
import com.chartboost.heliumsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.heliumsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.heliumsdk.utils.HeliumJson
import com.chartboost.heliumsdk.utils.MacroHelper
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert
import org.skyscreamer.jsonassert.JSONAssert

fun ChartboostMediationNetworkingTest.`verify rewarded callback GET request success`() = runTest {
    val expectedResponseHttpCode = 200
    MockResponse()
        .setResponseCode(expectedResponseHttpCode)
        .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
        .let {
            mockWebServer.enqueue(it)
        }

    val bidsResponse = HeliumJson.decodeFromString(
        BidsResponse.serializer(),
        NetworkTestJsonObjects.REWARDED_BID_RESPONSE_GET_CALLBACK.rawJsonString.replace(
            "https://chartboost.com",
            getMockServerUrl() + "/"
        )
    )

    val activeBid = Bids(
        setupRewardedAdLoadParams(),
        bidsResponse,
    ).activeBid

    Assert.assertNotNull(activeBid)

    val rewardedCallbackData = bidsResponse.bidsExt?.rewardedCallbackData

    Assert.assertNotNull(rewardedCallbackData)

    val macroHelper = activeBid!!.let {
        MacroHelper(
            System.currentTimeMillis(),
            "",
            it.adRevenue,
            it.cpmPrice,
            it.partnerName
        )
    }

    val expectedUrl = macroHelper.replaceMacros(rewardedCallbackData!!.url, true)
    val response = ChartboostMediationNetworking.makeRewardedCallbackRequest(
        activeBid,
        "",
        rewardedCallbackData
    )

    val request = mockWebServer.takeRequest()

    val actualUrl = request.requestUrl.toString()

    Assert.assertEquals(expectedUrl, actualUrl)

    Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
    val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
    Assert.assertEquals(expectedResponseHttpCode, httpCode)
}


fun ChartboostMediationNetworkingTest.`verify rewarded callback POST request success`() = runTest {
    val expectedResponseHttpCode = 200
    MockResponse()
        .setResponseCode(expectedResponseHttpCode)
        .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
        .let {
            mockWebServer.enqueue(it)
        }

    val bidsResponse = HeliumJson.decodeFromString(
        BidsResponse.serializer(),
        NetworkTestJsonObjects.REWARDED_BID_RESPONSE_POST_CALLBACK.rawJsonString.replace(
            "https://chartboost.com",
            getMockServerUrl() + "/"
        )
    )

    val activeBid = Bids(
        setupRewardedAdLoadParams(),
        bidsResponse,
    ).activeBid

    Assert.assertNotNull(activeBid)

    val rewardedCallbackData = bidsResponse.bidsExt?.rewardedCallbackData

    Assert.assertNotNull(rewardedCallbackData)

    val macroHelper = activeBid!!.let {
        MacroHelper(
            System.currentTimeMillis(),
            "",
            it.adRevenue,
            it.cpmPrice,
            it.partnerName
        )
    }

    val expectedUrl = macroHelper.replaceMacros(rewardedCallbackData!!.url, true)
    val response = ChartboostMediationNetworking.makeRewardedCallbackRequest(
        activeBid,
        "",
        rewardedCallbackData
    )

    val request = mockWebServer.takeRequest()

    val actualUrl = request.requestUrl.toString()

    Assert.assertEquals(expectedUrl, actualUrl)

    Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
    val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
    Assert.assertEquals(expectedResponseHttpCode, httpCode)

    JSONAssert.assertEquals(
        rewardedCallbackData!!.body,
        request.body.readUtf8(),
        true
    )
}
