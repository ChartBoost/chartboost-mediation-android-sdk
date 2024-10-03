package com.chartboost.sdk.view

import com.chartboost.sdk.internal.View.ViewBase

/**
 * Contract interfaces between CBImpressionActivity and its presenter
 */
internal interface ImpressionActivityContract {
    /**
     * CBImpressionActivity view interface
     * Passes view data down to the presenter and handles
     * setting view to a full screen
     */
    interface ImpressionActivityView {
        fun attachViewToActivity(view: ViewBase)

        fun getActivity(): CBImpressionActivity

        fun setFullscreen()

        fun isActivityHardwareAccelerated(): Boolean

        fun finishActivity()
    }

    /**
     * ImpressionActivity presenter interface
     * Handles internal implementation of the activity lifecycle
     */
    interface ImpressionActivityPresenter {
        fun onCreate()

        fun onStart()

        fun onResume()

        fun onPause()

        fun onDestroy()

        fun onConfigurationChange()

        fun onViewAttached()

        fun onBackPressed(): Boolean
    }
}
