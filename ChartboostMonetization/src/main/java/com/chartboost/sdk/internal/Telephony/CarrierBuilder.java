package com.chartboost.sdk.internal.Telephony;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.chartboost.sdk.internal.logging.Logger;

public class CarrierBuilder {

    public Carrier build(Context context) {
        Carrier carrier = null;
        if (isPhoneStatePermissionNotGranted(context)) {
            Logger.d("Permission READ_PHONE_STATE not granted", null);
            return null;
        }

        TelephonyManager telephonyManager = getPhoneManager(context);
        if (isSimReady(telephonyManager)) {
            String simOperator = telephonyManager.getSimOperator();
            String mccCode = null;
            String mncCode = null;
            if (!TextUtils.isEmpty(simOperator)) {
                mccCode = simOperator.substring(0, 3);
                mncCode = simOperator.substring(3);
            }
            String networkOperatorName = telephonyManager.getNetworkOperatorName();
            String networkCountryIso = telephonyManager.getNetworkCountryIso();
            int phoneType = telephonyManager.getPhoneType();
            carrier = new Carrier(simOperator, mccCode, mncCode, networkOperatorName, networkCountryIso, phoneType);
        }
        return carrier;
    }

    private TelephonyManager getPhoneManager(Context context) {
        if (context != null) {
            try {
                return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            } catch (Exception e) {
                Logger.e("Unable to retrieve TELEPHONY_SERVICE", e);
            }
        }
        return null;
    }

    private boolean isSimReady(TelephonyManager telephonyManager) {
        if (telephonyManager != null) {
            return telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE && telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
        }
        return false;
    }

    private boolean isPhoneStatePermissionNotGranted(Context context) {
        if (context != null) {
            return ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) == PERMISSION_DENIED;
        }
        return false;
    }
}
