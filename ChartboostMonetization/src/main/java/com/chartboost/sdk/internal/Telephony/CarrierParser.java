package com.chartboost.sdk.internal.Telephony;

import com.chartboost.sdk.internal.Libraries.CBConstants;
import com.chartboost.sdk.internal.Libraries.CBJSON;

import org.json.JSONObject;

public class CarrierParser {

    public JSONObject parseCarrierToJsonObject(Carrier carrier) {
        if (carrier == null) {
            return new JSONObject();
        }

        return CBJSON.jsonObject(
                CBJSON.JKV(CBConstants.REQUEST_PARAM_CARRIER_NAME, carrier.getNetworkOperatorName()),
                CBJSON.JKV(CBConstants.REQUEST_PARAM_MCC, carrier.getMccCode()),
                CBJSON.JKV(CBConstants.REQUEST_PARAM_MNC, carrier.getMncCode()),
                CBJSON.JKV(CBConstants.REQUEST_PARAM_ISO, carrier.getNetworkCountryIso()),
                CBJSON.JKV(CBConstants.REQUEST_PARAM_PHONE_TYPE, carrier.getPhoneType()));
    }
}
