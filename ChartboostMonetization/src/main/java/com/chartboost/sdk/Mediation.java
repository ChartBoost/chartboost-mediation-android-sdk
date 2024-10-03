package com.chartboost.sdk;

import com.chartboost.sdk.internal.Model.MediationBodyFields;

/**
 * @suppress
 */
public class Mediation {

    public final String mediationType;
    public final String libraryVersion;
    public final String adapterVersion;

    public Mediation(
         String mediationType,
         String libraryVersion,
         String adapterVersion
    )  {
        this.mediationType = sanitizeMediation(mediationType);
        this.libraryVersion = libraryVersion;
        this.adapterVersion = adapterVersion;
    }

    private String sanitizeMediation(String mediationType) {
        if(mediationType == null) return null;
        String sanitized = mediationType.replace(" ", "_");
        if(sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return sanitized;
    }

    public MediationBodyFields toMediationBodyFields() {
        if(mediationType == null) return null;
        String lVersion = (libraryVersion != null) ? libraryVersion : "";
        String aVersion = (adapterVersion != null) ? adapterVersion : "";
        return new MediationBodyFields(getMediationName(), lVersion, aVersion);
    }

    private String getMediationName() {
        if(libraryVersion == null || libraryVersion.isEmpty()) return mediationType;
        return mediationType + " "+ libraryVersion;
    }

}
