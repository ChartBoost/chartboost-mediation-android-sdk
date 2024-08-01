/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
data class QueueRequestBody(
    @SerialName("app_id")
    private val appId: String = ChartboostMediationSdk.getAppId() ?: "",
    @SerialName("placement_name")
    private val placement: String,
    @SerialName("actual_max_queue_size")
    private val actualMaxQueueSize: Int? = null,
    @SerialName("queue_capacity")
    private val queueCapacity: Int,
    @SerialName("current_queue_depth")
    private val currentQueueDepth: Int,
    @SerialName("queue_id")
    private val queueId: String,
)
