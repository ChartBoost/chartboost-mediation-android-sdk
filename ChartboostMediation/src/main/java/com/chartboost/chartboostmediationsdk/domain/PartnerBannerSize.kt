/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.util.Size

/**
 * The Chartboost Mediation partner banner size.
 *
 * @property size The indicated size of the partner banner ad.
 * @property type The type of the partner banner, either [BannerTypes.BANNER] or [BannerTypes.ADAPTIVE_BANNER].
 */
class PartnerBannerSize(
    val size: Size,
    val type: BannerType,
)

typealias BannerType = String

object BannerTypes {
    val BANNER: BannerType = AdFormat.BANNER.key
    val ADAPTIVE_BANNER: BannerType = AdFormat.ADAPTIVE_BANNER.key
}
