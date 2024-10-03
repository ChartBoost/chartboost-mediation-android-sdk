package com.chartboost.sdk.internal.Model;

import com.chartboost.sdk.internal.impression.CBImpression;

public interface ImpressionCallback {
    void impressionReadyToBeDisplayed(CBImpression impression);

    void impressionError(CBImpression impression, CBError.Impression error);

    void impressionShownFully(CBImpression impression);

    void impressionCloseTriggered(CBImpression impression);

    void impressionClicked(String impressionId);

    void impressionClickedFailed(String impressionId, String url, CBError.Click error);

    void impressionReward(String impressionId, int reward);

    void impressionDismiss(String impressionId);
}
