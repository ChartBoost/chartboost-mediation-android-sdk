package com.chartboost.sdk.internal.initialization

import org.json.JSONObject

interface ConfigRequestCallback {
    fun onConfigRequestSuccess(configJson: JSONObject)

    fun onConfigRequestFailure(errorMsg: String)
}
