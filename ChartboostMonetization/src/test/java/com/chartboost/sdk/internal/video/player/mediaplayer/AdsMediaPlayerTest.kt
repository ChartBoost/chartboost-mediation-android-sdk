package com.chartboost.sdk.internal.video.player.mediaplayer

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.utils.RandomAccessFileWrapper
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressScheduler
import com.chartboost.sdk.test.callPrivateMethod
import com.chartboost.sdk.test.justRunMockk
import com.chartboost.sdk.test.relaxedMockk
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Before
import org.junit.Test
import java.io.FileDescriptor

@ExperimentalCoroutinesApi
class AdsMediaPlayerTest {
    private val mediaPlayerMock: MediaPlayer = mockk(relaxed = true)
    private val surfaceMock: SurfaceView = mockk(relaxed = true)
    private val holderMock: SurfaceHolder = mockk(relaxed = true)
    private val callbackMock: AdsVideoPlayerListener = mockk(relaxed = true)
    private val uiHandlerMock: UiPoster = mockk(relaxed = true)
    private val videoProgressMock: VideoProgressScheduler = mockk()
    private val fileCacheMock: FileCache = mockk()
    private val videoBufferMock: VideoBuffer = relaxedMockk()
    private val videoProgressSchedulerMock: VideoProgressScheduler = justRunMockk()

    private val randomAccessFileWrapperMock: RandomAccessFileWrapper = mockk()

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    private lateinit var adsPlayer: AdsMediaPlayer

    @Before
    fun setup() {
        every { surfaceMock.holder } returns holderMock
        every { videoBufferMock.randomAccessVideoFile } returns randomAccessFileWrapperMock
        every { randomAccessFileWrapperMock.fd } returns mockk()
        justRun { randomAccessFileWrapperMock.close() }
        justRun { videoProgressMock.stopProgressUpdate() }
        justRun { videoProgressMock.startProgressUpdate(any()) }
        justRun { mediaPlayerMock.setDisplay(any()) }
        justRun { mediaPlayerMock.setDataSource(any<FileDescriptor>()) }
        adsPlayer =
            AdsMediaPlayer(
                mediaPlayer = mediaPlayerMock,
                surface = surfaceMock,
                callback = callbackMock,
                uiPoster = uiHandlerMock,
                videoProgressFactory = { _, _, _ -> videoProgressMock },
                coroutineDispatcher = testDispatcher,
                fileCache = fileCacheMock,
                videoBufferFactory = { _, _, _, _ -> videoBufferMock },
            )
    }

    @Test
    fun initTest() {
        every { holderMock.addCallback(any()) } answers {
            adsPlayer.surfaceCreated(holderMock)
        }

        every { surfaceMock.width } answers { 100 }
        every { surfaceMock.height } answers { 100 }
        every { mediaPlayerMock.videoWidth } answers { 100 }
        every { mediaPlayerMock.videoHeight } answers { 100 }

        every { mediaPlayerMock.prepareAsync() } answers {
            adsPlayer.onMediaPlayerPrepared(mediaPlayerMock)
        }

        adsPlayer.asset(mockk())

        verify(exactly = 1) { mediaPlayerMock.setOnPreparedListener(any()) }
        verify(exactly = 1) { mediaPlayerMock.setOnInfoListener(any()) }
        verify(exactly = 1) { mediaPlayerMock.setOnCompletionListener(any()) }
        verify(exactly = 1) { mediaPlayerMock.setOnErrorListener(any()) }

        verify(exactly = 1) { holderMock.addCallback(any()) }
        verify(exactly = 1) { mediaPlayerMock.setDisplay(holderMock) }
        verify(exactly = 1) { mediaPlayerMock.prepareAsync() }
        verify(exactly = 1) { callbackMock.onVideoDisplayPrepared(any()) }
    }

    @Test
    fun playTest() {
        val seekToCaptor = CapturingSlot<Int>()
        val runnableCaptor = CapturingSlot<() -> Unit>()
        val delayCaptor = CapturingSlot<Long>()
        initTest() // needs to configure before play
        every { uiHandlerMock(any(), any()) } answers {
            adsPlayer.startMediaPlayer()
        }
        every { videoProgressMock.startProgressUpdate() } just runs

        adsPlayer.play()
        verify(exactly = 1) { uiHandlerMock(capture(delayCaptor), capture(runnableCaptor)) }
        val runnable = runnableCaptor.captured
        runnable.shouldNotBeNull()
        val delay = delayCaptor.captured
        delay shouldBe 500

        verify(exactly = 1) { videoProgressMock.startProgressUpdate() }

        verify(exactly = 1) { mediaPlayerMock.start() }
        verify(exactly = 1) { mediaPlayerMock.seekTo(capture(seekToCaptor)) }
        verify(exactly = 1) { callbackMock.onVideoDisplayStarted() }
        val currentPosition = seekToCaptor.captured
        currentPosition shouldBe 0
    }

    @Test
    fun playWithoutInitTest() {
        adsPlayer.play()
        verify(exactly = 0) { uiHandlerMock(any(), any()) }
        verify(exactly = 0) { mediaPlayerMock.start() }
    }

    @Test
    fun pauseTest() {
        every { videoProgressMock.stopProgressUpdate() } just runs
        prepareMediaPlayer(1000L)
        adsPlayer.play()
        adsPlayer.pause()
        verify(exactly = 1) { mediaPlayerMock.currentPosition }
        verify(exactly = 1) { mediaPlayerMock.pause() }
    }

    @Test
    fun stopTest() {
        every { videoProgressMock.stopProgressUpdate() } just runs
        prepareMediaPlayer(1000L)
        adsPlayer.play()
        adsPlayer.stop()
        verify(exactly = 1) { mediaPlayerMock.stop() }
    }

    @Test
    fun muteTest() {
        val argumentLeft = CapturingSlot<Float>()
        val argumentRight = CapturingSlot<Float>()

        adsPlayer.mute()
        verify(exactly = 1) {
            mediaPlayerMock.setVolume(
                capture(argumentLeft),
                capture(argumentRight),
            )
        }
        val left = argumentLeft.captured
        val right = argumentRight.captured
        left shouldBe 0f
        right shouldBe 0f
    }

    @Test
    fun unmuteTest() {
        val argumentLeft = CapturingSlot<Float>()
        val argumentRight = CapturingSlot<Float>()

        adsPlayer.unmute()
        verify(exactly = 1) {
            mediaPlayerMock.setVolume(
                capture(argumentLeft),
                capture(argumentRight),
            )
        }
        val left = argumentLeft.captured
        val right = argumentRight.captured
        left shouldBe 1f
        right shouldBe 1f
    }

    @Test
    fun surfaceCreatedTest() {
        val holderCaptor = CapturingSlot<SurfaceHolder>()
        adsPlayer.surfaceCreated(holderMock)
        verify(exactly = 1) { mediaPlayerMock.setDisplay(capture(holderCaptor)) }
        val holder = holderCaptor.captured
        holder shouldBe holderMock
        verify(exactly = 1) { mediaPlayerMock.prepareAsync() }
    }

    @Test
    fun surfaceDestroyedTest() {
        val holderCaptor = CapturingSlot<SurfaceHolder>()
        adsPlayer.surfaceDestroyed(holderMock)
        verify(exactly = 1) { mediaPlayerMock.setDisplay(null) }
    }

    @Test
    fun bufferFileFullTest() {
        val fdCaptor = CapturingSlot<FileDescriptor>()
        every { uiHandlerMock(any(), any()) } answers {
            if (it.toString().contains("runCalculateBufferStatus")) {
                adsPlayer.callPrivateMethod<AdsMediaPlayer, Unit>("runCalculateBufferStatus")
                verify(exactly = 1) { mediaPlayerMock.reset() }
                verify(exactly = 2) { mediaPlayerMock.setDataSource(any<MediaDataSource>()) }
                verify(exactly = 2) { mediaPlayerMock.prepare() }
                verify(exactly = 1) { callbackMock.onVideoBufferFinish() }
                val fdArg = fdCaptor.captured
                fdArg.shouldNotBeNull()
            }
            true
        }
        prepareMediaPlayer(1000L)
        adsPlayer.play()
        adsPlayer.buffer()
        verify(exactly = 1) { mediaPlayerMock.pause() }
        verify(atLeast = 1) { uiHandlerMock(any(), any()) }
        verify(exactly = 1) { callbackMock.onVideoBufferStart() }
    }

    @Test
    fun bufferFileDownloadingTest() {
        val fdCaptor = CapturingSlot<FileDescriptor>()
        every { uiHandlerMock(any(), any()) } answers {
            if (it.toString().contains("runCalculateBufferStatus")) {
                adsPlayer.callPrivateMethod<AdsMediaPlayer, Unit>("calculateBufferStatus")
                verify(exactly = 1) { mediaPlayerMock.reset() }
                verify(exactly = 2) { mediaPlayerMock.setDataSource(any<MediaDataSource>()) }
                verify(exactly = 2) { mediaPlayerMock.prepare() }
                verify(exactly = 1) { callbackMock.onVideoBufferFinish() }
                val fdArg = fdCaptor.captured
                fdArg.shouldNotBeNull()
            }
            true
        }
        prepareMediaPlayer(100L)
        adsPlayer.play()
        adsPlayer.buffer()
        verify(exactly = 1) { mediaPlayerMock.pause() }
        verify(atLeast = 1) { uiHandlerMock(any(), any()) }
        verify(exactly = 1) { callbackMock.onVideoBufferStart() }
    }

    private fun prepareMediaPlayer(fileSize: Long) {
        every { randomAccessFileWrapperMock.length() } returns fileSize
        val fdCaptor = CapturingSlot<FileDescriptor>()
        every { mediaPlayerMock.prepareAsync() } answers {
            adsPlayer.onMediaPlayerPrepared(mediaPlayerMock)
        }

        val holderCaptor = CapturingSlot<SurfaceHolder>()
        adsPlayer.asset(mockk())
        adsPlayer.surfaceCreated(holderMock)

        verify(exactly = 1) { mediaPlayerMock.setDisplay(capture(holderCaptor)) }
        verify(exactly = 1) { mediaPlayerMock.setDataSource(capture(fdCaptor)) }
        val fdArg = fdCaptor.captured
        fdArg.shouldNotBeNull()
        val holder = holderCaptor.captured
        holder shouldBe holderMock
        verify(exactly = 1) { mediaPlayerMock.prepareAsync() }
    }

    @Test
    fun mediaPlayerIsNullTest() {
        adsPlayer =
            AdsMediaPlayer(
                mediaPlayer = null,
                surface = surfaceMock,
                callback = callbackMock,
                uiPoster = uiHandlerMock,
                fileCache = fileCacheMock,
                videoProgressFactory = { _, _, _ -> videoProgressSchedulerMock },
                videoBufferFactory = { _, _, _, _ -> videoBufferMock },
            )

        adsPlayer.asset(mockk())

        verify(exactly = 0) { mediaPlayerMock.setOnPreparedListener(any()) }
        verify(exactly = 0) { mediaPlayerMock.setOnInfoListener(any()) }
        verify(exactly = 0) { mediaPlayerMock.setOnCompletionListener(any()) }
        verify(exactly = 0) { mediaPlayerMock.setOnErrorListener(any()) }

        verify(exactly = 0) { holderMock.addCallback(any()) }
        verify(exactly = 0) { mediaPlayerMock.setDisplay(holderMock) }
        verify(exactly = 0) { mediaPlayerMock.prepareAsync() }
        verify(exactly = 0) { callbackMock.onVideoDisplayPrepared(any()) }

        adsPlayer.play()
        verify(exactly = 1) { callbackMock.onVideoDisplayError("Missing media player during startMediaPlayer") }
    }

    @Test
    fun getVideoCurrentPositionTest() {
        every { mediaPlayerMock.currentPosition } answers { 10 }
        val position = adsPlayer.currentPosition()
        verify(exactly = 1) { mediaPlayerMock.currentPosition }
        position.shouldNotBeNull()
        position shouldBe 10
    }

    @Test
    fun wasMediaStartedForTheFirstTimeFalseTest() {
        val wasStarted = adsPlayer.wasMediaStartedForTheFirstTime()
        wasStarted shouldBe false
    }

    @Test
    fun wasMediaStartedForTheFirstTimeTrueTest() {
        initTest() // needs to configure before play
        every { uiHandlerMock(any(), any()) } answers { adsPlayer.startMediaPlayer() }
        adsPlayer.play()
        val wasStarted = adsPlayer.wasMediaStartedForTheFirstTime()
        wasStarted shouldBe true
    }
}
