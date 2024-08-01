/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentValue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

data class PartnerConfiguration(
    /**
     * A JSON object containing any partner-specific information required on setup.
     */
    val credentials: JsonObject = buildJsonObject { },
    /**
     * Initial consent info at the moment of set up.
     */
    val consents: Map<ConsentKey, ConsentValue>,
    /**
     * Indicates if the user is underage as determined by the publisher.
     */
    val isUserUnderage: Boolean?,
)
