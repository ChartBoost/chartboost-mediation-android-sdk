package com.chartboost.sdk.internal.video.player.mediaplayer

import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.utils.RandomAccessFileWrapper
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.test.advanceTimeBy
import com.chartboost.sdk.test.advanceUntilIdle
import com.chartboost.sdk.test.justRunMockk
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher

@ExperimentalCoroutinesApi
class VideoBufferTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    val randomAccessVideoFileMock: RandomAccessFileWrapper = mockk()
    val listenerMock: VideoBuffer.VideoBufferListener = justRunMockk()
    val videoAssetMock: VideoAsset = mockk()
    val fileCacheMock: FileCache = mockk()
    val testCoroutineDispatcher: TestDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    val expectedVideoSize = 200L
    every { videoAssetMock.expectedFileSize } returns expectedVideoSize

    Given("A VideoBuffer instance") {
        val videoBuffer =
            spyk(
                VideoBuffer(
                    videoAsset = videoAssetMock,
                    listener = listenerMock,
                    coroutineDispatcher = testCoroutineDispatcher,
                    fileCache = fileCacheMock,
                    randomAccessFileFactory = { _, _, _ -> randomAccessVideoFileMock },
                ),
                recordPrivateCalls = true,
            )

        When("Calling checkBufferDownload() while mocking calculateBufferStatus()") {
            every { videoBuffer["calculateBufferStatus"]() } returns Unit
            videoBuffer.checkBufferDownload()

            Then("It should call calculateBufferStatus() after the buffer waiting time") {
                testCoroutineDispatcher.advanceUntilIdle()
                verify { videoBuffer["calculateBufferStatus"]() }
            }
        }

        And("Video file is fully downloaded") {
            every { randomAccessVideoFileMock.length() } returns expectedVideoSize

            When("Call checkBufferDownload()") {
                videoBuffer.checkBufferDownload()

                Then("It should call stop() and inform callback") {
                    testCoroutineDispatcher.advanceUntilIdle()
                    verify {
                        videoBuffer.stop()
                        listenerMock.onVideoBuffered()
                    }
                }
            }
        }

        And("Downloaded percentage is above threshold") {
            every { randomAccessVideoFileMock.length() } returns 0
            videoBuffer.buffer()

            When("Call checkBufferDownload()") {
                val fileSize: Long = (expectedVideoSize * DEFAULT_BUFFER_UNLOCK_THRESHOLD).toLong() + 10
                every { randomAccessVideoFileMock.length() } returns fileSize
                videoBuffer.checkBufferDownload()

                Then("It should call stop() and inform callback") {
                    testCoroutineDispatcher.advanceTimeBy(BUFFER_WAIT_TIME + 100)
                    verify {
                        videoBuffer.stop()
                        listenerMock.onVideoBuffered()
                    }
                }
            }
        }

        And("Downloaded percentage is below threshold") {
            every { randomAccessVideoFileMock.length() } returns 0
            videoBuffer.buffer()

            When("Call checkBufferDownload()") {
                val fileSize: Long = (expectedVideoSize * DEFAULT_BUFFER_UNLOCK_THRESHOLD).toLong() - 5
                every { randomAccessVideoFileMock.length() } returns fileSize
                videoBuffer.checkBufferDownload()

                Then("It should call checkBufferDownload() again") {
                    testCoroutineDispatcher.advanceTimeBy(BUFFER_WAIT_TIME + 100)
                    verify(exactly = 2) {
                        videoBuffer.checkBufferDownload()
                    }
                }
            }
        }
    }
})
