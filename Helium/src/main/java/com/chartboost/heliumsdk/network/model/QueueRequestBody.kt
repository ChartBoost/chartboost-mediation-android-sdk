/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.HeliumSdk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
data class QueueRequestBody(
    @SerialName("app_id")
    private val appId: String = HeliumSdk.getAppId() ?: "",
    @SerialName("placement_name")
    private val placementName: String,
    @SerialName("actual_max_queue_size")
    private val actualMaxQueueSize: Int? = null,
    @SerialName("queue_capacity")
    private val queueCapacity: Int,
    @SerialName("current_queue_depth")
    private val currentQueueDepth: Int,
    @SerialName("queue_id")
    private val queueId: String,
)
