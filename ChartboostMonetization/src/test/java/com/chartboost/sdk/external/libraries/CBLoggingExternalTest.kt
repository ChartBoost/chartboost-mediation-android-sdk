package com.chartboost.sdk.external.libraries

import com.chartboost.sdk.LoggingLevel
import com.chartboost.sdk.internal.logging.Logger
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CBLoggingExternalTest : StringSpec({
    "Logger default logging level is integration" {
        Logger.level shouldBe LoggingLevel.INTEGRATION
    }
})
