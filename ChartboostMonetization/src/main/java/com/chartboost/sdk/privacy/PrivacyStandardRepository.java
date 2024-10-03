package com.chartboost.sdk.privacy;

import static com.chartboost.sdk.privacy.model.CCPA.CCPA_STANDARD;
import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;
import static com.chartboost.sdk.privacy.model.GDPR.GDPR_STANDARD;
import static com.chartboost.sdk.privacy.model.LGPD.LGPD_STANDARD;

import android.content.SharedPreferences;

import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.Custom;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;
import com.chartboost.sdk.privacy.model.LGPD;
import com.chartboost.sdk.tracking.CriticalEvent;
import com.chartboost.sdk.tracking.EventTracker;
import com.chartboost.sdk.tracking.TrackingEventName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class PrivacyStandardRepository {

    public static final String PRIVACY_STANDARD_KEY = "privacyStandard";
    public static final String CONSENT_KEY = "consent";
    public static final String PRIVACY_STANDARDS_KEY = "privacy_standards";

    private final HashMap<String, DataUseConsent> privacyStandards;
    private final SharedPreferences sharedPreferences;
    private final EventTracker eventTracker;

    public PrivacyStandardRepository(
            SharedPreferences sharedPrefs,
            EventTracker eventTracker
    ) {
        privacyStandards = new HashMap<>();
        sharedPreferences = sharedPrefs;
        this.eventTracker = eventTracker;
        load();
    }

    /**
     * Return HashMap of consents
     *
     * @return
     */
    public HashMap<String, DataUseConsent> getMap() {
        return privacyStandards;
    }

    /**
     * Get GDPR consent
     *
     * @return
     */
    public DataUseConsent getGDPR() {
        return privacyStandards.get(GDPR_STANDARD);
    }

    public void put(DataUseConsent consent) {
        Logger.d("Added privacy standard: " + consent.getPrivacyStandard() + " with consent: " + consent.getConsent(), null);
        privacyStandards.put(consent.getPrivacyStandard(), consent);
        save();
    }

    public void remove(String privacyStandard) {
        privacyStandards.remove(privacyStandard);
        save();
    }
    
    private void save() {
        if (sharedPreferences != null) {
            JSONArray jsonarray = new JSONArray();
            for (DataUseConsent standard : privacyStandards.values()) {
                jsonarray.put(privacyStandardToJson(standard));
            }
            saveArrayInPrefs(sharedPreferences, jsonarray);
        }
    }

    private void load() {
        if (sharedPreferences != null) {
            String json = sharedPreferences.getString(PRIVACY_STANDARDS_KEY, "");
            if (!json.isEmpty()) {
                try {
                    JSONArray array = new JSONArray(json);
                    int arrayLength = array.length();
                    for (int i = 0; i < arrayLength; i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String privacyStandardName = obj.getString(PRIVACY_STANDARD_KEY);
                        String consent = obj.getString(CONSENT_KEY);
                        DataUseConsent standard = null;
                        switch (privacyStandardName) {
                            case GDPR_STANDARD:
                                //Due to the legacy gdpr values which also includes -1 or UNKNOWN value
                                //it is required to manually parse GDPR object rather just use Custom
                                //which will throw an exception if GDPR name is used
                                if (GDPR.GDPR_CONSENT.BEHAVIORAL.getValue().equals(consent)) {
                                    standard = new GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL);
                                } else if (GDPR.GDPR_CONSENT.NON_BEHAVIORAL.getValue().equals(consent)) {
                                    standard = new GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL);
                                }
                                break;
                            case CCPA_STANDARD:
                                if (CCPA.CCPA_CONSENT.OPT_IN_SALE.getValue().equals(consent)) {
                                    standard = new CCPA(CCPA.CCPA_CONSENT.OPT_IN_SALE);
                                } else if (CCPA.CCPA_CONSENT.OPT_OUT_SALE.getValue().equals(consent)) {
                                    standard = new CCPA(CCPA.CCPA_CONSENT.OPT_OUT_SALE);
                                }
                                break;
                            case COPPA_STANDARD:
                                standard = new COPPA(obj.getBoolean(CONSENT_KEY));
                                break;
                            case LGPD_STANDARD:
                                standard = new LGPD(obj.getBoolean(CONSENT_KEY));
                                break;
                            default:
                                standard = new Custom(
                                        obj.getString(PRIVACY_STANDARD_KEY),
                                        obj.getString(CONSENT_KEY));
                                break;
                        }

                        if (standard != null) {
                            privacyStandards.put(standard.getPrivacyStandard(), standard);
                        } else {
                            trackConsentDataReadingError(privacyStandardName);
                            Logger.d("Failed to load consent: " + privacyStandardName, null);
                        }
                    }
                } catch (JSONException e) {
                    trackConsentDecodingError(e);
                    e.printStackTrace();
                }
            }
        }
    }

    private void trackConsentDataReadingError(String privacyStandardName) {
        eventTracker.track(
                CriticalEvent.instance(
                        TrackingEventName.Consent.PERSISTED_DATA_READING_ERROR,
                        privacyStandardName,
                        "",
                        ""
                )
        );
    }

    private void trackConsentDecodingError(JSONException e) {
        eventTracker.track(
                CriticalEvent.instance(
                        TrackingEventName.Consent.DECODING_ERROR,
                        e.getMessage(),
                        "",
                        ""
                )
        );
    }

    private void saveArrayInPrefs(SharedPreferences sharedPrefs, JSONArray standardsJsonArray) {
        if (sharedPrefs != null && standardsJsonArray != null) {
            sharedPrefs.edit()
                    .putString(PRIVACY_STANDARDS_KEY, standardsJsonArray.toString())
                    .apply();
        }
    }

    private JSONObject privacyStandardToJson(DataUseConsent standard) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PrivacyStandardRepository.PRIVACY_STANDARD_KEY, standard.getPrivacyStandard());
            jsonObject.put(PrivacyStandardRepository.CONSENT_KEY, standard.getConsent());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
