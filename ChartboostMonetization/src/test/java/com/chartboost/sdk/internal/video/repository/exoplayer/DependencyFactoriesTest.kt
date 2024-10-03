package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class DependencyFactoriesTest : ShouldSpec({

    val contextMock =
        mockk<Context>().apply {
            every { cacheDir } returns File("cache")
        }

    should("return correct precache directory") {
        contextMock.preCacheDirectory().path shouldBe "cache/.chartboost/precache"
    }

    should("return correct precache queue directory") {
        contextMock.preCacheQueueDirectory().path shouldBe "cache/.chartboost/precache_queue"
    }
})
