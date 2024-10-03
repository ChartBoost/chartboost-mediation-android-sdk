package com.chartboost.sdk.test

import com.chartboost.sdk.internal.UiPoster

/**
 * This Fake UiPoster just runs whatever is meant to be run in Ui in the callers thread.
 * That means that in tests, everything is executed on the same thread simplifying testing the
 * side efects of the execution of the lambdas passed to UiPoster.
 */
internal open class FakeUiPoster : UiPoster {
    override fun invoke(call: () -> Unit) {
        call()
    }

    override fun invoke(
        delay: Long,
        call: () -> Unit,
    ) {
        call()
    }
}
