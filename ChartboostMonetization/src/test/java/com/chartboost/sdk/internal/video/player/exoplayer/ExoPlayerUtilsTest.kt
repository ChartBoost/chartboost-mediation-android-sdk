package com.chartboost.sdk.internal.video.player.exoplayer

import com.google.android.exoplayer2.ExoPlayer
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

class ExoPlayerUtilsTest : ShouldSpec({

    mockkStatic("com.chartboost.sdk.internal.video.player.exoplayer.ExoPlayerUtilsKt")

    val exoPlayerMock: ExoPlayer = mockk()

    // Cannot mock ExoPlayer's Format class because it is final

    should("return 1 for width if the video format is null") {
        every { exoPlayerMock.videoFormat } returns null
        exoPlayerMock.width() shouldBe 1
    }

    should("return 1 for height if the video format is null") {
        every { exoPlayerMock.videoFormat } returns null
        exoPlayerMock.height() shouldBe 1
    }
})
