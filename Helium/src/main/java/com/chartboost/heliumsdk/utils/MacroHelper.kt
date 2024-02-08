/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.net.Uri
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/**
 * @suppress
 */
class MacroHelper(
    private val timestamp: Long,
    private val customData: String?,
    private val adRevenue: Double,
    private val cpmPrice: Double,
    private val networkName: String,
) {
    companion object {
        private const val SDK_TIMESTAMP_MACRO = "%%SDK_TIMESTAMP%%"
        private const val CUSTOM_DATA_MACRO = "%%CUSTOM_DATA%%"
        private const val AD_REVENUE_MACRO = "%%AD_REVENUE%%"
        private const val CPM_PRICE_MACRO = "%%CPM_PRICE%%"
        private const val NETWORK_NAME_MACRO = "%%NETWORK_NAME%%"

        /**
         * Removes all null and JSON NULL objects from the passed in JSONObject. This will
         * remove all keys with null values within the object and within any nested object
         * or JSONArray. This method is NOT thread-safe and modifies the passed in object.
         * @param json The JSONObject to delete all nulls from
         */
        fun scrubNulls(json: JSONObject) {
            val iterator = json.keys()
            while (iterator.hasNext()) {
                val token = json.opt(iterator.next())
                if (token == null || JSONObject.NULL.equals(token)) {
                    iterator.remove()
                } else if (token is JSONObject) {
                    scrubNulls(token)
                } else if (token is JSONArray) {
                    scrubNullsJsonArray(token)
                }
            }
        }

        private fun scrubNullsJsonArray(array: JSONArray) {
            var index = 0
            while (index < array.length()) {
                val token = array.opt(index)
                if (token == null || JSONObject.NULL.equals(token)) {
                    // Don't think we actually allow under 19, but just in case
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        array.remove(index)
                        // Don't increase index since we removed that element
                        continue
                    }
                } else if (token is JSONObject) {
                    scrubNulls(token)
                } else if (token is JSONArray) {
                    scrubNullsJsonArray(token)
                }
                index++
            }
        }
    }

    fun replaceMacros(
        data: String,
        urlEncode: Boolean = true,
    ): String {
        // We're using urlEncode to also say whether or not to pass in 'null' for bad doubles
        return data.replace(SDK_TIMESTAMP_MACRO, timestamp.toString())
            .replace(
                CUSTOM_DATA_MACRO,
                (if (urlEncode) Uri.encode(customData) else customData) ?: "",
            ).replace(AD_REVENUE_MACRO, prettyPrintDouble(adRevenue, !urlEncode))
            .replace(CPM_PRICE_MACRO, prettyPrintDouble(cpmPrice, !urlEncode))
            .replace(
                NETWORK_NAME_MACRO,
                (if (urlEncode) Uri.encode(networkName) else networkName) ?: "",
            )
    }

    private fun prettyPrintDouble(
        value: Double,
        useNullValues: Boolean,
    ): String {
        if (value.isNaN() || value.isInfinite()) {
            return if (useNullValues) "null" else ""
        }
        return value.toString()
    }
}
