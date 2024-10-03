package com.chartboost.sdk.internal.Networking

import android.os.Build
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.Networking.NetworkHelper.isForceSDKToAcceptAllSSLCertsEnabled
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

class CBSSLSocketFactory {
    companion object {
        @JvmStatic
        fun getSSLSocketFactory(): SSLSocketFactory {
            val sslContext =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    SSLContext.getInstance("TLSv1.3")
                } else {
                    SSLContext.getInstance("TLSv1.2")
                }
            if (isForceSDKToAcceptAllSSLCertsEnabled()) {
                // This should never happen in production, only for wiremock and automated tests
                sslContext.init(null, arrayOf(SandboxBridgeSettings.trustManager), null)
            } else {
                sslContext.init(null, null, null)
            }

            sslContext.createSSLEngine()
            return sslContext.socketFactory
        }
    }
}
