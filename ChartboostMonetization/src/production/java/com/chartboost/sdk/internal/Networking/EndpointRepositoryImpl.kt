package com.chartboost.sdk.internal.Networking

import android.content.Context
import com.chartboost.sdk.internal.Model.SdkConfiguration

fun endpointRepository(context: Context? = null): EndpointRepository = error("Function is not available")

internal class EndpointRepositoryImpl(sdkConfiguration: SdkConfiguration) : EndpointRepositoryBase(sdkConfiguration)
