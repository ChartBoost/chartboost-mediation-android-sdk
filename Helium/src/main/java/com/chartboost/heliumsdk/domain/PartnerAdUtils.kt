/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.util.Size

object PartnerAdUtils {
    fun getCreativeSizeFromPartnerAdDetails(
        partnerAd: PartnerAd,
        requestedSize: Size,
    ): Size {
        return partnerAd.run {
            Size(
                details["banner_width_dips"]?.toInt() ?: requestedSize.width,
                details["banner_height_dips"]?.toInt() ?: requestedSize.height,
            )
        }
    }
}
