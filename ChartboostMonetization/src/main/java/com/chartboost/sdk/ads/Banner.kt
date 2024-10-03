package com.chartboost.sdk.ads

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.callbacks.BannerCallback
import com.chartboost.sdk.events.CacheError
import com.chartboost.sdk.events.CacheEvent
import com.chartboost.sdk.events.ShowError
import com.chartboost.sdk.events.ShowEvent
import com.chartboost.sdk.internal.BannerApi
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer.androidComponent
import com.chartboost.sdk.internal.di.createBannerApi
import com.chartboost.sdk.internal.logging.Logger

/**
 * Banner is a view subclass able to show banner ads. Once obtained via the provided initializer,
 * the developer is responsible for adding it to the app's view hierarchy and laying it out properly.
 *
 * The developer should not give the banner view object a frame size different from its BannerSize
 * property.
 *
 * The developer is responsible for removing the banner from the view when changing the view.
 * After the banner is detached, it has to be recreated as a new object.
 *
 * By default, a banner will automatically update its content on its own.
 * This means you only need to call show() once and the banner will get new ads and show them,
 * gracefully handling errors if they occur.
 *
 * You can create and present as many banners as you want at the same time.
 *
 * A typical implementation would look like this:
 * ```
 * fun createAndShowBanner() {
 *     val banner = Banner(this, "location", BannerSize.STANDARD, callback)
 *     view_container?.removeAllViews()
 *     view_container?.addView(banner, getViewParams())
 *     banner.show()
 * }
 * ```
 *
 * For more information on integrating and using the Chartboost SDK, please visit our help site
 * documentation at https://help.chartboost.com.
 */
@SuppressLint("ViewConstructor")
class Banner(
    context: Context,
    override val location: String,
    private val size: BannerSize,
    private val callback: BannerCallback,
    private val mediation: Mediation? = null,
) : FrameLayout(context), Ad {
    private val api: BannerApi by lazy { createBannerApi(mediation) }

    override fun cache() {
        if (!Chartboost.isSdkStarted()) {
            postSessionNotStartedInMainThread(true)
            return
        }
        api.cache(this, callback)
    }

    override fun cache(bidResponse: String?) {
        if (!Chartboost.isSdkStarted()) {
            postSessionNotStartedInMainThread(true)
            return
        }

        if (bidResponse.isNullOrEmpty()) {
            api.onAdFailToLoad("", CBError.Impression.INVALID_RESPONSE)
        } else {
            api.cache(this, callback, bidResponse)
        }
    }

    override fun show() {
        if (!Chartboost.isSdkStarted()) {
            postSessionNotStartedInMainThread(false)
            return
        }
        api.fillSize(this)
        api.show(this, callback)
    }

    override fun clearCache() {
        if (Chartboost.isSdkStarted()) {
            api.clearCache()
        }
    }

    override fun isCached(): Boolean {
        return if (Chartboost.isSdkStarted()) {
            api.isCached()
        } else {
            false
        }
    }

    fun getBannerWidth(): Int {
        return size.width
    }

    fun getBannerHeight(): Int {
        return size.height
    }

    fun detach() {
        if (Chartboost.isSdkStarted()) {
            api.detach()
        }
    }

    private fun postSessionNotStartedInMainThread(isCache: Boolean) {
        try {
            androidComponent.uiPoster {
                if (isCache) {
                    callback.onAdLoaded(
                        CacheEvent(null, this),
                        CacheError(CacheError.Code.SESSION_NOT_STARTED),
                    )
                } else {
                    callback.onAdShown(
                        ShowEvent(null, this),
                        ShowError(ShowError.Code.SESSION_NOT_STARTED),
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("Banner ad cannot post session not started callback $e")
        }
    }

    /**
     * Defines standard sizes for Banner objects.
     * Standard sizes used to describe default banner bounds.
     */
    enum class BannerSize(val width: Int, val height: Int) {
        /** "Banner" - Standard banner size on phones. */
        STANDARD(320, 50),

        /** "Medium Rect" - Medium banner size on phones. */
        MEDIUM(300, 250),

        /** "Tablet" - Leaderboard banner size on tablets. */
        LEADERBOARD(728, 90),
    }
}
