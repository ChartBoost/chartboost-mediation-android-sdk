package com.chartboost.sdk.internal.initialization

import android.content.Context
import android.content.Intent
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.logging.Logger
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller

internal class ProviderInstallerHelper(
    private val context: Context,
    private val uiPoster: UiPoster,
) : ProviderInstaller.ProviderInstallListener {
    /**
     * Google Play will install latest security updated for Provider
     * https://developer.android.com/training/articles/security-gms-provider.html
     * We should wait for this to finish to execute first network call but in our case it doesn't
     * make sense as we cannot handle the fix and waiting could result in sdk timeout when used
     * in mediation.
     */
    fun installProviderIfPossible() {
        if (isGooglePlayServicesAvailable()) {
            uiPoster {
                try {
                    // Must be called on the UI thread
                    ProviderInstaller.installIfNeededAsync(
                        context,
                        this,
                    )
                } catch (e: Exception) {
                    Logger.e("ProviderInstaller", e)
                }
            }
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        try {
            if (GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS
            ) {
                return false
            }
        } catch (e: Exception) {
            Logger.e("GoogleApiAvailability error", e)
            return false
        }
        return true
    }

    override fun onProviderInstallFailed(
        errorCode: Int,
        recoveryIntent: Intent?,
    ) {
        Logger.w(
            "ProviderInstaller onProviderInstallFailed: $errorCode " +
                "ProviderInstaller is unable to install an updated Provider," +
                " your device's security provider might be vulnerable to known exploits." +
                " Your app should behave as if all HTTP communication is unencrypted.",
        )
    }

    override fun onProviderInstalled() {
        Logger.e("ProviderInstaller onProviderInstalled")
    }
}
