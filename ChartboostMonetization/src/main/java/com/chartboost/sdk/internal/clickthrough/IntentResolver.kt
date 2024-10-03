package com.chartboost.sdk.internal.clickthrough

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import com.chartboost.sdk.internal.logging.Logger

/**
 * For different use cases we will have to check if application to open scheme are valid
 * and available in the resources
 */
internal class IntentResolver(
    private val packageManager: PackageManager,
    private val intentFactory: () -> Intent = { Intent(Intent.ACTION_VIEW) },
) {
    /**
     * This is only used in onClick for AdGet url.
     */
    fun canOpenDeeplink(deepLink: String?): Boolean {
        if (deepLink.isNullOrEmpty()) return false
        return try {
            getActionsInfo(
                actionViewIntentForURL(deepLink),
            ).isNotEmpty()
        } catch (e: Exception) {
            Logger.e("Cannot open URL", e)
            false
        }
    }

    private fun getActionsInfo(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryActivities(intent, ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    private fun queryActivities(
        intent: Intent,
        flags: ResolveInfoFlags,
    ): List<ResolveInfo> = packageManager.queryIntentActivities(intent, flags)

    private fun actionViewIntentForURL(url: String): Intent =
        intentFactory().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse(url)
        }
}
