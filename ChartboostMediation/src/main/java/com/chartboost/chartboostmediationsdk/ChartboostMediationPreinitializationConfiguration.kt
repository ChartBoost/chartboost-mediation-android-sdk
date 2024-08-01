/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

data class ChartboostMediationPreinitializationConfiguration(
    val skippedPartnerIds: Set<String> = setOf(),
)
