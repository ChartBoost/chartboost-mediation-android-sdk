package com.chartboost.sdk.internal.di

import com.chartboost.sdk.internal.measurement.OpenMeasurementController
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.measurement.OpenMeasurementSessionBuilder

interface OpenMeasurementComponent {
    val openMeasurementManager: OpenMeasurementManager
    val openMeasurementSessionBuilder: OpenMeasurementSessionBuilder
    val openMeasurementController: OpenMeasurementController
}

internal class OpenMeasurementModule(
    androidComponent: AndroidComponent,
    applicationComponent: ApplicationComponent,
) : OpenMeasurementComponent {
    override val openMeasurementManager: OpenMeasurementManager by lazy {
        OpenMeasurementManager(
            androidComponent.context,
            androidComponent.sharedPrefsHelper,
            androidComponent.resourceLoader,
            applicationComponent.sdkConfig,
        )
    }

    override val openMeasurementSessionBuilder: OpenMeasurementSessionBuilder by lazy {
        OpenMeasurementSessionBuilder()
    }

    override val openMeasurementController: OpenMeasurementController by lazy {
        OpenMeasurementController(openMeasurementManager, openMeasurementSessionBuilder)
    }
}
