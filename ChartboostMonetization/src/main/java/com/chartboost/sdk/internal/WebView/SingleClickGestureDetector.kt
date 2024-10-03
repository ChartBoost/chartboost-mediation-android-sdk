package com.chartboost.sdk.internal.WebView

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting

internal class SingleClickGestureDetector(context: Context) :
    GestureDetector.SimpleOnGestureListener() {
    private val gestureDetector = GestureDetector(context, this)
    internal var hasClick = false
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        hasClick = true
        return super.onSingleTapUp(e)
    }

    fun resetClickState() {
        hasClick = false
    }
}
