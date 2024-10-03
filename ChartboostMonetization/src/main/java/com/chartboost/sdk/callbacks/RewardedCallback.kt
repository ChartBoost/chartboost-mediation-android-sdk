package com.chartboost.sdk.callbacks

import com.chartboost.sdk.events.RewardEvent

/**
 * RewardedCallback
 * Provides methods to receive notifications related to a rewarded ad's behavior.
 * In a typical integration, you would implement onAdRequestedToShow, onAdShown, and onAdDismiss,
 * pausing and resuming ongoing processes (e.g., gameplay, video) accordingly.
 * The method onRewardEarned needs to be implemented to be notified when the user earns a reward.
 */
interface RewardedCallback : DismissibleAdCallback {
    /**
     * Called when a rewarded ad has completed playing.
     * Implement this method to be notified when a reward is earned.
     *
     * @param event A reward event with information related to the ad and the reward.
     */
    fun onRewardEarned(event: RewardEvent)
}
