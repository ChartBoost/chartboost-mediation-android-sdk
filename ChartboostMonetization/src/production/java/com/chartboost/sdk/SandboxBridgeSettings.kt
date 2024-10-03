package com.chartboost.sdk

import android.annotation.SuppressLint
import javax.net.ssl.X509TrustManager

object SandboxBridgeSettings {
    // Don't make this const, we cannot mock it in the tests!
    @SuppressWarnings
    var isSandboxMode = false

    @JvmStatic
    fun setAdId(id: String?) {}

    @JvmStatic
    fun setUUID(id: String?) {}

    @JvmStatic
    val header: String = ""

    @JvmStatic
    val customHeader: String? = null

    @JvmStatic
    fun sendLogsToSandbox(log: String?) {}

    // ONLY FOR AUTOMATED TESTS - won't be compiled in production flavour
    @get:SuppressLint("CustomX509TrustManager")
    @JvmStatic
    val trustManager: X509TrustManager? = null
}
