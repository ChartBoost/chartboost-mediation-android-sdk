package com.chartboost.sdk.legacy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.internal.logging.Logger;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

public final class CBConfig { // MO-1408 make fields nonstatic

    /**
     * Update the configuration from the server.
     * <p>
     * Changes nothing if there's an error processing the configuration json.
     * <p>
     * Returns true if successful.
     */

    public static boolean updateConfig(AtomicReference<SdkConfiguration> sdkConfig,
                                       JSONObject config) {
        try {
            sdkConfig.set(new SdkConfiguration(config));
            return true;
        } catch (Exception e) {
            Logger.e("updateConfig: " + e, null);
        }
        return false;
    }

    public static boolean validatePermissions(Context context) {
        try {
            if (context == null)
                throw new RuntimeException("Invalid activity context passed during intitalization");
            int isNetworkStatePermissionAvailable;
            int isInternetPermissionAvailable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isNetworkStatePermissionAvailable = context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);
                isInternetPermissionAvailable = context.checkSelfPermission(Manifest.permission.INTERNET);
            } else {
                isNetworkStatePermissionAvailable = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE);
                isInternetPermissionAvailable = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET);
            }

            boolean isInternetPermissionRevoked = isInternetPermissionAvailable != PackageManager.PERMISSION_GRANTED;
            boolean isNetworkStatePermissionRevoked = isNetworkStatePermissionAvailable != PackageManager.PERMISSION_GRANTED;

            if (isInternetPermissionRevoked)
                throw new RuntimeException("Please add the permission : android.permission.INTERNET in your android manifest.xml");
            if (isNetworkStatePermissionRevoked)
                throw new RuntimeException("Please add the permission : android.permission.ACCESS_NETWORK_STATE in your android manifest.xml");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

