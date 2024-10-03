package com.chartboost.sdk.internal.View

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import android.widget.RelativeLayout
import com.chartboost.sdk.R
import com.chartboost.sdk.internal.AdUnitManager.data.InfoIcon
import com.chartboost.sdk.internal.Networking.CBImageDownloader
import com.chartboost.sdk.internal.WebView.CBHtmlWebView
import com.chartboost.sdk.internal.WebView.CBHtmlWebViewClient
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.WebView.CustomWebViewInterface
import com.chartboost.sdk.internal.WebView.SingleClickGestureDetector
import com.chartboost.sdk.internal.clickthrough.CBUrl
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.tracking.EventTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
internal class HtmlWebViewBase(
    context: Context,
    baseUrl: String,
    html: String,
    private val infoIcon: InfoIcon,
    eventTracker: EventTracker,
    private val callback: CustomWebViewInterface,
    private val impressionInterface: ImpressionInterface,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    cbWebViewFactory: (Context) -> CBWebView = { CBHtmlWebView(it) },
    private val cbImageDownloader: CBImageDownloader = CBImageDownloader(),
) : CommonWebViewBase(
        context,
        html,
        callback,
        baseUrl,
        eventTracker,
        cbWebViewFactory = cbWebViewFactory,
        cbWebViewClientFactory = { cb, et ->
            CBHtmlWebViewClient(
                impressionInterface = impressionInterface,
                gestureDetector = SingleClickGestureDetector(context),
                callback = cb,
                eventTracker = et,
            )
        },
    ) {
    private var infoIconDownloadJob: Job? = null

    init {
        this.addView(webViewContainer)
        callback.onWebViewInit()
        callback.onRegisterWebViewTimeout()
    }

    internal fun makeInfoIcon(container: RelativeLayout) {
        // Define layout parameters including size, margin, and position
        val imageViewLayoutParams =
            LayoutParams(
                dpToPx(infoIcon.size.width),
                dpToPx(infoIcon.size.height),
            ).apply {
                when (infoIcon.position) {
                    InfoIcon.Position.TOP_LEFT -> {
                        addRule(ALIGN_PARENT_TOP)
                        addRule(ALIGN_PARENT_LEFT)
                    }

                    InfoIcon.Position.TOP_RIGHT -> {
                        addRule(ALIGN_PARENT_TOP)
                        addRule(ALIGN_PARENT_RIGHT)
                    }

                    InfoIcon.Position.BOTTOM_LEFT -> {
                        addRule(ALIGN_PARENT_BOTTOM)
                        addRule(ALIGN_PARENT_LEFT)
                    }

                    InfoIcon.Position.BOTTOM_RIGHT -> {
                        addRule(ALIGN_PARENT_BOTTOM)
                        addRule(ALIGN_PARENT_RIGHT)
                    }
                }

                // Apply margins
                setMargins(
                    dpToPx(infoIcon.margin.width),
                    dpToPx(infoIcon.margin.height),
                    dpToPx(infoIcon.margin.width),
                    dpToPx(infoIcon.margin.height),
                )
            }

        val imageView =
            ImageView(context).apply {
                // Set default drawable
                setImageResource(R.drawable.cb_info_icon)

                // Set click listener to open clickThroughUrl
                setOnClickListener { _ ->
                    impressionInterface.onOpenNonClickURL(
                        CBUrl(infoIcon.clickthroughUrl, false),
                    )
                }

                // start with it not visible so that we don't accidentally show the default icon before the
                // real one finishes loading
                visibility = GONE
            }

        // Load the image
        infoIconDownloadJob =
            CoroutineScope(dispatcher).launch {
                cbImageDownloader.downloadImage(infoIcon.imageUrl)?.let { bitmap ->
                    imageView.setImageBitmap(bitmap)
                }
                imageView.visibility = VISIBLE
            }.also { job ->
                job.invokeOnCompletion {
                    // null out the infoIconDownloadJob when complete
                    infoIconDownloadJob = null
                }
            }

        container.addView(imageView, imageViewLayoutParams)
        callback.onRegisterFriendlyWebViewObstruction(imageView)
    }

    override fun destroyWebview() {
        infoIconDownloadJob?.cancel()
        infoIconDownloadJob = null
        super.destroyWebview()
    }

    private fun dpToPx(dp: Double): Int {
        return (context?.resources?.displayMetrics?.density?.let { dp * it } ?: dp).roundToInt()
    }
}
