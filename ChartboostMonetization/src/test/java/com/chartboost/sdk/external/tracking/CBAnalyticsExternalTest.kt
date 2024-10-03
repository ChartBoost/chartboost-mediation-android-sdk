package com.chartboost.sdk.external.tracking

import com.chartboost.sdk.Analytics
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CBAnalyticsExternalTest : StringSpec({
    "Analytics Level types should be the correct ones" {
        mapOf(
            Analytics.LevelType.HIGHEST_LEVEL_REACHED.levelType to 1,
            Analytics.LevelType.CURRENT_AREA.levelType to 2,
            Analytics.LevelType.CHARACTER_LEVEL.levelType to 3,
            Analytics.LevelType.OTHER_SEQUENTIAL.levelType to 4,
            Analytics.LevelType.OTHER_NONSEQUENTIAL.levelType to 5,
        ).forEach { (value, expected) ->
            value shouldBe expected
        }
    }
})
