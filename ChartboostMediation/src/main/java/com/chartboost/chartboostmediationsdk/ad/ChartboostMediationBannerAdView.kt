/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.Theme
import android.graphics.Color
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import com.chartboost.chartboostmediationsdk.R
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.LEADERBOARD
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.MEDIUM
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.STANDARD
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.bannerSize
import com.chartboost.chartboostmediationsdk.controllers.banners.BannerController
import com.chartboost.chartboostmediationsdk.controllers.banners.VisibilityTracker
import com.chartboost.chartboostmediationsdk.domain.AdFormat
import com.chartboost.chartboostmediationsdk.domain.AppConfigStorage
import com.chartboost.chartboostmediationsdk.domain.Keywords
import com.chartboost.chartboostmediationsdk.utils.Dips
import com.chartboost.chartboostmediationsdk.utils.LogController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.floor

private const val NOT_FOUND = -1
private const val EMPTY_PLACEMENT = ""

@SuppressLint("ViewConstructor")
class ChartboostMediationBannerAdView :
    FrameLayout,
    ChartboostMediationAd {
    var availableWidthDips: Int = 0
        private set
    var availableHeightDips: Int = 0
        private set

    override var keywords: Keywords = Keywords()

    override var placement: String = ""
        private set
    override var partnerSettings: MutableMap<String, Any> = mutableMapOf()

    private var size: ChartboostMediationBannerSize? = null
    var chartboostMediationBannerAdViewListener: ChartboostMediationBannerAdViewListener? = null
        get() {
            if (field == null) {
                LogController.w("Banner listener is null on getChartboostMediationBannerAdListener")
            }
            return field
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val bannerController = BannerController(WeakReference(this))

    /**
     * This sets whether or not the winning bid information is sent to the listeners. If this
     * placement automatically refreshes, this value is false. Otherwise, this is true.
     */
    var shouldFireListeners = true
        get() = !bannerController.shouldAutoRefresh
        private set
    private var lastWindowVisibility = windowVisibility
    private var lastVisibility = visibility

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        if (attrs != null) {
            val attributesFromLayout = retrieveValuesFromAttributes(context.theme, attrs)
            val attrPlacement = attributesFromLayout.placement ?: EMPTY_PLACEMENT
            val attrSize = attributesFromLayout.size
            if (attrSize == NOT_FOUND) {
                LogController.e("Error creating ChartboostMediationBannerAd, make sure the attributes declared in the XML are correct")
            } else {
                this.size =
                    attrSize.toBannerSize(
                        Dips.pixelsToIntDips(attributesFromLayout.flexibleWidth, context),
                        Dips.pixelsToIntDips(attributesFromLayout.flexibleHeight, context),
                    )
                this.placement = attrPlacement
            }
        } else {
            LogController.e("Error creating ChartboostMediationBannerAd, make sure the attributes declared in the XML are correct")
        }
    }

    constructor(
        context: Context,
        placement: String,
        size: ChartboostMediationBannerSize,
        chartboostMediationBannerAdViewListener: ChartboostMediationBannerAdViewListener?,
    ) : super(context) {
        this.placement = placement
        this.size = size
        this.chartboostMediationBannerAdViewListener = chartboostMediationBannerAdViewListener
    }

    init {
        if (!isInEditMode) {
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // The parent's constraints on the width and height
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (parentHeight == 0 && parentWidth == 0) {
            availableWidthDips = 0
            availableHeightDips = 0
            return
        }

        // Get the total height used by the siblings
        var siblingsMaxWidth = 0
        var siblingsMaxHeight = 0
        if (parent is ViewGroup) {
            val parentVG = parent as ViewGroup
            for (i in 0 until parentVG.childCount) {
                val child = parentVG.getChildAt(i)
                if (child != this && child.visibility != View.GONE) {
                    siblingsMaxWidth = maxOf(siblingsMaxWidth, child.measuredWidth)
                    siblingsMaxHeight = maxOf(siblingsMaxHeight, child.measuredHeight)
                }
            }
        }

        val density = context.resources.displayMetrics.density

        // Compute the available width and height in dips
        availableWidthDips =
            floor((parentWidth - siblingsMaxWidth - paddingLeft - paddingRight) / density).toInt()
        availableHeightDips =
            floor((parentHeight - siblingsMaxHeight - paddingTop - paddingBottom) / density).toInt()
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        chartboostMediationBannerAdViewListener?.onAdViewAdded(placement, child)
    }

    /**
     * Information regarding the banner size.
     */
    data class ChartboostMediationBannerSize private constructor(
        /**
         * The name of the particular banner size. This is one of the standard sizes or "ADAPTIVE"
         * for an adaptive size.
         */
        val name: String,
        /**
         * The width of the ad in density-independent pixels.
         */
        val width: Int,
        /**
         * The height of the ad in density-independent pixels. This can be 0 to indicate adaptive
         * height banners.
         */
        val height: Int,
        /**
         * Whether or not the banner is an adaptive banner.
         */
        val isAdaptive: Boolean = false,
    ) {
        val aspectRatio: Double =
            if (height == 0 || width == 0) {
                0.0
            } else {
                width.toDouble() / height.toDouble()
            }

        companion object {
            @JvmField
            val STANDARD = ChartboostMediationBannerSize("STANDARD", 320, 50)

            @JvmField
            val MEDIUM = ChartboostMediationBannerSize("MEDIUM", 300, 250)

            @JvmField
            val LEADERBOARD = ChartboostMediationBannerSize("LEADERBOARD", 728, 90)

            /**
             * Extension function to turn this into an Android [Size] object.
             *
             * @return Android [Size] equivalent.
             */
            fun ChartboostMediationBannerSize.asSize() = Size(width, height)

            @JvmStatic
            fun bannerSize(
                width: Int,
                height: Int = 0,
            ): ChartboostMediationBannerSize = ChartboostMediationBannerSize("ADAPTIVE", width, height, true)

            // Converted functions
            fun adaptive2x1(width: Int) = bannerSize(width, (width / 2.0).toInt())

            fun adaptive4x1(width: Int) = bannerSize(width, (width / 4.0).toInt())

            fun adaptive6x1(width: Int) = bannerSize(width, (width / 6.0).toInt())

            fun adaptive8x1(width: Int) = bannerSize(width, (width / 8.0).toInt())

            fun adaptive10x1(width: Int) = bannerSize(width, (width / 10.0).toInt())

            fun adaptive1x2(width: Int) = bannerSize(width, (width * 2.0).toInt())

            fun adaptive1x3(width: Int) = bannerSize(width, (width * 3.0).toInt())

            fun adaptive1x4(width: Int) = bannerSize(width, (width * 4.0).toInt())

            fun adaptive9x16(width: Int) = bannerSize(width, ((width * 16.0) / 9.0).toInt())

            fun adaptive1x1(width: Int) = bannerSize(width, width)
        }
    }

    private fun Int.toBannerSize(
        flexibleWidth: Int,
        flexibleHeight: Int,
    ): ChartboostMediationBannerSize =
        when (this) {
            0 -> STANDARD

            1 -> MEDIUM

            2 -> LEADERBOARD

            4 -> bannerSize(flexibleWidth, flexibleHeight)

            else -> {
                LogController.w("Size not defined, set to STANDARD by default")
                STANDARD
            }
        }

    override fun getAdType(): Int =
        AdFormat.toAdType(
            AppConfigStorage.placementsToAdFormats?.get(placement),
        )

    suspend fun load(request: ChartboostMediationBannerAdLoadRequest): ChartboostMediationBannerAdLoadResult {
        this.placement = request.placement
        this.size = request.size

        val result = bannerController.load(request)
        return result
    }

    /**
     * Load a banner ad. This method is designed to be called from Java.
     *
     * @param request The publisher-supplied [ChartboostMediationBannerAdLoadRequest] containing relevant details to load the ad.
     * @param adLoadListener The [ChartboostMediationBannerAdLoadListener] to notify of the ad load event.
     */
    fun loadFromJava(
        request: ChartboostMediationBannerAdLoadRequest,
        adLoadListener: ChartboostMediationBannerAdLoadListener,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = load(request)
            adLoadListener.onAdLoaded(result)
        }
    }

    fun getSize() = this.size

    fun getCreativeSizeDips(): Size = bannerController.getCreativeSizeDips(size)

    fun clearAd() {
        bannerController.clearAd()
    }

    override fun destroy() {
        chartboostMediationBannerAdViewListener = null
        bannerController.destroy()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)

        when {
            // Ignore 'transitions' to the same visibility
            visibility == lastWindowVisibility -> return

            // Ignore transitions between View.GONE and View.INVISIBLE
            (visibility == View.GONE && lastWindowVisibility == View.INVISIBLE) -> return

            (visibility == View.INVISIBLE && lastWindowVisibility == View.GONE) -> return

            else -> {
                lastWindowVisibility = visibility
                checkAllVisibility()
            }
        }
    }

    override fun onVisibilityChanged(
        changedView: View,
        visibility: Int,
    ) {
        super.onVisibilityChanged(changedView, visibility)

        val overallVisibility = getOverallVisibility()
        when {
            // Ignore 'transitions' to the same visibility
            overallVisibility == lastVisibility -> return

            // Ignore transitions between View.GONE and View.INVISIBLE
            (overallVisibility == View.GONE && lastVisibility == View.INVISIBLE) -> return

            (overallVisibility == View.INVISIBLE && lastVisibility == View.GONE) -> return

            else -> {
                lastVisibility = overallVisibility
                checkAllVisibility()
            }
        }
    }

    private fun getOverallVisibility(): Int {
        var currentVisibility = visibility
        var traversalCount = 0
        var currentParent = parent
        while (currentParent != null && traversalCount < VisibilityTracker.TRAVERSAL_LIMIT) {
            if (currentParent is View && currentParent.visibility > currentVisibility) {
                currentVisibility = currentParent.visibility
            }
            // We don't actually care whether it's INVISIBLE or GONE as long as we know it's not VISIBLE.
            if (currentVisibility > View.VISIBLE) {
                return currentVisibility
            }
            traversalCount++
            currentParent = currentParent.parent
        }
        return currentVisibility
    }

    private fun checkAllVisibility() {
        if (lastWindowVisibility == View.VISIBLE && lastVisibility == View.VISIBLE) {
            bannerController.onChartboostMediationBannerAdResumeRefresh()
        } else {
            bannerController.onChartboostMediationBannerAdPauseRefresh()
        }
    }

    private fun retrieveValuesFromAttributes(
        theme: Theme,
        attrs: AttributeSet,
    ): ChartboostMediationBannerAttributes {
        val attributes =
            theme.obtainStyledAttributes(attrs, R.styleable.ChartboostMediationBannerAd, 0, 0)
        val location: String? =
            attributes.getString(R.styleable.ChartboostMediationBannerAd_chartboostMediationPlacement)
        val sizeInLayout: Int =
            attributes.getInt(
                R.styleable.ChartboostMediationBannerAd_chartboostMediationBannerSize,
                NOT_FOUND,
            )
        val flexibleWidth =
            attributes.getDimensionPixelSize(
                R.styleable.ChartboostMediationBannerAd_chartboostMediationBannerFlexibleWidth,
                0,
            )
        val flexibleHeight =
            attributes.getDimensionPixelSize(
                R.styleable.ChartboostMediationBannerAd_chartboostMediationBannerFlexibleHeight,
                0,
            )

        attributes.recycle()
        return ChartboostMediationBannerAttributes(
            location,
            sizeInLayout,
            flexibleWidth,
            flexibleHeight,
        )
    }

    private data class ChartboostMediationBannerAttributes(
        val placement: String?,
        val size: Int,
        val flexibleWidth: Int,
        val flexibleHeight: Int,
    )
}
