/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.Theme
import android.graphics.Color
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.chartboost.heliumsdk.R
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize.Companion.LEADERBOARD
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize.Companion.MEDIUM
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize.Companion.STANDARD
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize.Companion.bannerSize
import com.chartboost.heliumsdk.controllers.banners.BannerController
import com.chartboost.heliumsdk.controllers.banners.VisibilityTracker
import com.chartboost.heliumsdk.domain.Ad
import com.chartboost.heliumsdk.domain.AdFormat
import com.chartboost.heliumsdk.domain.AppConfigStorage
import com.chartboost.heliumsdk.domain.Keywords
import com.chartboost.heliumsdk.utils.Dips
import com.chartboost.heliumsdk.utils.LogController
import java.lang.ref.WeakReference
import kotlin.math.floor

private const val NOT_FOUND = -1
private const val EMPTY_PLACEMENT = ""

@SuppressLint("ViewConstructor")
class HeliumBannerAd : FrameLayout, HeliumAd {

    private var availableWidthDips: Int = 0
    private var availableHeightDips: Int = 0

    override val keywords: Keywords = Keywords()
    override var placementName: String = ""
        private set

    private var size: HeliumBannerSize? = null
    var heliumBannerAdListener: HeliumBannerAdListener? = null
        get() {
            if (field == null) {
                LogController.w("Banner listener is null on getHeliumBannerAdListener")
            }
            return field
        }

    private val bannerController = BannerController(WeakReference(this))

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
            defStyleAttr
    ) {
        if (attrs != null) {
            val attributesFromLayout = retrieveValuesFromAttributes(context.theme, attrs)
            val attrPlacementName = attributesFromLayout.placementName ?: EMPTY_PLACEMENT
            val attrSize = attributesFromLayout.size
            if (attrSize == NOT_FOUND) {
                LogController.e("Error creating HeliumBannerAd, make sure the attributes declared in the XML are correct")
            } else {
                this.size = attrSize.toBannerSize(
                    Dips.pixelsToIntDips(attributesFromLayout.flexibleWidth, context),
                    Dips.pixelsToIntDips(attributesFromLayout.flexibleHeight, context)
                )
                this.placementName = attrPlacementName
            }
        } else {
            LogController.e("Error creating HeliumBannerAd, make sure the attributes declared in the XML are correct")
        }
    }

    constructor(
        context: Context,
        placementName: String,
        size: HeliumBannerSize,
        heliumBannerAdListener: HeliumBannerAdListener?
    ) : super(context) {
        this.placementName = placementName
        this.size = size
        this.heliumBannerAdListener = heliumBannerAdListener
    }

    init {
        if (!isInEditMode) {
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // The parent's constraints on the width and height
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (parentHeight == 0  && parentWidth == 0) {
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
        heliumBannerAdListener?.onAdViewAdded(placementName, child)
    }

    data class HeliumBannerSize private constructor(
        val name: String,
        val width: Int,
        val height: Int,
        val isAdaptive: Boolean = false,
    ) {
        val aspectRatio: Double = if (height == 0 || width == 0) {
            0.0
        } else {
            width.toDouble() / height.toDouble()
        }

        companion object {
            @JvmField
            val STANDARD = HeliumBannerSize("STANDARD", 320, 50)

            @JvmField
            val MEDIUM = HeliumBannerSize("MEDIUM", 300, 250)

            @JvmField
            val LEADERBOARD = HeliumBannerSize("LEADERBOARD", 728, 90)

            @JvmStatic
            fun bannerSize(width: Int, height: Int = 0): HeliumBannerSize {
                return HeliumBannerSize("ADAPTIVE", width, height, true)
            }

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

    private fun Int.toBannerSize(flexibleWidth: Int, flexibleHeight: Int): HeliumBannerSize {
        return when (this) {
            0 -> STANDARD
            1 -> MEDIUM
            2 -> LEADERBOARD
            4 -> bannerSize(flexibleWidth, flexibleHeight)
            else -> {
                LogController.w("Size not defined, set to STANDARD by default")
                STANDARD
            }
        }
    }

    override fun getAdType(): Int {
        return AdFormat.toAdType(
            AppConfigStorage.placementsToAdFormats?.get(placementName)
        )
    }

    fun load(placementName: String? = null, size: HeliumBannerSize? = null) {
        if (placementName != this.placementName || this.size != size) {
            this.placementName = placementName ?: this.placementName
            this.size = size ?: this.size

            bannerController.renewCachedAd()
        } else {
            load()
        }
    }

    override fun load() {
        bannerController.load()
    }

    fun getSize() = this.size

    fun getCreativeSizeDips(): Size {
        return bannerController.getCreativeSizeDips(size)
    }

    fun clearAd() {
        bannerController.clearAd()
    }

    override fun destroy() {
        heliumBannerAdListener = null
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

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
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
            bannerController.onHeliumBannerAdResumeRefresh()
        } else {
            bannerController.onHeliumBannerAdPauseRefresh()
        }
    }

    private fun retrieveValuesFromAttributes(
            theme: Theme,
            attrs: AttributeSet
    ): HeliumBannerAttributes {
        val attributes = theme.obtainStyledAttributes(attrs, R.styleable.HeliumBannerAd, 0, 0)
        val location: String? = attributes.getString(R.styleable.HeliumBannerAd_heliumPlacementName)
        val sizeInLayout: Int = attributes.getInt(
                R.styleable.HeliumBannerAd_heliumBannerSize,
                NOT_FOUND
        )
        val flexibleWidth = attributes.getDimensionPixelSize(
            R.styleable.HeliumBannerAd_heliumBannerFlexibleWidth, 0
        )
        val flexibleHeight = attributes.getDimensionPixelSize(
            R.styleable.HeliumBannerAd_heliumBannerFlexibleHeight, 0
        )

        attributes.recycle()
        return HeliumBannerAttributes(location, sizeInLayout, flexibleWidth, flexibleHeight)
    }

    private data class HeliumBannerAttributes(
        val placementName: String?,
        val size: Int,
        val flexibleWidth: Int,
        val flexibleHeight: Int
    )
}
