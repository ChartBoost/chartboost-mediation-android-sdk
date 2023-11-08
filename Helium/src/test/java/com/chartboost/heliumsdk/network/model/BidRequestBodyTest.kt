/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.heliumsdk.utils.HeliumJson
import kotlinx.serialization.SerializationException
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class BidRequestBodyTest {

    @Test
    fun `deserialized banner bid request body serializes to semantically equal json string`() {
        val actualRequestBody = HeliumJson.decodeFromString(
            BidRequestBody.serializer(),
            NetworkTestJsonObjects.BANNER_BID_REQUEST.trimmedJsonString
        )

        val actualRequestBodyString = HeliumJson.encodeToString(
            BidRequestBody.serializer(),
            actualRequestBody
        )

        JSONAssert.assertEquals(
            NetworkTestJsonObjects.BANNER_BID_REQUEST.trimmedJsonString,
            actualRequestBodyString, true
        )
    }

    @Test
    fun `deserialized interstitial bid request body serializes to semantically equal json string`() {
        val actualRequestBody = HeliumJson.decodeFromString(
            BidRequestBody.serializer(),
            NetworkTestJsonObjects.INTERSTITIAL_BID_REQUEST.trimmedJsonString
        )

        val actualRequestBodyString = HeliumJson.encodeToString(
            BidRequestBody.serializer(),
            actualRequestBody
        )

        JSONAssert.assertEquals(
            NetworkTestJsonObjects.INTERSTITIAL_BID_REQUEST.trimmedJsonString,
            actualRequestBodyString, true
        )
    }

    @Test
    fun `deserialized rewarded bid request body serializes to semantically equal json string`() {
        val actualRequestBody = HeliumJson.decodeFromString(
            BidRequestBody.serializer(),
            NetworkTestJsonObjects.REWARDED_BID_REQUEST.trimmedJsonString
        )

        val actualRequestBodyString = HeliumJson.encodeToString(
            BidRequestBody.serializer(),
            actualRequestBody
        )

        JSONAssert.assertEquals(
            NetworkTestJsonObjects.REWARDED_BID_REQUEST.trimmedJsonString,
            actualRequestBodyString, true
        )
    }

    @Test(expected = SerializationException::class)
    fun `deserializing bid request body missing user object correctly throws exception`() {
        val result = HeliumJson.decodeFromString(
            BidRequestBody.serializer(),
            NetworkTestJsonObjects.BAD_BID_REQUEST.trimmedJsonString
        )

        System.out.println(result.user)
    }
}
