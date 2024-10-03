package com.chartboost.sdk.internal.di

import com.chartboost.sdk.internal.AdUnitManager.render.RendererActivityBridge
import com.chartboost.sdk.internal.AdUnitManager.render.RendererActivityBridgeImpl
import com.chartboost.sdk.internal.utils.ImpressionActivityIntentWrapper

internal interface RenderComponent {
    val rendererActivityBridge: RendererActivityBridge
}

internal class RenderModule(
    androidComponent: AndroidComponent,
    trackerComponent: TrackerComponent,
) : RenderComponent {
    override val rendererActivityBridge: RendererActivityBridge by lazy {
        RendererActivityBridgeImpl(
            ImpressionActivityIntentWrapper(androidComponent.context),
            trackerComponent.eventTracker,
        )
    }
}
