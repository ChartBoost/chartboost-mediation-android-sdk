package com.chartboost.sdk.internal.di

import android.content.Context
import com.chartboost.sdk.internal.clickthrough.IntentResolver

internal fun getApplicationContext(): Context = ChartboostDependencyContainer.androidComponent.context.applicationContext

internal fun getIntentResolver(): IntentResolver = ChartboostDependencyContainer.applicationComponent.intentResolver
