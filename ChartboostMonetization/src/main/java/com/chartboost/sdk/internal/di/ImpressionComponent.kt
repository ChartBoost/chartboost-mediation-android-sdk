package com.chartboost.sdk.internal.di

import android.view.ViewGroup
import com.chartboost.sdk.internal.clickthrough.ImpressionClick
import com.chartboost.sdk.internal.clickthrough.ImpressionClickable
import com.chartboost.sdk.internal.impression.CBImpression
import com.chartboost.sdk.internal.impression.ImpressionCompletable
import com.chartboost.sdk.internal.impression.ImpressionComplete
import com.chartboost.sdk.internal.impression.ImpressionDependency
import com.chartboost.sdk.internal.impression.ImpressionDismiss
import com.chartboost.sdk.internal.impression.ImpressionDismissable
import com.chartboost.sdk.internal.impression.ImpressionView
import com.chartboost.sdk.internal.impression.ImpressionViewable

internal interface ImpressionComponent {
    val impressionFactory: (ImpressionDependency, ViewGroup?) -> CBImpression
}

internal class ImpressionComponentImpl : ImpressionComponent {
    override val impressionFactory: (ImpressionDependency, ViewGroup?) -> CBImpression =
        { impressionDependency, viewGroup ->
            CBImpression(
                impressionDependency = impressionDependency,
                impressionClick = impressionClickableFactory(impressionDependency),
                impressionDismiss = impressionDismissableFactory(impressionDependency),
                impressionComplete = impressionCompletableFactory(impressionDependency),
                impressionView = impressionViewableFactory(impressionDependency, viewGroup),
            )
        }

    private val impressionClickableFactory: (ImpressionDependency) -> ImpressionClickable =
        { impressionDependency ->
            with(impressionDependency) {
                ImpressionClick(
                    adUnit,
                    urlResolver,
                    intentResolver,
                    clickRequest,
                    clickTracking,
                    mediaType,
                    impressionClickCallback,
                    openMeasurementImpressionCallback,
                    adUnitRendererImpressionCallback,
                )
            }
        }

    private val impressionDismissableFactory: (ImpressionDependency) -> ImpressionDismissable =
        { impressionDependency ->
            with(impressionDependency) {
                ImpressionDismiss(
                    adUnit,
                    location,
                    adTypeTraits,
                    adUnitRendererImpressionCallback,
                    impressionCallback,
                    appRequest,
                    downloader,
                    openMeasurementImpressionCallback,
                    eventTracker,
                )
            }
        }

    private val impressionCompletableFactory: (ImpressionDependency) -> ImpressionCompletable =
        { impressionDependency ->
            with(impressionDependency) {
                ImpressionComplete(
                    adUnit,
                    adTypeTraits,
                    completeRequest,
                    adUnitRendererImpressionCallback,
                )
            }
        }

    private val impressionViewableFactory: (ImpressionDependency, ViewGroup?) -> ImpressionViewable =
        { impressionDependency, externalView ->
            with(impressionDependency) {
                ImpressionView(
                    appRequest,
                    viewProtocol,
                    downloader,
                    externalView,
                    adUnitRendererImpressionCallback,
                    impressionCallback,
                    impressionClickCallback,
                )
            }
        }
}
