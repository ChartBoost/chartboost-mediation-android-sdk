/*
 * Copyright 2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import kotlinx.serialization.Serializable

@Serializable
data class ServerEventTracker(val url: String = "")
