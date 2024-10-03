/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import kotlinx.serialization.json.*
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.Before
import org.junit.Test

class BidTest {
    companion object {
        private val SAMPLE_BID =
            "{" +
                "            \"bid\": [" +
                "                {" +
                "                    \"id\": \"MEDIATION\"," +
                "                    \"impid\": \"1\"," +
                "                    \"price\": 123.4," +
                "                    \"ext\": {" +
                "                        \"partner_placement\": \"490251\"," +
                "                        \"line_item_id\": \"-- LINE_ITEM_ID --\"," +
                "                        \"ilrd\": {" +
                "                            \"network_name\": \"fyber\"," +
                "                            \"network_type\": \"mediation\"," +
                "                            \"precision\": \"publisher_defined\"," +
                "                            \"line_item_name\": \"-- LINE_ITEM_NAME --\"," +
                "                            \"network_placement_id\": \"-- NETWORK_PLACEMENT --\"," +
                "                            \"ad_revenue\": 123.4" +
                "                        }," +
                "                        \"cpm_price\": 123.4," +
                "                        \"ad_revenue\": 0.1234" +
                "                    }" +
                "                }" +
                "            ]," +
                "            \"seat\": \"fyber\"," +
                "            \"helium_bid_id\": \"-- HELIUM_BID_ID --\"" +
                "        }"

        private val BID_WITH_NULL_CPM =
            "{" +
                "            \"bid\": [" +
                "                {" +
                "                    \"id\": \"MEDIATION\"," +
                "                    \"impid\": \"1\"," +
                "                    \"price\": 123.4," +
                "                    \"ext\": {" +
                "                        \"partner_placement\": \"490251\"," +
                "                        \"line_item_id\": \"-- LINE_ITEM_ID --\"," +
                "                        \"ilrd\": {" +
                "                            \"network_name\": \"fyber\"," +
                "                            \"network_type\": \"mediation\"," +
                "                            \"precision\": \"publisher_defined\"," +
                "                            \"line_item_name\": \"-- LINE_ITEM_NAME --\"," +
                "                            \"network_placement_id\": \"-- NETWORK_PLACEMENT --\"," +
                "                            \"ad_revenue\": null" +
                "                        }," +
                "                        \"cpm_price\": null," +
                "                        \"ad_revenue\": null" +
                "                    }" +
                "                }" +
                "            ]," +
                "            \"seat\": \"fyber\"," +
                "            \"helium_bid_id\": \"-- HELIUM_BID_ID --\"" +
                "        }"

        private const val ANOTHER_KEY = "another_key"
        private const val ANOTHER_VALUE = "another_value"
        private const val NUMBER_KEY = "number_key"
        private const val NUMBER_VALUE = 3.1
        private const val BOOLEAN_KEY = "boolean_key"
        private const val BOOLEAN_VALUE = true
        private const val NESTED_JSON_OBJ_KEY = "nested_json"
        private val NESTED_JSON_OBJECT =
            buildJsonObject {
                put("nested", "value")
            }
        private const val NETWORK_NAME_KEY = "network_name"
        private const val AD_REVENUE_KEY = "ad_revenue"
        private const val CPM_PRICE_VALUE = 123.4
        private const val AD_REVENUE_VALUE = 0.1234
        private const val PARTNER_NAME = "fyber"
    }

    private lateinit var subject: Bid
    private lateinit var expectedJsonObject: JsonObject
    private val adIdentifier: AdIdentifier = AdIdentifier(Ad.AdType.INTERSTITIAL, "-- NETWORK_PLACEMENT --")

    @Before
    fun setUp() {
        val sampleBidJson = ChartboostMediationJson.decodeFromString<BidResponse>(SAMPLE_BID)
        subject = Bid(null, adIdentifier, sampleBidJson, "loadRequestId")

        expectedJsonObject =
            buildJsonObject {
                put(NETWORK_NAME_KEY, PARTNER_NAME)
                put("network_type", "mediation")
                put("precision", "publisher_defined")
                put("line_item_name", "-- LINE_ITEM_NAME --")
                put("network_placement_id", "-- NETWORK_PLACEMENT --")
                put("ad_revenue", CPM_PRICE_VALUE)
            }
    }

    @Test
    fun constructor_shouldInitializeFields() {
        val ilrdJson = subject.ilrd!!

        assertThatJson(ilrdJson).isEqualTo(expectedJsonObject)
        assertEquals(subject.adRevenue, AD_REVENUE_VALUE)
        assertEquals(subject.cpmPrice, CPM_PRICE_VALUE)
    }

    @Test
    fun updateIlrd_withGlobalIlrd_shouldAddIlrd() {
        val globalIlrdJson =
            buildJsonObject {
                put(ANOTHER_KEY, ANOTHER_VALUE)
                put(NUMBER_KEY, NUMBER_VALUE)
                put(BOOLEAN_KEY, BOOLEAN_VALUE)
                put(NESTED_JSON_OBJ_KEY, NESTED_JSON_OBJECT)
            }

        expectedJsonObject =
            buildJsonObject {
                (expectedJsonObject as Map<String, JsonElement>).entries.forEach {
                    put(it.key, it.value)
                }
                put(ANOTHER_KEY, ANOTHER_VALUE)
                put(NUMBER_KEY, NUMBER_VALUE)
                put(BOOLEAN_KEY, BOOLEAN_VALUE)
                put(NESTED_JSON_OBJ_KEY, NESTED_JSON_OBJECT)
            }

        subject.updateIlrd(globalIlrdJson, false)

        val ilrdJson = subject.ilrd!!
        assertThatJson(ilrdJson).isEqualTo(expectedJsonObject)
    }

    @Test
    fun updateIlrd_withGlobalIlrd_withOverwriteTrue_shouldOverwriteAllIlrd() {
        val globalIlrdJson =
            buildJsonObject {
                put(ANOTHER_KEY, ANOTHER_VALUE)
                put(NUMBER_KEY, NUMBER_VALUE)
                put(BOOLEAN_KEY, BOOLEAN_VALUE)
                put(NESTED_JSON_OBJ_KEY, NESTED_JSON_OBJECT)
                put(NETWORK_NAME_KEY, "new_network")
                put(AD_REVENUE_KEY, 25.1)
            }

        expectedJsonObject =
            buildJsonObject {
                (expectedJsonObject as Map<String, JsonElement>).entries.forEach {
                    put(it.key, it.value)
                }
                put(ANOTHER_KEY, ANOTHER_VALUE)
                put(NUMBER_KEY, NUMBER_VALUE)
                put(BOOLEAN_KEY, BOOLEAN_VALUE)
                put(NESTED_JSON_OBJ_KEY, NESTED_JSON_OBJECT)
                put(NETWORK_NAME_KEY, "new_network")
                put(AD_REVENUE_KEY, 25.1)
            }

        subject.updateIlrd(globalIlrdJson, true)

        val ilrdJson = subject.ilrd!!
        assertThatJson(ilrdJson).isEqualTo(expectedJsonObject)
    }

    @Test
    fun updateIlrd_withGlobalIlrd_withOverwriteFalse_shouldIgnoreDuplicateValues() {
        val globalIlrdJson =
            buildJsonObject {
                put(ANOTHER_KEY, ANOTHER_VALUE)
                put(NUMBER_KEY, NUMBER_VALUE)
                put(BOOLEAN_KEY, BOOLEAN_VALUE)
                put(NESTED_JSON_OBJ_KEY, NESTED_JSON_OBJECT)
                put(NETWORK_NAME_KEY, "new_network")
                put(AD_REVENUE_KEY, 25.1)
            }

        expectedJsonObject =
            buildJsonObject {
                (expectedJsonObject as Map<String, JsonElement>).entries.forEach {
                    put(it.key, it.value)
                }
                put(ANOTHER_KEY, ANOTHER_VALUE)
                put(NUMBER_KEY, NUMBER_VALUE)
                put(BOOLEAN_KEY, BOOLEAN_VALUE)
                put(NESTED_JSON_OBJ_KEY, NESTED_JSON_OBJECT)
            }

        subject.updateIlrd(globalIlrdJson, false)

        val ilrdJson = subject.ilrd!!

        assertThatJson(ilrdJson).isEqualTo(expectedJsonObject)
    }

    @Test
    fun getBidInfo_withNullCpm_shouldNotIncludeCpm() {
        val bidWithNullCpm = ChartboostMediationJson.decodeFromString<BidResponse>(BID_WITH_NULL_CPM)

        subject =
            Bid(
                null,
                adIdentifier,
                bidWithNullCpm,
                "loadRequestId",
            )

        val bidInfo = subject.bidInfo

        assertEquals(PARTNER_NAME, bidInfo["partner_id"])
        assertFalse(bidInfo.contains("price"))
    }
}
