package com.chartboost.sdk.internal.di

import android.app.Application
import android.content.Context
import com.chartboost.sdk.internal.logging.Logger

internal interface DependencyContainer {
    val appId: String
    val appSignature: String
    val sdkComponent: SdkComponent
    val renderComponent: RenderComponent
    val initialized: Boolean
    val started: Boolean
    val androidComponent: AndroidComponent
    val applicationComponent: ApplicationComponent
    val trackerComponent: TrackerComponent
    val privacyComponent: PrivacyComponent
    val executorComponent: ExecutorComponent
    val openMeasurementComponent: OpenMeasurementComponent
    val impressionComponent: ImpressionComponent

    fun initialize(context: Context)

    // TODO This has nothing to do with DI and should not belong here
    fun start(
        appId: String,
        appSignature: String,
    )
}

/**
 * Internal implementation of the dependency container. This class is used to isolate unit tests.
 * This class is extended by the object ChartboostDependencyContainer and it shouldn't be instantiated
 * from anywhere else.
 */
internal class DependencyContainerInternalImpl : DependencyContainer {
    private lateinit var _appId: String
    private lateinit var _appSignature: String

    // Don't pass it as a dependency to the components, pass application instead
    private lateinit var unsafeApplication: Application

    private val application: Application
        get() {
            if (!::unsafeApplication.isInitialized) {
                try {
                    throw ChartboostNotInitializedException()
                } catch (e: Exception) {
                    Logger.e("Missing application. Cannot start Chartboost SDK", e)
                }
            }
            return unsafeApplication
        }

    override val initialized: Boolean
        get() = ::unsafeApplication.isInitialized

    override val started: Boolean
        get() = (::_appId.isInitialized) && (::_appSignature.isInitialized)

    override val appId: String
        get() = if (::_appId.isInitialized) _appId else ""

    override val appSignature: String
        get() = if (::_appSignature.isInitialized) _appSignature else ""

    override val androidComponent: AndroidComponent by lazy {
        AndroidModule(
            application.applicationContext,
            application,
        )
    }

    override val applicationComponent: ApplicationComponent by lazy {
        ApplicationModule(
            androidComponent,
            executorComponent,
            privacyComponent,
            trackerComponent = trackerComponent,
        )
    }

    override val privacyComponent: PrivacyComponent by lazy {
        PrivacyModule(androidComponent, trackerComponent)
    }

    override val executorComponent: ExecutorComponent by lazy { ExecutorModule() }

    override val openMeasurementComponent: OpenMeasurementComponent by lazy {
        OpenMeasurementModule(
            androidComponent,
            applicationComponent,
        )
    }

    override val impressionComponent: ImpressionComponent by lazy {
        ImpressionComponentImpl()
    }

    override val trackerComponent: TrackerComponent by lazy {
        TrackerModule(
            lazy { androidComponent },
            lazy { applicationComponent },
            lazy { privacyComponent.privacyApi },
        )
    }

    override val sdkComponent: SdkComponent by lazy {
        SdkModule(
            androidComponent,
            executorComponent,
            applicationComponent,
            openMeasurementComponent,
            trackerComponent,
        )
    }

    override val renderComponent: RenderComponent by lazy {
        RenderModule(
            androidComponent,
            trackerComponent,
        )
    }

    override fun initialize(context: Context) {
        unsafeApplication = context.applicationContext as Application
    }

    override fun start(
        appId: String,
        appSignature: String,
    ) {
        _appId = appId
        _appSignature = appSignature
    }
}

internal object ChartboostDependencyContainer : DependencyContainer by DependencyContainerInternalImpl()
