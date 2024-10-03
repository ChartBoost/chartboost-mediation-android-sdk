package com.chartboost.sdk.view

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.logging.Logger.e

internal class CBImpressionActivity : Activity(), ImpressionActivityContract.ImpressionActivityView {
    private var presenter: ImpressionActivityPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBackInvokedCallback()
        if (!hasActivityValidIdentifier()) {
            e("This activity cannot be called from outside chartboost SDK")
            finish()
            return
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setWindowAnimations(0)
        createPresenterWhenNeeded()
        presenter?.onCreate()
    }

    private fun hasActivityValidIdentifier(): Boolean {
        return intent?.getBooleanExtra(
            CBConstants.CBIMPRESSIONACTIVITY_IDENTIFIER,
            false,
        ) ?: false
    }

    override fun onStart() {
        super.onStart()
        presenter?.onStart()
    }

    override fun onResume() {
        super.onResume()
        createPresenterWhenNeeded()
        presenter?.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter?.onPause()
    }

    override fun onDestroy() {
        presenter?.onDestroy()
        presenter = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO - uncomment when updating to target 34
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            onBackInvoked()
//        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        presenter?.onConfigurationChange()
        super.onConfigurationChanged(newConfig)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter?.onViewAttached()
    }

    override fun isActivityHardwareAccelerated(): Boolean {
        return window?.decorView?.isHardwareAccelerated ?: false
    }

    override fun finishActivity() {
        finish()
    }

    override fun attachViewToActivity(view: ViewBase) {
        try {
            val parent = view.parent
            if (parent is ViewGroup) {
                parent.removeView(view)
            }

            addContentView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        } catch (e: Exception) {
            Logger.d("Cannot attach view to activity", e)
        }
    }

    override fun getActivity(): CBImpressionActivity {
        return this
    }

    override fun setFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.let {
                    it.setDecorFitsSystemWindows(true)
                    it.insetsController?.apply {
                        hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        systemBarsBehavior =
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                @Suppress("DEPRECATION")
                window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window?.attributes?.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } catch (e: Exception) {
            // Added this catch after reports of a null pointer happening here in some devices
            Logger.d("Cannot set view to fullscreen", e)
        }
    }

    /**
     * Due to sdk initialisation issue this could create crash because it tries to access
     * the renderer in ChartboostDependencyContainer.renderComponent.uiManager which can only be
     * done after sdk successful initialisation
     */
    private fun createPresenterWhenNeeded() {
        if (presenter == null) {
            if (Chartboost.isSdkStarted()) {
                presenter =
                    ImpressionActivityPresenter(
                        this,
                        ChartboostDependencyContainer.renderComponent.rendererActivityBridge,
                        ChartboostDependencyContainer.applicationComponent.sdkConfig.get(),
                        ChartboostDependencyContainer.androidComponent.displayMeasurement,
                    )
            } else {
                Logger.e("Cannot start Chartboost activity due to SDK not being initialized.")
                finish()
            }
        }
    }

    private fun setOnBackInvokedCallback() {
        // TODO - uncomment when updating to target 34
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            onBackInvokedDispatcher.registerOnBackInvokedCallback(
//                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
//                ::onBackInvoked,
//            )
//        }
    }

    private fun onBackInvoked() {
        try {
            val isBackPressed = presenter?.onBackPressed() ?: false
            if (!isBackPressed) {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            e("onBackPressed error", e)
            finish()
        }
    }
}
