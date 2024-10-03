package com.chartboost.sdk.internal.di

import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Networking.CBAsync
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService

interface ExecutorComponent {
    val networkExecutor: ExecutorService
    val backgroundExecutor: ScheduledExecutorService
}

class ExecutorModule : ExecutorComponent {
    override val networkExecutor: ExecutorService by lazy {
        CBAsync.createNetworkExecutor(CBConstants.NETWORK_REQUEST_SERVICE_THREADS)
    }

    override val backgroundExecutor: ScheduledExecutorService by lazy {
        CBAsync.createBackgroundExecutor()
    }
}
