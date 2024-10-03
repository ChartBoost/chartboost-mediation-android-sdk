package com.chartboost.sdk.internal.initialization

import com.chartboost.sdk.internal.di.getEventTracker
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONObject

internal object ExceptionHandler : EventTrackerExtensions by getEventTracker() {
    internal fun setCustomExceptionHandler() {
        val defaultException = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isExceptionFromDesiredPackage(throwable)) {
                ErrorEvent(
                    TrackingEventName.Show.DISMISS_MISSING,
                    throwable.asErrorMessage(),
                ).track()
            }
            // Call the default handler to ensure the app is terminated
            defaultException?.uncaughtException(thread, throwable)
        }
    }

    private fun isExceptionFromDesiredPackage(throwable: Throwable): Boolean {
        return throwable.stackTrace.any {
            it?.className?.startsWith("com.chartboost.sdk") ?: false
        }
    }

    private fun Throwable?.asErrorMessage(): String {
        return JSONObject().put("reason", "dismiss_event due to the unhandled exceptions")
            .put("error", "${this@asErrorMessage}").toString()
    }
}
