package com.chartboost.sdk.test

import com.chartboost.sdk.ChartboostDSP
import io.mockk.every
import io.mockk.mockkObject

fun mockChartboostDSP(isDsp: Boolean) {
    mockkObject(ChartboostDSP)
    every { ChartboostDSP.isDSP } returns isDsp
}
