package com.chartboost.sdk.internal

import com.chartboost.sdk.ChartboostDSP.isDSP
import java.util.regex.Matcher
import java.util.regex.Pattern

object ChartboostDSPHelper {
    var dspCode: String = ""
    var dspCreatives: IntArray? = null

    fun setDspData(
        dspCode: String?,
        creativeTypes: IntArray?,
    ): Boolean {
        if (isDSP && dspCode != null && dspCode.length == 4 && creativeTypes != null && creativeTypes.isNotEmpty() && creativeTypes.size < 10) {
            // check for alphanumeric characters only capitalised
            val pattern: Pattern = Pattern.compile("^[A-Z0-9]+$")
            val matcher: Matcher = pattern.matcher(dspCode)
            if (matcher.matches()) {
                this.dspCode = dspCode
                this.dspCreatives = creativeTypes
                return true
            }
        }
        return false
    }
}
