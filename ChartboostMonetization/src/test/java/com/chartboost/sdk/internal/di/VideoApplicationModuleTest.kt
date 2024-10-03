package com.chartboost.sdk.internal.di

import android.content.Context
import android.view.SurfaceView
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Model.VideoPreCachingModel
import com.chartboost.sdk.internal.video.player.exoplayer.AdsExoPlayer
import com.chartboost.sdk.internal.video.player.mediaplayer.AdsMediaPlayer
import com.chartboost.sdk.internal.video.repository.exoplayer.VideoRepositoryExoplayer
import com.chartboost.sdk.internal.video.repository.mediaplayer.VideoRepositoryMediaPlayer
import com.chartboost.sdk.test.FakeUiPoster
import com.chartboost.sdk.test.mockAndroidLog
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import java.io.File

class VideoApplicationModuleTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    mockAndroidLog()

    val contextMock: Context =
        mockk<Context> {
            every { applicationContext } returns this
            every { cacheDir } returns File("cacheDir")
        }

    val androidComponentMock: AndroidComponent =
        mockk<AndroidComponent> {
            every { context } returns contextMock
            every { uiPoster } returns mockk<FakeUiPoster>()
        }

    val applicationComponentMock: ApplicationComponent =
        mockk<ApplicationComponent> {
            every { videoCachePolicy } returns mockk()
            every { exoPlayerDownloadManager } returns mockk()
        }

    val executorComponentMock: ExecutorComponent =
        mockk<ExecutorComponent> {
            every { backgroundExecutor } returns mockk()
            every { networkExecutor } returns mockk()
        }

    val privacyComponentMock: PrivacyComponent = mockk()
    val sdkConfigMock: SdkConfiguration = mockk()

    val trackerComponentMock =
        mockk<TrackerComponent> {
            every { eventTracker } returns mockk()
        }

    mockkObject(ChartboostDependencyContainer)
    every { ChartboostDependencyContainer.androidComponent } returns androidComponentMock
    every { ChartboostDependencyContainer.applicationComponent } returns applicationComponentMock

    Given("An ApplicationModule instance") {
        val sut =
            ApplicationModule(
                androidComponent = androidComponentMock,
                executorComponent = executorComponentMock,
                privacyComponent = privacyComponentMock,
                sdkConfigFactory = { sdkConfigMock },
                trackerComponent = trackerComponentMock,
            )

        And("video player configuration is exoplayer") {
            every { sdkConfigMock.precacheConfig } returns
                mockk<VideoPreCachingModel>().apply {
                    every { videoPlayer } returns VideoPreCachingModel.VideoPlayerType.EXO_PLAYER
                }

            When("getting video repository") {
                val videoRepository = sut.videoRepository

                Then("video repository should be exoplayer") {
                    videoRepository.shouldBeTypeOf<VideoRepositoryExoplayer>()
                }
            }

            When("getting video player") {
                val videoPlayer =
                    sut.adsVideoPlayerFactory.invoke(
                        contextMock,
                        mockk(),
                        mockk(),
                        mockk(),
                        mockk(),
                    )

                Then("video player should be exoplayer") {
                    videoPlayer.shouldBeTypeOf<AdsExoPlayer>()
                }
            }
        }

        And("video player configuration is mediaplayer") {
            every { sdkConfigMock.precacheConfig } returns
                mockk<VideoPreCachingModel>().apply {
                    every { videoPlayer } returns VideoPreCachingModel.VideoPlayerType.MEDIA_PLAYER
                }

            When("getting video repository") {
                val videoRepository = sut.videoRepository

                Then("video repository should be media player") {
                    videoRepository.shouldBeTypeOf<VideoRepositoryMediaPlayer>()
                }
            }

            When("getting video player") {
                val videoPlayer =
                    sut.adsVideoPlayerFactory.invoke(
                        contextMock,
                        mockk<SurfaceView>().apply {
                            every { holder } returns mockk()
                        },
                        mockk(),
                        mockk(),
                        mockk(),
                    )

                Then("video player should be media player") {
                    videoPlayer.shouldBeTypeOf<AdsMediaPlayer>()
                }
            }
        }
    }
})
