package com.chartboost.sdk

object ChartboostDSP {
    val isDSP = false

    @JvmStatic
    fun setDSPHeader(
        dspCode: String?,
        creativeTypes: IntArray?,
    ): Boolean = false
}
