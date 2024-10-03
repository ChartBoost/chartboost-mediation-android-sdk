package com.chartboost.sdk.internal.video.repository.exoplayer

import android.app.Notification
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper

private const val NOTIFICATION_CHANNEL_ID = "chartboost"

class VideoRepositoryDownloadService : DownloadService(FOREGROUND_NOTIFICATION_ID_NONE) {
    private val exoPlayerDownloadManager: ExoPlayerDownloadManager by lazy {
        ChartboostDependencyContainer.applicationComponent.exoPlayerDownloadManager
    }

    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        // Initialize dependency container in case the service is started before SDK initialization
        // Note that it needs to be before super.onCreate() since it will call getDownloadManager()
        ChartboostDependencyContainer.initialize(this)
        super.onCreate()
        downloadNotificationHelper = DownloadNotificationHelper(this, NOTIFICATION_CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager =
        run {
            with(exoPlayerDownloadManager) {
                initialize()
                getDownloadManager()
            }
        }

    override fun getScheduler(): Scheduler? = scheduler(context = this)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification =
        downloadNotificationHelper
            .buildProgressNotification(
                this,
                0,
                null,
                null,
                emptyList(),
                0,
            )
}
