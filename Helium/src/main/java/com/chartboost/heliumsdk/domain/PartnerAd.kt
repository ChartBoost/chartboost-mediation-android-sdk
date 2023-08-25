/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.view.View

data class PartnerAd(
    val ad: Any?,
    val details: Map<String, String>,
    val request: PartnerAdLoadRequest
) {
    val inlineView: View? = ad as? View
}
