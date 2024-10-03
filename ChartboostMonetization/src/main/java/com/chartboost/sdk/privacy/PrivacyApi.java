package com.chartboost.sdk.privacy;

import static com.chartboost.sdk.privacy.model.COPPA.COPPA_STANDARD;

import com.chartboost.sdk.internal.Model.PrivacyBodyFields;
import com.chartboost.sdk.internal.Model.SdkConfiguration;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;
import com.chartboost.sdk.privacy.usecase.GetDataUseConsentUseCase;
import com.chartboost.sdk.privacy.usecase.GetPrivacyListAsJsonUseCase;
import com.chartboost.sdk.privacy.usecase.GetWhitelistedPrivacyStandardsListUseCase;
import com.chartboost.sdk.privacy.usecase.PutDataUseConsentUseCase;
import com.chartboost.sdk.privacy.usecase.RemoveDataUseConsentUseCase;

import org.json.JSONObject;

import java.util.List;

/**
 * Contains all the use cases about privacy and consent
 */
public class PrivacyApi {

    private final static String UNKNOWN = "-1";

    private final PutDataUseConsentUseCase putDataUseConsentUseCase;
    private final GetDataUseConsentUseCase getDataUseConsentUseCase;
    private final RemoveDataUseConsentUseCase removeDataUseConsentUseCase;
    private final GetPrivacyListAsJsonUseCase getPrivacyListAsJsonUseCase;
    private final GetWhitelistedPrivacyStandardsListUseCase getWhitelistedPrivacyStandardsListUseCase;
    private final TCFv2 tcfv2;
    private final GPP gpp;
    private final String gppSid;

    /**
     * PrivacyStandardsConfig is only set after SDK is initialised
     */
    private SdkConfiguration.PrivacyStandardsConfig config;

    public PrivacyApi(PutDataUseConsentUseCase putDataUseConsentUseCase,
                      GetDataUseConsentUseCase getDataUseConsentUseCase,
                      RemoveDataUseConsentUseCase removeDataUseConsentUseCase,
                      GetPrivacyListAsJsonUseCase getPrivacyListAsJsonUseCase,
                      GetWhitelistedPrivacyStandardsListUseCase getWhitelistedPrivacyStandardsListUseCase,
                      TCFv2 tcfv2,
                      GPP gpp,
                      String gppSid
    ) {
        this.putDataUseConsentUseCase = putDataUseConsentUseCase;
        this.getDataUseConsentUseCase = getDataUseConsentUseCase;
        this.removeDataUseConsentUseCase = removeDataUseConsentUseCase;
        this.getPrivacyListAsJsonUseCase = getPrivacyListAsJsonUseCase;
        this.getWhitelistedPrivacyStandardsListUseCase = getWhitelistedPrivacyStandardsListUseCase;
        this.tcfv2 = tcfv2;
        this.gpp = gpp;
        this.gppSid = gppSid;
    }

    public void setPrivacyConfig(SdkConfiguration.PrivacyStandardsConfig config) {
        this.config = config;
    }

    public void putPrivacyStandard(DataUseConsent dataUseConsent) {
        if (putDataUseConsentUseCase != null) {
            putDataUseConsentUseCase.execute(dataUseConsent);
        }
    }

    public DataUseConsent getPrivacyStandard(String privacyStandard) {
        if (getDataUseConsentUseCase != null) {
            return getDataUseConsentUseCase.execute(privacyStandard);
        }
        return null;
    }

    public void removePrivacyStandard(String privacyStandard) {
        if (removeDataUseConsentUseCase != null) {
            removeDataUseConsentUseCase.execute(privacyStandard);
        }
    }

    public JSONObject getPrivacyListAsJson() {
        List<DataUseConsent> data = getWhitelistedPrivacyStandardsList();
        if (getPrivacyListAsJsonUseCase != null && data != null) {
            return getPrivacyListAsJsonUseCase.execute(data);
        }
        return null;
    }

    public List<DataUseConsent> getWhitelistedPrivacyStandardsList() {
        if (getWhitelistedPrivacyStandardsListUseCase != null && config != null) {
            return getWhitelistedPrivacyStandardsListUseCase.execute(config);
        }
        return null;
    }

    public String getPIDataUseConsent() {
        DataUseConsent gdprConsent = getDataUseConsentUseCase.execute(GDPR.GDPR_STANDARD);
        if (gdprConsent == null) {
            return UNKNOWN;
        }
        return (String) gdprConsent.getConsent();
    }

    public int getOpenRtbConsent() {
        return getPIDataUseConsent().equals(GDPR.GDPR_CONSENT.BEHAVIORAL.getValue()) ? 1 : 0;
    }

    public int getOpenRtbGdpr() {
        return getPIDataUseConsent().equals(UNKNOWN) ? 0 : 1;
    }

    public Integer getOpenRtbCoppa() {
        COPPA coppa = (COPPA) getPrivacyStandard(COPPA_STANDARD);
        if (coppa != null) {
            if (coppa.getConsent()) {
                return 1;
            } else {
                return 0;
            }
        }
        return null;
    }

    public PrivacyBodyFields toPrivacyBodyFields() {
        return new PrivacyBodyFields(
                getOpenRtbConsent(),
                getWhitelistedPrivacyStandardsList(),
                getOpenRtbGdpr(),
                getOpenRtbCoppa(),
                getPrivacyListAsJson(),
                getPIDataUseConsent(),
                tcfv2.getTCFString(),
                gpp.getGppString(),
                gpp.getGppSid()
        );
    }
}
