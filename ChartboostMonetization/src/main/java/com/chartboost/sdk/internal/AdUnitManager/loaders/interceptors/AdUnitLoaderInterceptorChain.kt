package com.chartboost.sdk.internal.AdUnitManager.loaders.interceptors

import org.json.JSONObject

class AdUnitLoaderInterceptorChain : AbstractInterceptor<JSONObject> {
    private val interceptors = mutableListOf<AbstractInterceptor<JSONObject>>()

    override fun intercept(response: JSONObject): JSONObject {
        var modifiedResponse = response
        for (interceptor in interceptors) {
            modifiedResponse = interceptor.intercept(modifiedResponse)
        }

        return modifiedResponse
    }
}
