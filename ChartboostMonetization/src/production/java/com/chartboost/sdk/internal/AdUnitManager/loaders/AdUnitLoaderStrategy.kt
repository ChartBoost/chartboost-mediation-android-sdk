package com.chartboost.sdk.internal.AdUnitManager.loaders

import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest

internal object AdUnitLoaderStrategy {
    fun validateFormat(
        appRequest: AppRequest,
        params: LoadParams,
        loadOpenRTBAd: (appRequest: AppRequest, params: LoadParams) -> Unit,
        loadAdGet: (appRequest: AppRequest, params: LoadParams) -> Unit,
    ): Pair<(appRequest: AppRequest, params: LoadParams) -> Unit, LoadParams> {
        if (appRequest.bidResponse != null) {
            return Pair(loadOpenRTBAd, params)
        } else {
            return Pair(loadAdGet, params)
        }
    }
}
