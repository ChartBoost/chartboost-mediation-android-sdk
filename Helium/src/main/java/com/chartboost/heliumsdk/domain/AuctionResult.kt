/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

/**
 * @suppress
 */
data class AuctionResult(
    val bids: Bids,
    val headers: Map<String, List<String>>,
    val chartboostMediationError: ChartboostMediationError? = null
)
