package com.chartboost.sdk

import org.junit.runner.notification.RunNotifier
import org.robolectric.RobolectricTestRunner

class DebugAwareRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun run(notifier: RunNotifier?) {
        if (BuildConfig.DEBUG) {
            super.run(notifier)
        }
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class DebugOnlyTest
