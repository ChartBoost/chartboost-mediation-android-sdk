/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.BuildConfig
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.core.ChartboostCore

/**
 * @suppress
 */
sealed class ChartboostMediationHeaderMap : HashMap<String, String?>() {
    data class ChartboostMediationAdLifecycleHeaderMap(
        val loadId: String?,
        val appSetId: String,
        val queueId: String? = null,
        val adType: String,
        val appId: String,
    ) : ChartboostMediationHeaderMap() {
        init {
            mapOf<String, String>().let {
                put(ChartboostMediationNetworking.APP_ID_HEADER_KEY, appId)
                put(ChartboostMediationNetworking.AD_TYPE_HEADER_KEY, adType)
                put(ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY, appSetId)
                put(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY, ChartboostCore.analyticsEnvironment.appSessionIdentifier ?: "")
                put(ChartboostMediationNetworking.SDK_VERSION_HEADER_KEY, ChartboostMediationSdk.getVersion())
                put(ChartboostMediationNetworking.DEVICE_OS_HEADER_KEY, ChartboostCore.analyticsEnvironment.osName)
                put(ChartboostMediationNetworking.DEVICE_OS_VERSION_HEADER_KEY, ChartboostCore.analyticsEnvironment.osVersion)
                put(ChartboostMediationNetworking.MEDIATION_VERSION_GIT_HASH_HEADER_KEY, BuildConfig.CHARTBOOST_MEDIATION_GIT_HASH)
                loadId?.let { put(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY, it) }
                queueId?.let { put(ChartboostMediationNetworking.QUEUE_ID_HEADER_KEY, it) }
                if (ChartboostMediationSdk.chartboostMediationInternal.testMode) {
                    put(
                        ChartboostMediationNetworking.DEBUG_HEADER_KEY,
                        ChartboostMediationSdk.chartboostMediationInternal.appId,
                    )
                }
            }
        }
    }

    data class ChartboostMediationAppConfigHeaderMap(
        val initHash: String,
        val appSetId: String,
        val appId: String,
    ) : ChartboostMediationHeaderMap() {
        init {
            mapOf(
                ChartboostMediationNetworking.APP_ID_HEADER_KEY to appId,
                ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY to appSetId,
                ChartboostMediationNetworking.SESSION_ID_HEADER_KEY to (ChartboostCore.analyticsEnvironment.appSessionIdentifier ?: ""),
                ChartboostMediationNetworking.SDK_VERSION_HEADER_KEY to ChartboostMediationSdk.getVersion(),
                ChartboostMediationNetworking.DEVICE_OS_HEADER_KEY to ChartboostCore.analyticsEnvironment.osName,
                ChartboostMediationNetworking.DEVICE_OS_VERSION_HEADER_KEY to ChartboostCore.analyticsEnvironment.osVersion,
                ChartboostMediationNetworking.INIT_HASH_HEADER_KEY to initHash,
            ).let {
                putAll(it)
                if (ChartboostMediationSdk.chartboostMediationInternal.testMode) {
                    put(
                        ChartboostMediationNetworking.DEBUG_HEADER_KEY,
                        ChartboostMediationSdk.chartboostMediationInternal.appId,
                    )
                }
            }
        }
    }

    data class ChartboostBidRequestMediationHeaderMap(
        val rateLimit: String,
        val loadId: String,
        val appSetId: String,
        val adType: String,
        val appId: String,
    ) : ChartboostMediationHeaderMap() {
        init {
            mapOf(
                ChartboostMediationNetworking.SESSION_ID_HEADER_KEY to (ChartboostCore.analyticsEnvironment.appSessionIdentifier ?: ""),
                ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY to appSetId,
                ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY to loadId,
                ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY to rateLimit,
                ChartboostMediationNetworking.AD_TYPE_HEADER_KEY to adType,
                ChartboostMediationNetworking.APP_ID_HEADER_KEY to appId,
                ChartboostMediationNetworking.SDK_VERSION_HEADER_KEY to ChartboostMediationSdk.getVersion(),
                ChartboostMediationNetworking.DEVICE_OS_HEADER_KEY to ChartboostCore.analyticsEnvironment.osName,
                ChartboostMediationNetworking.DEVICE_OS_VERSION_HEADER_KEY to ChartboostCore.analyticsEnvironment.osVersion,
            ).let {
                putAll(it)
                if (ChartboostMediationSdk.chartboostMediationInternal.testMode) {
                    put(
                        ChartboostMediationNetworking.DEBUG_HEADER_KEY,
                        ChartboostMediationSdk.chartboostMediationInternal.appId,
                    )
                }
            }
        }
    }

    data class ChartboostQueueRequestMediationHeaderMap(
        val queueId: String,
        val appSetId: String,
        val adType: String,
        val appId: String,
    ) : ChartboostMediationHeaderMap() {
        init {
            mapOf(
                ChartboostMediationNetworking.SESSION_ID_HEADER_KEY to (ChartboostCore.analyticsEnvironment.appSessionIdentifier ?: ""),
                ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY to appSetId,
                ChartboostMediationNetworking.QUEUE_ID_HEADER_KEY to queueId,
                ChartboostMediationNetworking.AD_TYPE_HEADER_KEY to adType,
                ChartboostMediationNetworking.APP_ID_HEADER_KEY to appId,
                ChartboostMediationNetworking.SDK_VERSION_HEADER_KEY to ChartboostMediationSdk.getVersion(),
                ChartboostMediationNetworking.DEVICE_OS_HEADER_KEY to ChartboostCore.analyticsEnvironment.osName,
                ChartboostMediationNetworking.DEVICE_OS_VERSION_HEADER_KEY to ChartboostCore.analyticsEnvironment.osVersion,
            ).let {
                putAll(it)
                if (ChartboostMediationSdk.chartboostMediationInternal.testMode) {
                    put(
                        ChartboostMediationNetworking.DEBUG_HEADER_KEY,
                        ChartboostMediationSdk.chartboostMediationInternal.appId,
                    )
                }
            }
        }
    }
}
