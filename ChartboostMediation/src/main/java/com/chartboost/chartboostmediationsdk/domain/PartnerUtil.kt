/*
 * Copyright 2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object PartnerUtil {
    /**
     * Compile a list of partners from the given JSON object.
     */
    fun compilePartners(credentials: JsonObject): Set<Partner> {
        return credentials.mapNotNull { (partnerId, jsonElement) ->
            runCatching {
                Partner(partnerId, jsonElement.jsonObject)
            }.getOrNull()
        }.toSet()
    }
}
