/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 *
 * Data class to hold the load ID and related data for requests to the `event/adload` endpoint.
 *
 * @property placementName The name of the placement that was loaded.
 * @property adType The ad type that was loaded.
 * @property loadId The ID String of the load that was performed.
 * @property status The status of the load.
 */
@Serializable
data class AdLoadNotificationRequestBody(
    @SerialName("placement_name")
    private val placementName: String,

    @SerialName("ad_type")
    private val adType: String,

    @SerialName("load_id")
    private val loadId: String,

    @SerialName("status")
    private val status: String
)
