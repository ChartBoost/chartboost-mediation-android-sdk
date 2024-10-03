package com.chartboost.sdk.internal.Networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Network state checker to monitor internet connectivity.
 *
 * @param context The application context.
 */
internal class NetworkStateChecker(context: Context) {
    private var isConnected: Boolean = false

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onAvailable(network: Network) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                isConnected =
                    networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }

            override fun onLost(network: Network) {
                isConnected = false
            }
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun startChecking() {
        val networkRequest =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Check initial network state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            isConnected =
                networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            // For API levels below 23, use alternative method
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            isConnected = activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }

    internal fun stopChecking() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    internal fun isInternetAvailable(): Boolean {
        return isConnected
    }
}
