package com.chartboost.sdk.internal.video.repository.exoplayer

import com.google.android.exoplayer2.offline.DownloadCursor
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UtilsTest : ShouldSpec({

    context("DownloadCursor") {
        val downloadCursor: DownloadCursor = mockk()

        should("return empty list when cursor is empty") {
            every { downloadCursor.moveToNext() } returns false
            downloadCursor.asList() shouldBe emptyList()
        }
    }
})
