package com.chartboost.sdk.internal.Networking;

import com.chartboost.sdk.internal.adType.AdType;

public class AdParameters {

    public final AdType adType;
    public final Integer height;
    public final Integer width;
    public final String location;
    public final int impDepth;

    public AdParameters(
            AdType adType,
            Integer height,
            Integer width,
            String location,
            int impDepth
    ) {
        this.adType = adType;
        this.height = height;
        this.width = width;
        this.location = location;
        this.impDepth = impDepth;
    }
}
