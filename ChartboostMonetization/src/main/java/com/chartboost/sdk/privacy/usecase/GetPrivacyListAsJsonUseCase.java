package com.chartboost.sdk.privacy.usecase;

import com.chartboost.sdk.privacy.model.DataUseConsent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class GetPrivacyListAsJsonUseCase {

    public GetPrivacyListAsJsonUseCase() {
    }

    /**
     * Pass whitelisted data use consent list to convert it to JSONObject
     *
     * @param data List of DataUseConsent objects
     *
     * @return JSONObject of privacy standards and consent values
     */
    public JSONObject execute(List<DataUseConsent> data) {
        JSONObject privacyObject = new JSONObject();

        for (DataUseConsent dataUseConsent : data) {
            String standard = dataUseConsent.getPrivacyStandard();
            Object consent = dataUseConsent.getConsent();
            try {
                privacyObject.put(standard, consent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return privacyObject;
    }
}
