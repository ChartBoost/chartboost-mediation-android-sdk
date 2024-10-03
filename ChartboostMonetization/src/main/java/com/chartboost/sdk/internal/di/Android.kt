package com.chartboost.sdk.internal.di

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.util.DisplayMetrics
import android.view.WindowManager
import com.chartboost.sdk.internal.External.Android
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Model.DeviceFieldsWrapper
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.UiPosterImpl
import com.chartboost.sdk.internal.utils.Base64Wrapper
import com.chartboost.sdk.internal.utils.ResourceLoader
import com.chartboost.sdk.internal.utils.SharedPrefsHelper

internal interface AndroidComponent {
    val context: Context
    val app: Application
    val sharedPreferences: SharedPreferences
    val trackingSharedPreferences: SharedPreferences
    val android: Android
    val uiPoster: UiPoster
    val base64Wrapper: Base64Wrapper
    val resourceLoader: ResourceLoader
    val sharedPrefsHelper: SharedPrefsHelper
    val windowManager: WindowManager
    val displayMetrics: DisplayMetrics
    val displayMeasurement: DisplayMeasurement
    val deviceFieldsWrapper: DeviceFieldsWrapper
    val contentResolver: ContentResolver
}

internal class AndroidModule(
    override val context: Context,
    override val app: Application,
) :
    AndroidComponent {
    override val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(CBConstants.PREFERENCES_FILE_DEFAULT, Context.MODE_PRIVATE)
    }

    override val trackingSharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(CBConstants.PREFERENCES_FILE_TRACKING, Context.MODE_PRIVATE)
    }

    override val android: Android by lazy {
        Android.instance()
    }

    override val uiPoster: UiPoster by lazy {
        UiPosterImpl()
    }

    override val base64Wrapper: Base64Wrapper by lazy {
        Base64Wrapper()
    }

    override val resourceLoader: ResourceLoader by lazy {
        ResourceLoader(context.resources)
    }

    override val sharedPrefsHelper: SharedPrefsHelper by lazy {
        SharedPrefsHelper(sharedPreferences)
    }

    override val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override val displayMetrics: DisplayMetrics by lazy {
        context.resources.displayMetrics
    }

    override val displayMeasurement: DisplayMeasurement by lazy {
        DisplayMeasurement(windowManager, displayMetrics)
    }

    override val deviceFieldsWrapper: DeviceFieldsWrapper by lazy {
        DeviceFieldsWrapper(context, displayMeasurement)
    }

    override val contentResolver: ContentResolver by lazy {
        context.contentResolver
    }
}
