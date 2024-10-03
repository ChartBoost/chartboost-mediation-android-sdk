package com.chartboost.sdk.internal.Telephony;

public class Carrier {

    private final String mSimOperator;
    private final String mMccCode;
    private final String mMncCode;
    private final String mNetworkOperatorName;
    private final String mNetworkCountryIso;
    private final int mPhoneType;

    public Carrier(String simOperator, String mccCode, String mncCode, String networkOperatorName, String networkCountryIso, int phoneType) {
        mSimOperator = simOperator;
        mMccCode = mccCode;
        mMncCode = mncCode;
        mNetworkOperatorName = networkOperatorName;
        mNetworkCountryIso = networkCountryIso;
        mPhoneType = phoneType;
    }

    public String getSimOperator() {
        return mSimOperator;
    }

    public String getMccCode() {
        return mMccCode;
    }

    public String getMncCode() {
        return mMncCode;
    }

    public String getNetworkOperatorName() {
        return mNetworkOperatorName;
    }

    public String getNetworkCountryIso() {
        return mNetworkCountryIso;
    }

    public int getPhoneType() {
        return mPhoneType;
    }
}