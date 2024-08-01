/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

/**
 * The contract for Chartboost Mediation errors.
 *
 * @property code The error code.
 * @property message The error message.
 * @property cause The error cause.
 * @property resolution The error resolution.
 * @property serverErrorName The name of the error to be sent to the server.
 */
interface ChartboostMediationErrorContract {
    val code: String
    val message: String
    val cause: String
    val resolution: String
    val serverErrorName: String
}
