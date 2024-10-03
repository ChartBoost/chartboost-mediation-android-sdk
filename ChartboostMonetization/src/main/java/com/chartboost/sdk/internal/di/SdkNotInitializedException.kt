package com.chartboost.sdk.internal.di

class ChartboostNotInitializedException :
    RuntimeException("Chartboost SDK is not initialized, call Chartboost.initWithAppId first")
