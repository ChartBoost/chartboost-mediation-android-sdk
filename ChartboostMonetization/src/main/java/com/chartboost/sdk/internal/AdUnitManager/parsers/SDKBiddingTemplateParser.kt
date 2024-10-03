package com.chartboost.sdk.internal.AdUnitManager.parsers

import com.chartboost.sdk.internal.logging.Logger
import java.io.File

private const val SDK_BIDDING_PARAMS = "\"{% params %}\""
private const val SDK_BIDDING_ADM = "{% adm %}"

class SDKBiddingTemplateParser {
    fun parse(
        htmlFile: File,
        params: String,
        adm: String,
    ): String? {
        return try {
            htmlFile.readText(Charsets.UTF_8)
                .replace(SDK_BIDDING_PARAMS, params)
                .replace(SDK_BIDDING_ADM, adm)
        } catch (e: Exception) {
            Logger.e("Parse sdk bidding template exception", e)
            null
        }
    }
}
