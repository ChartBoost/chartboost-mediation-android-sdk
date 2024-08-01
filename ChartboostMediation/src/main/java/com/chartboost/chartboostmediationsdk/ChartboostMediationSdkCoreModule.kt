/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.content.Context
import com.chartboost.core.initialization.Module
import com.chartboost.core.initialization.ModuleConfiguration
import org.json.JSONObject

/**
 * The Chartboost Mediation Sdk Module that ChartboostCore initializes.
 */
internal class ChartboostMediationSdkCoreModule : Module {
    override val moduleId: String = "chartboost_mediation"
    override val moduleVersion: String = BuildConfig.CHARTBOOST_MEDIATION_VERSION

    override fun updateCredentials(
        context: Context,
        credentials: JSONObject,
    ) {
    }

    override suspend fun initialize(
        context: Context,
        moduleConfiguration: ModuleConfiguration,
    ): Result<Unit> {
        val appId = moduleConfiguration.chartboostApplicationIdentifier
        return ChartboostMediationSdk.initialize(
            context = context,
            appId = appId,
        )
    }
}
