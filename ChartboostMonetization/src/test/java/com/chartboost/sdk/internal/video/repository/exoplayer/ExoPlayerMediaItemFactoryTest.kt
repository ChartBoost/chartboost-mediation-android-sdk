package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.internal.video.VideoAsset
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ExoPlayerMediaItemFactoryTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val downloadManagerMock: ExoPlayerDownloadManager = mockk()

    Given("a media item factory") {
        val factory = ExoPlayerMediaItemFactory(downloadManagerMock)

        And("a video asset") {
            val videoAsset: VideoAsset =
                mockk<VideoAsset>().apply {
                    every { filename } returns "filename"
                }

            And("download does not exist for this video asset") {
                every { downloadManagerMock.download(any()) } returns null

                When("creating a media item from the video asset") {
                    val mediaItem = factory.mediaItemFrom(videoAsset)

                    Then("the media item is null") {
                        mediaItem shouldBe null
                    }
                }
            }
        }
    }
})
