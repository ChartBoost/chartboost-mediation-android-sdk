package com.chartboost.sdk.internal.AdUnitManager.loaders.interceptors

import org.json.JSONObject

class MockAdGetInterceptor(private val mockResponse: JSONObject) :
    AbstractInterceptor<JSONObject> {
    override fun intercept(response: JSONObject) = mockResponse
}
