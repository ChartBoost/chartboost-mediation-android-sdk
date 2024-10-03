package com.chartboost.sdk.internal.video.player.exoplayer

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.handleAspectRatio
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressScheduler
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerFactory
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerMediaItemFactory
import com.chartboost.sdk.test.justRunMockk
import com.chartboost.sdk.test.mockAndroidLog
import com.chartboost.sdk.test.relaxedMockk
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify

class AdsExoPlayerTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    mockAndroidLog()

    mockkStatic("com.chartboost.sdk.internal.video.player.VideoUtilsKt")
    justRun { any<SurfaceView>().handleAspectRatio(any(), any(), any(), any()) }

    mockkStatic("com.chartboost.sdk.internal.video.player.exoplayer.ExoPlayerUtilsKt")
    every { any<ExoPlayer>().width() } returns 100
    every { any<ExoPlayer>().height() } returns 100

    val contextMock: Context = mockk()

    val exoPlayerMock: ExoPlayer =
        justRunMockk<ExoPlayer>().apply {
            every { currentPosition } returns 100L
            justRun { volume = any() }
            every { duration } returns 1000L
        }

    val exoPlayerFactoryMock: ExoPlayerFactory =
        mockk<ExoPlayerFactory>().apply {
            every { this@apply.invoke() } returns exoPlayerMock
        }

    val exoPlayerMediaItemFactoryMock: ExoPlayerMediaItemFactory =
        mockk<ExoPlayerMediaItemFactory>().apply {
            every { mediaItemFrom(any()) } returns mockk()
        }

    val surfaceHolderMock: SurfaceHolder = relaxedMockk()

    val surfaceViewMock: SurfaceView =
        mockk<SurfaceView>().apply {
            every { holder } returns surfaceHolderMock
            every { width } returns 100
            every { height } returns 100
        }

    val adsVideoPlayerListenerMock: AdsVideoPlayerListener = relaxedMockk()

    val handlerMock: UiPoster = mockk(relaxed = true)

    val videoProgressSchedulerMock: VideoProgressScheduler = relaxedMockk()

    Given("An AdsExoPlayer instance") {
        val adsExoPlayer =
            spyk(
                AdsExoPlayer(
                    context = contextMock,
                    exoPlayerFactory = exoPlayerFactoryMock,
                    exoPlayerMediaItemFactory = exoPlayerMediaItemFactoryMock,
                    surfaceView = surfaceViewMock,
                    callback = adsVideoPlayerListenerMock,
                    uiPoster = handlerMock,
                    videoProgressFactory = { _, _, _ -> videoProgressSchedulerMock },
                ),
            )

        When("currentPosition() is called") {
            val position = adsExoPlayer.currentPosition()

            Then("it should return the current position of the exoPlayer") {
                position shouldBe 100L
            }

            Then("it should get the position from ExoPlayer instance") {
                verify { exoPlayerMock.currentPosition }
            }
        }

        When("asset() is called") {
            adsExoPlayer.asset(mockk())

            Then("it should add the media item to the exoPlayer") {
                verify { exoPlayerMock.addMediaItem(any()) }
            }

            Then("it should prepare the exoPlayer") {
                verify { exoPlayerMock.prepare() }
            }

            Then("it should add a callback to the surfaceView") {
                verify { surfaceHolderMock.addCallback(adsExoPlayer) }
            }
        }

        When("play() is called") {
            adsExoPlayer.play()

            Then("it should set the surface view on the exoPlayer") {
                verify { exoPlayerMock.setVideoSurfaceView(surfaceViewMock) }
            }

            Then("it should call play on the exoPlayer") {
                verify { exoPlayerMock.play() }
            }
        }

        When("pause() is called") {
            adsExoPlayer.pause()

            Then("it should call pause on the exoPlayer") {
                verify { exoPlayerMock.pause() }
            }
        }

        When("stop() is called") {
            And("not playing") {
                every { exoPlayerMock.isPlaying } returns false
                adsExoPlayer.stop()

                Then("it should not call stop on the exoPlayer") {
                    verify(exactly = 0) { exoPlayerMock.stop() }
                }

                Then("it should release the exoPlayer") {
                    verify { exoPlayerMock.release() }
                }
            }

            And("playing") {
                every { exoPlayerMock.isPlaying } returns true
                adsExoPlayer.stop()

                Then("it should call stop on the exoPlayer") {
                    verify { exoPlayerMock.stop() }
                }

                Then("it should release the exoPlayer") {
                    verify { exoPlayerMock.release() }
                }
            }
        }

        When("mute() is called") {
            adsExoPlayer.mute()

            Then("it should mute on the exoPlayer") {
                verify { exoPlayerMock.volume = 0f }
            }
        }

        When("unmute() is called") {
            adsExoPlayer.unmute()

            Then("it should unmute on the exoPlayer") {
                verify { exoPlayerMock.volume = 1f }
            }
        }

        And("not coming from background") {
            When("surfaceCreated() is called") {
                adsExoPlayer.surfaceCreated(surfaceHolderMock)

                Then("it should not start playing") {
                    verify(exactly = 0) { exoPlayerMock.play() }
                }
            }
        }

        And("coming from background") {
            adsExoPlayer.onComingFromBackground()

            When("surfaceCreated() is called") {
                adsExoPlayer.surfaceCreated(surfaceHolderMock)

                Then("it should start playing") {
                    verify { exoPlayerMock.play() }
                }
            }
        }

        When("onPlaybackStateChanged() is called with state = STATE_READY") {
            adsExoPlayer.onPlaybackStateChanged(Player.STATE_READY)

            Then("it should call onVideoDisplayPrepared on the adsVideoPlayerListener") {
                verify { adsVideoPlayerListenerMock.onVideoDisplayPrepared(any()) }
            }
        }

        When("onPlaybackStateChanged() is called with state = STATE_ENDED") {
            every { exoPlayerMock.isPlaying } returns false
            adsExoPlayer.onPlaybackStateChanged(Player.STATE_ENDED)

            Then("it should stop playing") {
                verify { adsExoPlayer.stop() }
            }

            Then("it should call onVideoDisplayEnded on the adsVideoPlayerListener") {
                verify { adsVideoPlayerListenerMock.onVideoDisplayCompleted() }
            }

            Then("it should stop progress scheduler") {
                verify { videoProgressSchedulerMock.stopProgressUpdate() }
            }
        }

        When("onPlayerError() is called") {
            val error: PlaybackException =
                mockk<PlaybackException>().apply {
                    every { message } returns "error"
                }
            every { exoPlayerMock.isPlaying } returns false
            adsExoPlayer.onPlayerError(error)

            Then("it should stop playing") {
                verify { adsExoPlayer.stop() }
            }

            Then("it should call onVideoDisplayError on the adsVideoPlayerListener") {
                verify { adsVideoPlayerListenerMock.onVideoDisplayError(any()) }
            }
        }

        When("surfaceChanged() is called") {
            adsExoPlayer.surfaceChanged(surfaceHolderMock, 0, 100, 100)

            Then("it should not call handleAspectRatio on the surfaceView") {
                verify(exactly = 0) { surfaceViewMock.handleAspectRatio(any(), any(), 100, 100) }
            }
        }

        When("onIsPlayingChanged() is called") {
            And("isPlaying = true") {
                adsExoPlayer.onIsPlayingChanged(true)

                Then("it should start the progress update") {
                    verify { videoProgressSchedulerMock.startProgressUpdate() }
                }

                Then("it should call onVideoDisplayStarted") {
                    verify { adsVideoPlayerListenerMock.onVideoDisplayStarted() }
                }
            }

            And("isPlaying = false") {
                adsExoPlayer.onIsPlayingChanged(false)

                Then("it should stop the progress update") {
                    verify { videoProgressSchedulerMock.stopProgressUpdate() }
                }
            }
        }
    }
})
