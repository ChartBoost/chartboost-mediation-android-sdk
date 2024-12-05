/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.controllers.banners

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import com.chartboost.heliumsdk.controllers.banners.VisibilityTracker.Companion.VISIBILITY_CHECK_INTERVAL_MS
import com.chartboost.heliumsdk.utils.Dips
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.lang.ref.WeakReference

/**
 * @suppress
 *
 * Tracks views to determine when they become visible, where visibility is determined by
 * whether a minimum number of dips have been visible for a minimum duration. This check
 * is made every [VISIBILITY_CHECK_INTERVAL_MS] milliseconds.
 * trackedView is the view that we want to check, and rootView is the top most view where
 * we check bounds.
 */
class VisibilityTracker(
    context: Context,
    private val trackedView: View,
    private val rootView: View,
    private val minVisibleDips: Int,
    private val minVisibleMs: Int,
    private val visibilityCheckIntervalMs: Long,
    private val traversalLimit: Int,
) {
    companion object {
        /**
         * The default number of dips to be on screen before being counted as visible.
         */
        const val MIN_VISIBLE_DIPS = 1

        /**
         * The default duration that the tracked view needs to be on the screen before being
         * counted as visible.
         */
        const val MIN_VISIBLE_DURATION_MS = 0

        /**
         * This check is performed every [VISIBILITY_CHECK_INTERVAL_MS] milliseconds by default.
         */
        const val VISIBILITY_CHECK_INTERVAL_MS = 100L

        /**
         * Maximum number of parents to check visibility by default.
         */
        const val TRAVERSAL_LIMIT = 25

        /**
         * Finds the topmost view in the current Activity or current view hierarchy.
         *
         * @param context If an Activity Context, used to obtain the Activity's DecorView. This is
         *                ignored if it is a non-Activity Context.
         * @param view A View in the currently displayed view hierarchy. If a null or non-Activity
         *             Context is provided, this View's topmost parent is used to determine the
         *             rootView.
         * @return The topmost View in the currency Activity or current view hierarchy. Null if no
         * applicable View can be found.
         */
        fun getTopmostView(
            context: Context?,
            view: View?,
        ): View? {
            return (context as? Activity)?.window?.decorView?.findViewById(android.R.id.content)
                ?: view?.rootView?.findViewById(android.R.id.content) ?: view?.rootView
        }
    }

    /**
     * Callback for notifying that the ad has been sufficiently visible.
     */
    interface VisibilityTrackerListener {
        /**
         * Called when the visibility thresholds have been met.
         */
        fun onVisibilityThresholdMet()
    }

    /**
     * Callback for when the visibility threshold has been met.
     */
    var visibilityTrackerListener: VisibilityTrackerListener? = null

    private val weakActivity = WeakReference(context as? Activity)

    private var job: Job? = null
    private var weakViewTreeObserver: WeakReference<ViewTreeObserver> = WeakReference(null)
    private var preDrawListener =
        ViewTreeObserver.OnPreDrawListener {
            scheduleVisibilityCheck()
            true
        }
    private var isVisibilityTracked = false
    private var startTimeMs: Long? = null

    /**
     * A rectangle for overlap detection. Created once to avoid excessive garbage collection.
     */
    private val cachedRect = Rect()

    fun start() {
        setViewTreeObserver()
    }

    private fun setViewTreeObserver() {
        try {
            if (weakViewTreeObserver.get()?.isAlive == true) {
                return
            }
        } catch (e: Exception) {
            LogController.d("Exception when accessing view tree observer.")
        }

        val viewTreeObserver =
            getTopmostView(weakActivity.get(), trackedView)?.viewTreeObserver ?: return
        if (!viewTreeObserver.isAlive) {
            LogController.i("Unable to set ViewTreeObserver since it is not alive")
            return
        }

        weakViewTreeObserver = WeakReference(viewTreeObserver)
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    private fun scheduleVisibilityCheck() {
        if (job != null) return

        job =
            CoroutineScope(Main).launch(
                CoroutineExceptionHandler { _, throwable ->
                    LogController.d("Visibility check ran into a problem: $throwable")
                },
            ) {
                while (isActive) {
                    if (isVisibilityTracked) {
                        break
                    }

                    // Check to see if the tracked view is within the bounds of the root view.
                    if (isViewVisible()) {
                        // Start the timer for duration requirement if it hasn't already.
                        startTimeMs = startTimeMs ?: SystemClock.uptimeMillis()

                        if (hasRequiredTimeElapsed()) {
                            visibilityTrackerListener?.onVisibilityThresholdMet()
                            isVisibilityTracked = true
                            break
                        }
                    }
                    withContext(IO) {
                        delay(visibilityCheckIntervalMs)
                    }
                }
            }
    }

    /**
     * Destroys this VisibilityTracker and cancels all checks.
     */
    fun destroy() {
        cancelVisibilityCheck()
        weakViewTreeObserver.get()?.run {
            if (isAlive) {
                removeOnPreDrawListener(preDrawListener)
            }
        }
        weakViewTreeObserver.clear()
        visibilityTrackerListener = null
    }

    private fun cancelVisibilityCheck() {
        job?.cancel()
        job = null
    }

    private fun hasRequiredTimeElapsed(): Boolean {
        return startTimeMs?.let {
            SystemClock.uptimeMillis() - it >= minVisibleMs
        } ?: false
    }

    private fun isViewVisible(): Boolean {
        // ListView & GridView both call detachFromParent() for views that can be recycled for
        // new data. This is one of the rare instances where a view will have a null parent for
        // an extended period of time and will not be the main window.
        // view.getGlobalVisibleRect() doesn't check that case, so if the view has visibility
        // of View.VISIBLE but its group has no parent it is likely in the recycle bin of a
        // ListView / GridView and not on screen.
        if (trackedView.visibility != View.VISIBLE || rootView.parent == null) {
            return false
        }

        // If either width or height is non-positive, the view cannot be visible.
        if (trackedView.width <= 0 || trackedView.height <= 0) {
            return false
        }

        // Also traverse the view hierarchy to see if all the parent views are visible.
        var parent = trackedView.parent
        var count = 0
        while (parent != null && count < traversalLimit) {
            if (parent is View && parent.visibility != View.VISIBLE) {
                return false
            }
            count++
            parent = parent.parent
        }

        // View completely clipped by its parents
        if (!trackedView.getGlobalVisibleRect(cachedRect)) {
            return false
        }

        // These names are incorrect as we were previously using a flawed function. Leaving them as-is for now.
        val widthDips = Dips.dipsToPixelsInt(cachedRect.width(), trackedView.context)
        val heightDips = Dips.dipsToPixelsInt(cachedRect.height(), trackedView.context)
        return widthDips * heightDips >= minVisibleDips
    }
}
