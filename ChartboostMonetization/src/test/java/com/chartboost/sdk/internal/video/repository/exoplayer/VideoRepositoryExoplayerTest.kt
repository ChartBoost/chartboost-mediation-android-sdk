package com.chartboost.sdk.internal.video.repository.exoplayer

import com.chartboost.sdk.internal.video.AdUnitVideoPrecacheTemp
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.test.justRunMockk
import com.chartboost.sdk.test.mockAndroidLog
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VideoRepositoryExoplayerTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    mockAndroidLog()

    val policyMock: VideoCachePolicy =
        mockk<VideoCachePolicy>().apply {
            justRun { addDownloadToTimeWindow() }
        }

    val downloadManagerMock: ExoPlayerDownloadManager = justRunMockk()

    val fileName = "foo"
    val cachedFileMock: File =
        mockk<File>().apply {
            every { name } returns fileName
            every { parentFile } returns this
            every { setLastModified(any()) } returns true
        }

    val fileCacheMock: ExoPlayerFileCaching =
        mockk<ExoPlayerFileCaching>().apply {
            every { cachedFile(any()) } returns cachedFileMock
        }

    val fileCacheFactoryMock: FileCachingFactory =
        mockk<FileCachingFactory>().apply {
            every { this@apply.invoke(any()) } returns fileCacheMock
        }

    val testDispatcher = StandardTestDispatcher()

    Given("a VideoRepositoryExoplayer") {
        val videoRepositoryExoplayer =
            spyk(
                VideoRepositoryExoplayer(
                    policy = policyMock,
                    downloadManager = downloadManagerMock,
                    fileCachingFactory = fileCacheFactoryMock,
                    dispatcher = testDispatcher,
                ),
                recordPrivateCalls = true,
            )

        When("initialize() is called") {
            videoRepositoryExoplayer.initialize(mockk())

            Then("it should initialize the file caching") {
                verify { fileCacheFactoryMock(any()) }
            }

            Then("it should initialize the download manager") {
                verify {
                    with(downloadManagerMock) {
                        initialize()
                        addListener(videoRepositoryExoplayer)
                        cleanDownloads()
                    }
                }
            }

            Then("it should add listener to the download manager") {
                verify { downloadManagerMock.initialize() }
            }
        }

        And("it is initialized") {
            videoRepositoryExoplayer.initialize(mockk())

            And("showImmediately/forceDownload is false") {
                val showImmediately = false

                And("max count for time windows is not reached") {
                    every { policyMock.isMaxCountForTimeWindowReached() } returns false

                    When("downloadVideoFile() is called") {
                        videoRepositoryExoplayer.downloadVideoFile(
                            url = "url",
                            filename = fileName,
                            showImmediately = showImmediately,
                            callback = mockk(),
                        )

                        Then("it should queue the download") {
                            verify {
                                downloadManagerMock.addDownload(
                                    any(),
                                    DownloadStopReason.STOPPED_QUEUE,
                                )
                            }
                        }

                        Then("it should add the download to the time window") {
                            verify { policyMock.addDownloadToTimeWindow() }
                        }

                        Then("it should start the download") {
                            verify {
                                downloadManagerMock.addDownload(
                                    any(),
                                    DownloadStopReason.NONE,
                                )
                            }
                        }

                        And("fileName is null") {
                            When("startDownloadIfPossible() is called") {
                                videoRepositoryExoplayer.startDownloadIfPossible(
                                    filename = null,
                                    forceDownload = showImmediately,
                                )

                                Then("it should add the download to the time window") {
                                    verify { policyMock.addDownloadToTimeWindow() }
                                }

                                Then("it should queue next download") {
                                    verify {
                                        downloadManagerMock.startNextDownload(
                                            DownloadStopReason.NONE,
                                        )
                                    }
                                }
                            }
                        }

                        And("filename is not null") {
                            When("startDownloadIfPossible() is called") {
                                videoRepositoryExoplayer.startDownloadIfPossible(
                                    filename = fileName,
                                    forceDownload = showImmediately,
                                )

                                Then("it should add the download to the time window") {
                                    verify { policyMock.addDownloadToTimeWindow() }
                                }

                                Then("it should add download") {
                                    verify {
                                        downloadManagerMock.addDownload(any(), DownloadStopReason.NONE)
                                    }
                                }
                            }
                        }
                    }

                    And("fileName is null") {
                        When("startDownloadIfPossible() is called") {
                            videoRepositoryExoplayer.startDownloadIfPossible(
                                filename = null,
                                forceDownload = showImmediately,
                            )

                            Then("it should add the download to the time window") {
                                verify { policyMock.addDownloadToTimeWindow() }
                            }

                            Then("it should queue next download") {
                                verify {
                                    downloadManagerMock.startNextDownload(
                                        DownloadStopReason.NONE,
                                    )
                                }
                            }
                        }
                    }

                    And("filename is not null") {
                        When("startDownloadIfPossible() is called") {
                            videoRepositoryExoplayer.startDownloadIfPossible(
                                filename = fileName,
                                forceDownload = showImmediately,
                            )

                            Then("it should add the download to the time window") {
                                verify { policyMock.addDownloadToTimeWindow() }
                            }

                            Then("it should start next download") {
                                verify {
                                    downloadManagerMock.startNextDownload(any())
                                }
                            }
                        }
                    }
                }

                And("max count for time windows is reached") {
                    every { policyMock.isMaxCountForTimeWindowReached() } returns true

                    When("downloadVideoFile() is called") {
                        videoRepositoryExoplayer.downloadVideoFile(
                            url = "url",
                            filename = fileName,
                            showImmediately = showImmediately,
                            callback = mockk(),
                        )

                        Then("it should queue the download") {
                            verify {
                                downloadManagerMock.addDownload(
                                    any(),
                                    DownloadStopReason.STOPPED_QUEUE,
                                )
                            }
                        }

                        Then("it should not add the download to the time window") {
                            verify(exactly = 0) { policyMock.addDownloadToTimeWindow() }
                        }

                        Then("it should not start the download with time window as reason") {
                            verify {
                                downloadManagerMock.addDownload(
                                    any(),
                                    DownloadStopReason.MAX_COUNT_TIME_WINDOW,
                                )
                            }
                        }
                    }
                }
            }

            And("showImmediately/forceDownload is true") {
                val showImmediately = true

                And("max count for time windows is not reached") {
                    every { policyMock.isMaxCountForTimeWindowReached() } returns false

                    When("downloadVideoFile() is called") {
                        videoRepositoryExoplayer.downloadVideoFile(
                            url = "url",
                            filename = fileName,
                            showImmediately = showImmediately,
                            callback = mockk(),
                        )

                        Then("it should add the download to the time window") {
                            verify { policyMock.addDownloadToTimeWindow() }
                        }

                        Then("it should start the download") {
                            verify {
                                downloadManagerMock.startDownload(any())
                            }
                        }

                        And("fileName is null") {
                            When("startDownloadIfPossible() is called") {
                                videoRepositoryExoplayer.startDownloadIfPossible(
                                    filename = null,
                                    forceDownload = showImmediately,
                                )

                                Then("it should add the download to the time window") {
                                    verify { policyMock.addDownloadToTimeWindow() }
                                }

                                Then("it should queue next download") {
                                    verify {
                                        downloadManagerMock.startNextDownload(
                                            DownloadStopReason.NONE,
                                        )
                                    }
                                }
                            }
                        }

                        And("filename is not null") {
                            When("startDownloadIfPossible() is called") {
                                videoRepositoryExoplayer.startDownloadIfPossible(
                                    filename = fileName,
                                    forceDownload = showImmediately,
                                )

                                Then("it should add the download to the time window") {
                                    verify { policyMock.addDownloadToTimeWindow() }
                                }

                                Then("it should add download") {
                                    verify {
                                        downloadManagerMock.startDownload(any())
                                    }
                                }
                            }
                        }
                    }
                }

                And("max count for time windows is reached") {
                    every { policyMock.isMaxCountForTimeWindowReached() } returns true

                    When("downloadVideoFile() is called") {
                        videoRepositoryExoplayer.downloadVideoFile(
                            url = "url",
                            filename = fileName,
                            showImmediately = showImmediately,
                            callback = mockk(),
                        )

                        Then("it should add the download to the time window") {
                            verify { policyMock.addDownloadToTimeWindow() }
                        }

                        Then("it should start the download") {
                            verify {
                                downloadManagerMock.startDownload(any())
                            }
                        }
                    }
                }
            }

            When("isFileDownloadingOrDownloaded is called") {
                every { downloadManagerMock.isDownloadingOrDownloaded(any()) } returns true

                Then("it should return the same as the download manager") {
                    videoRepositoryExoplayer.isFileDownloadingOrDownloaded("url") shouldBe true
                }
            }

            And("video assets are empty") {

                When("getVideoAssets() is called") {
                    Then("it should return an empty list") {
                        videoRepositoryExoplayer.getVideoAsset("foo") shouldBe null
                    }
                }

                When("removeAsset() is called") {
                    And("asset is null") {
                        val result = videoRepositoryExoplayer.removeAsset(null)

                        Then("it should not remove anything") {
                            verify(exactly = 0) { downloadManagerMock.removeDownload(any()) }
                        }

                        Then("it should return false") {
                            result shouldBe false
                        }
                    }
                }

                When("tempFileIsReady is called") {
                    And("callback parameter is null") {
                        Then("it should not crash") {
                            shouldNotThrowAny {
                                videoRepositoryExoplayer.tempFileIsReady(
                                    url = "url",
                                    videoFileName = fileName,
                                    expectedContentSize = 0,
                                    adUnitVideoPrecacheTempCallback = null,
                                )
                            }
                        }
                    }

                    And("callback parameter is not null") {
                        val callback: AdUnitVideoPrecacheTemp =
                            mockk<AdUnitVideoPrecacheTemp>().apply {
                                justRun { tempVideoFileIsReady(any()) }
                            }

                        videoRepositoryExoplayer.tempFileIsReady(
                            url = "url",
                            videoFileName = fileName,
                            expectedContentSize = 0,
                            adUnitVideoPrecacheTempCallback = callback,
                        )

                        Then("it should call the callback") {
                            verify { callback.tempVideoFileIsReady("url") }
                        }
                    }
                }
            }

            And("video assets are not empty") {
                every { policyMock.isMaxCountForTimeWindowReached() } returns false
                val callback: AdUnitVideoPrecacheTemp =
                    mockk<AdUnitVideoPrecacheTemp>().apply {
                        justRun { tempVideoFileIsReady(any()) }
                    }
                videoRepositoryExoplayer.downloadVideoFile(
                    url = "url",
                    filename = fileName,
                    showImmediately = false,
                    callback = callback,
                )

                When("getVideoAssets() is called") {
                    Then("it should return corresponding video asset") {
                        videoRepositoryExoplayer.getVideoAsset("foo")?.filename shouldBe fileName
                    }
                }

                When("removeAsset() is called") {
                    val asset: VideoAsset =
                        mockk<VideoAsset>().apply {
                            every { filename } returns fileName
                        }
                    videoRepositoryExoplayer.removeAsset(asset)

                    Then("it should remove the download") {
                        verify { downloadManagerMock.removeDownload(fileName) }
                    }

                    Then("it should remove the asset from the cache") {
                        videoRepositoryExoplayer.getVideoAsset(fileName) shouldBe null
                    }
                }

                When("tempFileIsReady is called") {
                    And("callback parameter is null") {
                        videoRepositoryExoplayer.tempFileIsReady(
                            url = "url",
                            videoFileName = fileName,
                            expectedContentSize = 0,
                            adUnitVideoPrecacheTempCallback = null,
                        )
                        Then("it should call the callback") {
                            verify { callback.tempVideoFileIsReady("url") }
                        }
                    }

                    And("callback parameter is not null") {
                        val newCallback: AdUnitVideoPrecacheTemp =
                            mockk<AdUnitVideoPrecacheTemp>().apply {
                                justRun { tempVideoFileIsReady(any()) }
                            }

                        videoRepositoryExoplayer.tempFileIsReady(
                            url = "url",
                            videoFileName = fileName,
                            expectedContentSize = 0,
                            adUnitVideoPrecacheTempCallback = newCallback,
                        )

                        Then("it should call the parameter callback") {
                            verify { newCallback.tempVideoFileIsReady("url") }
                        }

                        Then("it should not call the cached callback") {
                            verify(exactly = 0) { callback.tempVideoFileIsReady(any()) }
                        }
                    }
                }
            }

            When("getVideoDownloadState() is called") {
                And("asset is null") {
                    Then("it should return empty state") {
                        videoRepositoryExoplayer.getVideoDownloadState(null) shouldBe VideoRepository.VIDEO_STATE_EMPTY
                    }
                }

                And("asset is not null") {
                    val asset: VideoAsset =
                        mockk<VideoAsset>().apply {
                            every { filename } returns fileName
                        }
                    Then("it should return state from download manager") {
                        every { downloadManagerMock.downloadPercentage(fileName) } returns 0.65f
                        videoRepositoryExoplayer.getVideoDownloadState(asset) shouldBe
                            VideoRepository.VIDEO_STATE_QUARTILE_3
                    }
                }
            }

            When("onSuccess() is called") {
                justRun { videoRepositoryExoplayer.startDownloadIfPossible() }
                videoRepositoryExoplayer.onSuccess("url", "foo")

                Then("it should start next download if any") {
                    verify { videoRepositoryExoplayer.startDownloadIfPossible() }
                }
            }
        }
    }
})
