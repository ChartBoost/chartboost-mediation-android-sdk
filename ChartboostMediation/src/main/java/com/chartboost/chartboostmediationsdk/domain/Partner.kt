/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.json.JsonObject

/**
 * @suppress
 */
data class Partner(
    /**
     * The unique identifier in the backend that represents this Partner.
     */
    val partnerId: String,
    /**
     * Credentials for partner initialization. appId, sdk_key, etc, in JSON form.
     */
    val credentials: JsonObject,
)
