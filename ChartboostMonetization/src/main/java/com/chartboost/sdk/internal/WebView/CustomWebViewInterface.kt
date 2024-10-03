package com.chartboost.sdk.internal.WebView

import android.view.View

internal interface CustomWebViewInterface {
    fun onPageStarted()

    fun onWebViewInit()

    fun onError(message: String)

    fun onRegisterWebViewTimeout()

    fun onRegisterFriendlyWebViewObstruction(obstruction: View)

    fun onPageFinished()
}
