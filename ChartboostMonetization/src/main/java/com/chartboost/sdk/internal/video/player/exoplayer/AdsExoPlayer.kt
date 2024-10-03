package com.chartboost.sdk.internal.video.player.exoplayer

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.video.VideoAsset
import com.chartboost.sdk.internal.video.player.AdsVideoPlayer
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.BackgroundListener
import com.chartboost.sdk.internal.video.player.handleAspectRatio
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressScheduler
import com.chartboost.sdk.internal.video.player.scheduler.VideoProgressSchedulerFactory
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerFactory
import com.chartboost.sdk.internal.video.repository.exoplayer.ExoPlayerMediaItemFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

internal class AdsExoPlayer(
    context: Context,
    exoPlayerFactory: ExoPlayerFactory = ExoPlayerFactory(context),
    private val exoPlayerMediaItemFactory: ExoPlayerMediaItemFactory,
    private val surfaceView: SurfaceView,
    private val callback: AdsVideoPlayerListener? = null,
    uiPoster: UiPoster,
    videoProgressFactory: VideoProgressSchedulerFactory,
) : AdsVideoPlayer,
    SurfaceHolder.Callback,
    Player.Listener,
    VideoProgressScheduler.VideoProgress,
    BackgroundListener {
    private val exoPlayer: ExoPlayer by lazy {
        exoPlayerFactory().apply {
            addListener(this@AdsExoPlayer)
        }
    }

    private val videoProgressScheduler: VideoProgressScheduler by lazy {
        videoProgressFactory(callback, this, uiPoster)
    }

    private var wasMediaStartedForTheFirstTime = false
    private var isComingFromBackground = false

    override fun currentPosition(): Long = exoPlayer.currentPosition

    override fun asset(asset: VideoAsset) {
        Logger.d("asset() - asset: $asset")
        asset.toMediaItem()?.let {
            with(exoPlayer) {
                addMediaItem(it)
                prepare()
                surfaceView.holder?.addCallback(this@AdsExoPlayer)
            }
        } ?: run {
            val errorMessage = "Error retrieving media item"
            callback?.onVideoDisplayError(errorMessage)
            Logger.e(errorMessage)
        }
        wasMediaStartedForTheFirstTime = false
    }

    private fun VideoAsset.toMediaItem(): MediaItem? =
        exoPlayerMediaItemFactory.mediaItemFrom(this)
            .apply {
                Logger.d("VideoAsset.toMediaItem() - $this")
            }

    override fun play() {
        Logger.d("play()")
        exoPlayer.setVideoSurfaceView(surfaceView)
        exoPlayer.play()
        isComingFromBackground = false
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Logger.d("onIsPlayingChanged() - isPlaying: $isPlaying")
        if (isPlaying) {
            wasMediaStartedForTheFirstTime = true
            callback?.onVideoDisplayStarted()
            startProgressUpdate()
        } else {
            stopProgressUpdate()
        }
    }

    override fun pause() {
        Logger.d("pause()")
        exoPlayer.pause()
    }

    override fun stop() {
        Logger.d("stop()")
        if (exoPlayer.isPlaying) exoPlayer.stop()
        exoPlayer.release()
    }

    private fun startProgressUpdate() {
        videoProgressScheduler.startProgressUpdate()
    }

    private fun stopProgressUpdate() {
        videoProgressScheduler.stopProgressUpdate()
    }

    override fun mute() {
        exoPlayer.volume = 0f
    }

    override fun unmute() {
        exoPlayer.volume = 1f
    }

    override fun wasMediaStartedForTheFirstTime(): Boolean = wasMediaStartedForTheFirstTime

    override fun volume(): Float = exoPlayer.volume

    override fun onScreenOrientationChange(
        width: Int,
        height: Int,
    ) {
        // Do nothing
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Logger.d("surfaceCreated()")
        if (isComingFromBackground) play()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        // Do nothing
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Logger.d("surfaceDestroyed()")
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        Logger.d("onPlaybackStateChanged() - playbackState: ${playbackState.asExoPlayerState()}")
        when (playbackState) {
            Player.STATE_READY -> onVideoReady()
            Player.STATE_ENDED -> onVideoCompleted()
            Player.STATE_BUFFERING -> callback?.onVideoBufferStart()
            Player.STATE_IDLE -> { /* Do nothing */ }
        }
    }

    private fun onVideoReady() {
        handleAspectRatio()
        callback?.onVideoBufferFinish()
        callback?.onVideoDisplayPrepared(exoPlayer.duration)
    }

    private fun onVideoCompleted() {
        stop()
        stopProgressUpdate()
        callback?.onVideoDisplayCompleted()
    }

    override fun onPlayerError(error: PlaybackException) {
        Logger.e("ExoPlayer error", error)
        stop()
        callback?.onVideoDisplayError(error.message ?: "No error message from ExoPlayer")
    }

    private fun handleAspectRatio(
        width: Int = surfaceView.width,
        height: Int = surfaceView.height,
    ) {
        surfaceView.handleAspectRatio(
            videoWidth = exoPlayer.width(),
            videoHeight = exoPlayer.height(),
            surfaceWidth = width,
            surfaceHeight = height,
        )
    }

    override fun onComingFromBackground() {
        isComingFromBackground = true
    }
}

private fun Int.asExoPlayerState(): String =
    when (this) {
        ExoPlayer.STATE_IDLE -> "STATE_IDLE"
        ExoPlayer.STATE_BUFFERING -> "STATE_BUFFERING"
        ExoPlayer.STATE_READY -> "STATE_READY"
        ExoPlayer.STATE_ENDED -> "STATE_ENDED"
        else -> "UNKNOWN"
    }
