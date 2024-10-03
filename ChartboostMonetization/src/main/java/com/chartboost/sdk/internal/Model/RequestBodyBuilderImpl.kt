package com.chartboost.sdk.internal.Model

import android.content.Context
import android.content.SharedPreferences
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Telephony.CarrierBuilder
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.tracking.Session
import java.util.concurrent.atomic.AtomicReference

interface RequestBodyBuilder {
    fun build(): RequestBodyFields
}

internal class RequestBodyBuilderImpl(
    val context: Context,
    val identity: CBIdentity,
    val reachability: CBReachability,
    val sdkConfig: AtomicReference<SdkConfiguration>,
    val sharedPreferences: SharedPreferences,
    val timeSource: TimeSource,
    private val carrierBuilder: CarrierBuilder,
    val session: Session,
    val privacyApi: PrivacyApi,
    val mediation: Mediation?,
    private val deviceBodyFieldsFactory: DeviceBodyFieldsFactory,
) : RequestBodyBuilder {
    override fun build(): RequestBodyFields {
        return RequestBodyFields(
            ChartboostDependencyContainer.appId,
            ChartboostDependencyContainer.appSignature,
            identity.toIdentityBodyFields(),
            reachability.toReachabilityBodyFields(),
            carrierBuilder.build(context),
            session.toSessionBodyFields(),
            timeSource.toBodyFields(),
            privacyApi.toPrivacyBodyFields(),
            sdkConfig.get().toConfigurationBodyFields(),
            deviceBodyFieldsFactory.build(),
            mediation?.toMediationBodyFields(),
        )
    }
}
