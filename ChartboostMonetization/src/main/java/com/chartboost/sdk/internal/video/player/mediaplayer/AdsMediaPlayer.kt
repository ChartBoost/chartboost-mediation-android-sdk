package com.chartboost.sdk.internal.video.player.mediaplayer

import android.media.MediaPlayer
import android.media.MediaPlayer.MEDIA_ERROR_IO
import android.media.MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING
import android.media.MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING
import android.media.MediaPlayer.SEEK_CLOSEST
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.OpenForTesting
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.RandomAccessFileWrapper
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.player.AdsVideoPlayer
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.BackgroundListener
import com.chartboost.sdk.internal.video.player.handleAspectRatio
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressScheduler
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressSchedulerFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.IOException

private const val MEDIA_PLAYER_START_DELAY = 500L

internal class AdsMediaPlayer(
    private var mediaPlayer: MediaPlayer? = MediaPlayer(),
    private var surface: SurfaceView?,
    private var callback: AdsVideoPlayerListener?,
    private val uiPoster: UiPoster,
    videoProgressFactory: VideoProgressSchedulerFactory,
    private val videoBufferFactory: VideoBufferFactory,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val fileCache: FileCache,
) : AdsVideoPlayer,
    SurfaceHolder.Callback,
    VideoProgressScheduler.VideoProgress,
    VideoBuffer.VideoBufferListener,
    BackgroundListener {
    private var videoPosition = 0L
    private var isPrepared = false
    private var isStarted = false
    private var isPaused = false
    private var isBuffering = false
    private var isComingFromBackground = false
    private var surfaceHolder: SurfaceHolder? = surface?.holder
    private var randomAccessVideoFile: RandomAccessFileWrapper? = null
    private var buffer: VideoBuffer? = null
    private val videoProgressScheduler: VideoProgressScheduler = videoProgressFactory(callback, this, uiPoster)
    private var wasMediaStartedForTheFirstTime = false
    private var mediaPlayerVolume = 0f

    override fun currentPosition(): Long =
        mediaPlayer?.run {
            videoPosition = currentPosition.toLong()
            videoPosition
        } ?: 0

    override fun asset(asset: VideoAsset) {
        Logger.d("asset() - asset: $asset")
        mediaPlayer?.let {
            buffer =
                videoBufferFactory(
                    asset,
                    this,
                    coroutineDispatcher,
                    fileCache,
                )
            randomAccessVideoFile = buffer?.randomAccessVideoFile
            surfaceHolder?.addCallback(this)
        } ?: callback?.onVideoDisplayError("Missing media player during startMediaPlayer")
        wasMediaStartedForTheFirstTime = false
    }

    @VisibleForTesting
    fun onMediaPlayerError(
        what: Int,
        extra: Int,
    ) {
        val error = "error: $what extra: $extra"
        Logger.e("MediaPlayer error: $error")
        if (isPrepared) buffer()
    }

    private fun setDataSource() {
        try {
            randomAccessVideoFile?.fd?.let { mediaPlayer?.setDataSource(it) }
                ?: callback?.onVideoDisplayError("Missing video asset")
                ?: Logger.e("MediaPlayer missing callback on error")
        } catch (e: IOException) {
            // This is related to https://chartboost.atlassian.net/browse/MO-5737
            // Passing empty string in android 5 prevents the crash when file source is broken
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                mediaPlayer?.setDataSource("")
            }

            callback?.onVideoDisplayError(e.toString()) ?: Logger.e(
                "MediaPlayer missing callback on IOException",
                e,
            )
        }
    }

    override fun onComingFromBackground() {
        isComingFromBackground = true
    }

    override fun play() {
        Logger.d("play()")
        if (isPrepared && !isStarted) startMediaPlayerWithDelay()
        isStarted = true // will start video automatically once video player is ready
        isPaused = isComingFromBackground
        isComingFromBackground = false
    }

    override fun pause() {
        Logger.d("pause()")
        if (isPrepared && isStarted) {
            buffer?.stop()
            removeCurrentVideoProgress()
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                callback?.onVideoDisplayError(e.toString())
            }
            videoPosition = currentPosition()
            isStarted = false
            isPaused = true
        }
    }

    override fun stop() {
        Logger.d("stop()")
        if (isPrepared) {
            buffer?.stop()
            buffer = null
            videoPosition = 0
            removeCurrentVideoProgress()
            try {
                mediaPlayer?.stop()
            } catch (e: Exception) {
                callback?.onVideoDisplayError(e.toString())
            }
            isStarted = false
            isPaused = false
            randomAccessVideoFile?.close()
            randomAccessVideoFile = null
            release()
        }
    }

    override fun wasMediaStartedForTheFirstTime(): Boolean {
        return wasMediaStartedForTheFirstTime
    }

    override fun mute() {
        mediaPlayerVolume = 0f
        mediaPlayer?.setVolume(0f, 0f)
    }

    override fun unmute() {
        mediaPlayer?.setVolume(1f, 1f)
    }

    override fun volume(): Float {
        return mediaPlayerVolume
    }

    private fun release() {
        mediaPlayer?.release()
        callback = null
        mediaPlayer = null
        surfaceHolder = null
        surface = null
        buffer = null
    }

    /**
     * Activity needs to notify player that configuration has changed (screen rotation)
     */
    override fun onScreenOrientationChange(
        width: Int,
        height: Int,
    ) {
        // on rotation swap the values passed here form the view
        handleAspectRatio(height, width)
    }

    @OpenForTesting
    internal fun onMediaPlayerPrepared(mp: MediaPlayer) {
        isBuffering = false
        val videoDuration = mp.duration
        handleAspectRatio(surface?.width ?: 0, surface?.height ?: 0)
        callback?.onVideoDisplayPrepared(videoDuration.toLong())
        isPrepared = true
        buffer?.calculateBufferThreshold(videoDuration)
        if (isStarted) startMediaPlayer()
    }

    private fun startMediaPlayerWithDelay() {
        // give half second for initial buffer otherwise media player could throw an error
        uiPoster.invoke(MEDIA_PLAYER_START_DELAY, ::startMediaPlayer)
    }

    @OpenForTesting
    internal fun startMediaPlayer() {
        mediaPlayer?.run {
            try {
                start()
                wasMediaStartedForTheFirstTime = true
                // From media player documentation: When seeking to the given time position,
                // there is no guarantee that the data source has a frame located at the position.
                // When this happens, a frame nearby will be rendered. Meaning:
                // During buffer video might be rewind to last synced framed instead of the provided position
                scheduleCurrentVideoProgress()
                callback?.onVideoDisplayStarted()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    seekTo(videoPosition, SEEK_CLOSEST)
                } else {
                    // On Android below 8 the only seek mode is SEEK_PREVIOUS_SYNC which "moves media
                    // position to a sync (or key) frame associated with a data source that is
                    // located right before or at the given time."
                    // That doesn't guarantee media player will show the frame that was on the
                    // screen before buffer/click. Another issue associated with this is the video
                    // format itself. In certain conditions, media player won't be able to decode
                    // frames accurately hence it will rewind to the beginning
                    seekTo(videoPosition.toInt())
                }
            } catch (e: IllegalStateException) {
                // media player can throw illegal state exception
                callback?.onVideoDisplayError(e.toString())
            }
        } ?: callback?.onVideoDisplayError("Missing video player during startVideoPlayer")
    }

    /**
     * For an incomplete video file, media player needs to handle buffering
     */
    fun buffer() {
        if (isStarted && !isBuffering) {
            buffer?.buffer()
            isBuffering = false
            callback?.onVideoBufferStart()
            pause()
            buffer?.checkBufferDownload()
        }
    }

    /**
     * Enough video was downloaded to try to resume video display
     */
    override fun onVideoBuffered() {
        isStarted = true
        mediaPlayer?.reset()
        setDataSource()
        mediaPlayer?.prepareAsync()
        callback?.onVideoBufferFinish()
    }

    private fun handleAspectRatio(
        width: Int,
        height: Int,
    ) {
        mediaPlayer ?: return
        surface.handleAspectRatio(
            videoHeight = mediaPlayer?.videoHeight ?: 1,
            videoWidth = mediaPlayer?.videoWidth ?: 1,
            surfaceHeight = height,
            surfaceWidth = width,
        )
    }

    private fun scheduleCurrentVideoProgress() {
        videoProgressScheduler.startProgressUpdate()
    }

    private fun removeCurrentVideoProgress() {
        videoProgressScheduler.stopProgressUpdate()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (isPaused) {
            mediaPlayer?.setDisplay(holder)
            play()
        } else {
            try {
                setupMediaPlayer()
                setDataSource()
                mediaPlayer?.prepareAsync()
                mediaPlayer?.setDisplay(holder)
            } catch (e: Exception) {
                Logger.e("SurfaceCreated exception", e)
            }
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayer?.run {
            setOnPreparedListener(::onMediaPlayerPrepared)

            setOnInfoListener { _, what, extra ->
                // I/O error due to buffering of the file
                if ((what == MEDIA_INFO_VIDEO_NOT_PLAYING || what == MEDIA_INFO_AUDIO_NOT_PLAYING) && extra == MEDIA_ERROR_IO) {
                    buffer()
                }
                true
            }

            setOnCompletionListener {
                val durationFivePercent = it.duration * 0.05
                // in some cases we never reach the end of the video duration and we fall into infinite loop
                // to avoid that, I added margin to close the ad when we are close to the end
                val endOfVideo = it.duration - durationFivePercent
                if (videoPosition >= endOfVideo) {
                    callback?.onVideoDisplayCompleted()
                } else {
                    buffer()
                }
            }

            setOnErrorListener { _, what, extra ->
                onMediaPlayerError(what, extra)
                true
            }
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mediaPlayer?.setDisplay(null)
    }
}
