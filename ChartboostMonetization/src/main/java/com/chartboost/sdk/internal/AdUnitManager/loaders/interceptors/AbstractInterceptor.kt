package com.chartboost.sdk.internal.AdUnitManager.loaders.interceptors

interface AbstractInterceptor<T> {
    fun intercept(response: T): T
}
