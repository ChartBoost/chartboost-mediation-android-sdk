package com.chartboost.sdk.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal interface UiPoster {
    operator fun invoke(call: () -> Unit)

    operator fun invoke(
        delay: Long,
        call: () -> Unit,
    )
}

internal class UiPosterImpl : UiPoster {
    override operator fun invoke(call: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            call()
        }
    }

    override fun invoke(
        delay: Long,
        call: () -> Unit,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(delay)
            call()
        }
    }
}
