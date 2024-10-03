package com.chartboost.sdk.mock.android

import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.test.ScheduledExecutionQueue
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/*
    ForegroundHandlerMockWrapper uses a UiPoster that processes events
    within the current thread.

    Use this in unit tests where the setup code would normally execute in the UI thread.

    The creating scope should do what it needs to as if it is the UI thread,
    and then call processEvents(# of expected events).  The runnables
    posted will be processed by the current thread.

    Any exception thrown by a Runnable.run() will be propagated to the caller
    of processEvents().

    If runnables were posted with a delay (postDelayed()), you can run them by
    calling TimeSource.advance() and then runNext().
 */
internal class UiPosterScheduledExecutionQueue(
    timeSource: TimeSource?,
) : ScheduledExecutionQueue(
        uptimeMillisReader(timeSource),
        TimeUnit.MILLISECONDS,
        ArrayList(),
    ) {
    val mockHandler: UiPoster = ScheduledUiPoster(this)
}

private class ScheduledUiPoster(
    private val scheduler: UiPosterScheduledExecutionQueue,
) : UiPoster {
    override fun invoke(call: () -> Unit) {
        scheduler.scheduleNow(call)
    }

    override fun invoke(
        delay: Long,
        call: () -> Unit,
    ) {
        scheduler.scheduleAfter(call, delay, TimeUnit.MILLISECONDS)
    }
}
