package com.chartboost.sdk.internal.utils

import android.content.Context
import android.content.Intent
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.view.CBImpressionActivity

class ImpressionActivityIntentWrapper(private val context: Context) {
    fun getImpressionActivityIntent(): Intent {
        return Intent(context, CBImpressionActivity::class.java)
            .putExtra(CBConstants.CBIMPRESSIONACTIVITY_IDENTIFIER, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("Cannot start the activity", e)
        }
    }
}
