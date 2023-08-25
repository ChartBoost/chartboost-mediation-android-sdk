/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

data class PartnerConfiguration(val credentials: JsonObject = buildJsonObject {  })
