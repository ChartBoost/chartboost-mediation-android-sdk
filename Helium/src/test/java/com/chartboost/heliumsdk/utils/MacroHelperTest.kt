/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class MacroHelperTest {

    companion object {
        private const val EXPECTED_TIMESTAMP = 1234567890L
        private const val EXPECTED_CUSTOM_DATA = "`~😅!@#$%^&*()_+-={}|[]\\\\\\\""
        private const val EXPECTED_CUSTOM_DATA_URLENCODED =
            "%60~%F0%9F%98%85!%40%23%24%25%5E%26*()_%2B-%3D%7B%7D%7C%5B%5D%5C%5C%5C%22"
        private const val EXPECTED_AD_REVENUE = 0.02
        private const val EXPECTED_CPM_PRICE = 20.0
        private const val EXPECTED_NETWORK_NAME = "network"
        private const val SAMPLE_GET_URL =
            "https://mac.rohelp.er/q?sdkts=%%SDK_TIMESTAMP%%&data=%%CUSTOM_DATA%%&money=%%AD_REVENUE%%&net=%%NETWORK_NAME%%&cpm=%%CPM_PRICE%%&another=%%UNSUPPORTED%%"
        private const val SAMPLE_POST_BODY =
            "{\"user\":\"user\", \"tx\":\"4ae716c5d24bcff17672bd553a8f7698e9bec32d\", \"data\":\"%%CUSTOM_DATA%%\", \"sdkts\":\"%%SDK_TIMESTAMP%%\", \"ts\":\"1643327180539\", \"hash\": \"abcd\", \"money\":%%AD_REVENUE%%, \"cpm\":%%CPM_PRICE%%, \"net\":\"%%NETWORK_NAME%%\"}\""
    }

    lateinit var subject: MacroHelper

    @Before
    fun setUp() {
        subject = MacroHelper(
            EXPECTED_TIMESTAMP,
            EXPECTED_CUSTOM_DATA,
            EXPECTED_AD_REVENUE,
            EXPECTED_CPM_PRICE,
            EXPECTED_NETWORK_NAME
        )
    }

    @Test
    fun replaceMacros_withUrlEncode_shouldDoMacroReplacement() {
        val actual = subject.replaceMacros(SAMPLE_GET_URL)

        assertEquals(
            "https://mac.rohelp.er/q?sdkts=1234567890&data=$EXPECTED_CUSTOM_DATA_URLENCODED&money=0.02&net=network&cpm=20.0&another=%%UNSUPPORTED%%",
            actual
        )
    }

    @Test
    fun replaceMacros_withUrlEncode_withNulls_shouldDoMacroReplacement() {
        subject = MacroHelper(
            EXPECTED_TIMESTAMP,
            null,
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            EXPECTED_NETWORK_NAME
        )
        val actual = subject.replaceMacros(SAMPLE_GET_URL)

        assertEquals(
            "https://mac.rohelp.er/q?sdkts=1234567890&data=&money=&net=network&cpm=&another=%%UNSUPPORTED%%",
            actual
        )
    }

    @Test
    fun replaceMacros_withNoUrlEncode_shouldDoMacroReplacement() {
        val actual = subject.replaceMacros(SAMPLE_POST_BODY, false)

        assertEquals(
            "{\"user\":\"user\", \"tx\":\"4ae716c5d24bcff17672bd553a8f7698e9bec32d\", \"data\":\"`~\uD83D\uDE05!@#\$%^&*()_+-={}|[]\\\\\\\"\", \"sdkts\":\"1234567890\", \"ts\":\"1643327180539\", \"hash\": \"abcd\", \"money\":0.02, \"cpm\":20.0, \"net\":\"network\"}\"",
            actual
        )
        JSONObject(actual)
    }

    @Test
    fun replaceMacros_withNoUrlEncode_withNulls_shouldDoMacroReplacement() {
        subject = MacroHelper(
            EXPECTED_TIMESTAMP,
            null,
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            EXPECTED_NETWORK_NAME
        )
        val actual = subject.replaceMacros(SAMPLE_POST_BODY, false)

        assertEquals(
            "{\"user\":\"user\", \"tx\":\"4ae716c5d24bcff17672bd553a8f7698e9bec32d\", \"data\":\"\", \"sdkts\":\"1234567890\", \"ts\":\"1643327180539\", \"hash\": \"abcd\", \"money\":null, \"cpm\":null, \"net\":\"network\"}\"",
            actual
        )
        val expectedJson = JSONObject()
        expectedJson.put("user", "user")
        expectedJson.put("tx", "4ae716c5d24bcff17672bd553a8f7698e9bec32d")
        expectedJson.put("data", "")
        expectedJson.put("sdkts", "1234567890")
        expectedJson.put("ts", "1643327180539")
        expectedJson.put("hash", "abcd")
        expectedJson.put("net", "network")
        val actualJsonObject = JSONObject(actual)
        MacroHelper.scrubNulls(actualJsonObject)
        JSONAssert.assertEquals(expectedJson, actualJsonObject, true)
    }

    @Test
    fun scrubNulls_withNulls_shouldRemoveNullValues() {
        val json = JSONObject()
        json.put("key1", null as? JSONObject?)
        json.put("key2", JSONObject.NULL as? JSONObject?)
        json.put("key3", JSONObject())

        MacroHelper.scrubNulls(json)

        val expectedJson = JSONObject()
        expectedJson.put("key3", JSONObject())
        JSONAssert.assertEquals(expectedJson, json, true)
    }

    @Test
    fun scrubNulls_withJSONArrays_shouldRemoveNullValues() {
        val jsonArray1 = JSONArray()
        val jsonArray2 = JSONArray()
        jsonArray2.put(JSONObject().also {
            it.put("abc", JSONObject.NULL as? JSONObject?)
        })
        jsonArray2.put(null as? JSONObject?)
        jsonArray2.put(JSONObject.NULL as? JSONObject?)
        jsonArray2.put("def")
        jsonArray1.put(jsonArray2)
        jsonArray1.put(null as? JSONObject?)
        jsonArray1.put(JSONObject.NULL as? JSONObject?)
        jsonArray1.put("ghi")
        jsonArray1.put(JSONObject().also {
            it.put("jkl", "mno")
            it.put("pqr", JSONObject.NULL as? JSONObject?)
        })
        val testObject = JSONObject()
        testObject.put("thisisnull", JSONObject.NULL as? JSONObject?)
        testObject.put("array1", jsonArray1)
        testObject.put("array2", jsonArray2)

        MacroHelper.scrubNulls(testObject)

        val expectedJsonArray1 = JSONArray()
        val expectedJsonArray2 = JSONArray()
        expectedJsonArray2.put(JSONObject())
        expectedJsonArray2.put("def")
        expectedJsonArray1.put(expectedJsonArray2)
        expectedJsonArray1.put("ghi")
        expectedJsonArray1.put(JSONObject().also {
            it.put("jkl", "mno")
        })
        val expectedObject = JSONObject()
        expectedObject.put("array1", expectedJsonArray1)
        expectedObject.put("array2", expectedJsonArray2)
        JSONAssert.assertEquals(expectedObject, testObject, true)
    }
}
