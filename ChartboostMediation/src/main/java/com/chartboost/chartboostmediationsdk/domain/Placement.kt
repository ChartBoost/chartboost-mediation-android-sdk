/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
data class Placement(
    @SerialName("auto_refresh_rate")
    val autoRefreshRate: Int = 0,
    @SerialName("chartboost_placement")
    val chartboostPlacement: String,
    @Serializable(with = AdFormatEnumSetSerializer::class)
    @SerialName("format")
    val format: AdFormat,
    @SerialName("ad_queueing_enabled")
    val adQueueingEnabled: Boolean = true,
    @SerialName("queue_size")
    val queueSize: Int = 2,
)
