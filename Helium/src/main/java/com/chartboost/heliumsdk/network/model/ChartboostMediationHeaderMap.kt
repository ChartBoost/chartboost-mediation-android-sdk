/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking
import com.chartboost.heliumsdk.utils.Environment

/**
 * @suppress
 */
sealed class ChartboostMediationHeaderMap : HashMap<String, String?>() {

    data class ChartboostMediationAdLifecycleHeaderMap(val loadId: String?) :
    ChartboostMediationHeaderMap() {
        init {
            mapOf<String, String>().let {
                put(ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY, (Environment.appSetId ?: ""))
                put(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY, Environment.sessionId)
                loadId?.let { put(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY, it) }
            }
        }
    }

    data class ChartboostMediationAppConfigHeaderMap(
        val initHash: String
    ) :
        ChartboostMediationHeaderMap() {
        init {
            mapOf(
                ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY to (Environment.appSetId ?: ""),
                ChartboostMediationNetworking.SESSION_ID_HEADER_KEY to Environment.sessionId,
                ChartboostMediationNetworking.SDK_VERSION_HEADER_KEY to HeliumSdk.getVersion(),
                ChartboostMediationNetworking.DEVICE_OS_HEADER_KEY to Environment.operatingSystem,
                ChartboostMediationNetworking.DEVICE_OS_VERSION_HEADER_KEY to Environment.operatingSystemVersion,
                ChartboostMediationNetworking.INIT_HASH_HEADER_KEY to initHash
            ).let { putAll(it) }
        }
    }

    data class ChartboostBidRequestMediationHeaderMap(
        val rateLimit: String,
        val loadId: String
    ) :
        ChartboostMediationHeaderMap() {
        init {
            mapOf(
                "X-Helium-SessionID" to Environment.sessionId,
                ChartboostMediationNetworking.APP_SET_ID_HEADER_KEY to (Environment.appSetId ?: ""),
                ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY to loadId,
                ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY to rateLimit
            ).let { putAll(it) }
        }
    }
}
