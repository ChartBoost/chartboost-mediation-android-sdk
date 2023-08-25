/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
data class RewardedCallbackData(
    @SerialName(REWARDED_CALLBACK_URL_FIELD_NAME)
    val url: String,
    @SerialName(REWARDED_CALLBACK_METHOD_FIELD_NAME)
    val method: String,
    @SerialName(REWARDED_CALLBACK_MAX_RETRIES_FIELD_NAME)
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    @SerialName(REWARDED_CALLBACK_BODY_FIELD_NAME)
    val body: String
) {
    companion object {
        const val DEFAULT_MAX_RETRIES = 2

        private const val REWARDED_CALLBACK_URL_FIELD_NAME = "url"
        private const val REWARDED_CALLBACK_METHOD_FIELD_NAME = "method"
        private const val REWARDED_CALLBACK_MAX_RETRIES_FIELD_NAME = "max_retries"
        private const val REWARDED_CALLBACK_BODY_FIELD_NAME = "body"
    }
}
