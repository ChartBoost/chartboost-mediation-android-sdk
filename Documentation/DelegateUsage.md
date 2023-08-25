# Delegate Usage

## HeliumInterstitialAdListener

By implementing the `HeliumInterstitialAdListener`, `HeliumRewardedAdListener`, and `HeliumBannerAdListerner` interfaces and providing them to the Helium objects (as seen in the the previous steps), you can get notifications about the success, failure, and other or lifecycle events of Helium Ads.

You can implement the `HeliumInterstitialAdListener` interface to receive notifications about interstitial ads loading, displaying, and closing.

```kotlin
val heliumInterstitialAdListener = object : HeliumInterstitialAdListener {
    override fun didReceiveWinningBid(
        placementName: String,
        bidInfo: HashMap<String, String>
    ) {
        Log.d(TAG, "HeliumInterstitialAd.didReceiveWinningBid for $placementName is $bidInfo")
    }

    override fun didCache(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"HeliumInterstitialAd.didCache failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumInterstitialAd.didCache for placement: $placementName")
        }
    }

    override fun didShow(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumInterstitialAd.didShow failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumInterstitialAd.didShow for placement: $placementName")
        }
    }

    override fun didClick(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumInterstitialAd.didClick failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumInterstitialAd.didClick for placement: $placementName")
        }
    }

    override fun didClose(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumInterstitialAd.didClose failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumInterstitialAd.didClose for placement: $placementName")
        }
    }

    override fun didRecordImpression(placementName: String) {
        Log.d(TAG,"HeliumInterstitialAd.didRecordImpression for placement: $placementName")
    }
}
```
```java
HeliumInterstitialAdListener heliumInterstitialAdListener = new HeliumInterstitialAdListener() {
    @Override
    public void didReceiveWinningBid(@NonNull String placementName, @NonNull HashMap bidInfo) {
        Log.d(TAG, "HeliumInterstitialAd.didReceiveWinningBid for " + placementName + " is " + bidInfo)
    }
 
    @Override
    public void didCache(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumInterstitialAd.didCache failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumInterstitialAd.didCache for placement: " + placementName);
        }
    }
 
    @Override
    public void didShow(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumInterstitialAd.didShow failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumInterstitialAd.didShow for placement: " + placementName);
        }
    }
 
    @Override
    public void didClick(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumInterstitialAd.didClick failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumInterstitialAd.didClick for placement: " + placementName);
        }
    }
 
    @Override
    public void didClose(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumInterstitialAd.didClose failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumInterstitialAd.didClose for placement: " + placementName);
        }
    }

    @Override
    public void didRecordImpression(@NonNull String placementName) {
        Log.d(TAG,"HeliumInterstitialAd.didRecordImpression for placement: " + placementName);
    }
};
```

## HeliumRewardedAdListener

You can implement the `HeliumRewardedAdListener` interface to receive notifications about rewarded ads loading, displaying, and closing.
```kotlin Kotlin
val heliumRewardedAdListener = object : HeliumRewardedAdListener {
    override fun didReceiveWinningBid(
        placementName: String,
        bidInfo: HashMap<String, String>
    ) {
        Log.d(TAG, "HeliumRewardedAd.didReceiveWinningBid for $placementName is $bidInfo")
    }

    override fun didCache(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumRewardedAd.didCache failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumRewardedAd.didCache for placement: $placementName")
        }
    }

    override fun didShow(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumRewardedAd.didShow failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumRewardedAd.didShow for placement: $placementName")
        }
    }

    override fun didClick(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumRewardedAd.didClick failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumRewardedAd.didClick for placement: $placementName")
        }
    }

    override fun didClose(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumRewardedAd.didClose failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumRewardedAd.didClose for placement: $placementName")
        }
    }

    override fun didReceiveReward(placementName: String, reward: String) {
        Log.d(TAG,"HeliumRewardedAd.didReceiveReward for placement: $placementName reward: $reward")
    }

    override fun didRecordImpression(placementName: String) {
        Log.d(TAG,"HeliumRewardedAd.didRecordImpression for placement: $placementName")
    }
}
```
```java Java
HeliumRewardedAdListener heliumRewardedAdListener = new HeliumRewardedAdListener() {
    @Override
    public void didReceiveWinningBid(@NonNull String placementName, @NonNull HashMap bidInfo) {
        Log.d(TAG, "HeliumRewardedAd.didReceiveWinningBid for " + placementName + " is " + bidInfo)
    }
 
    @Override
    public void didCache(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumRewardedAd.didCache failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumRewardedAd.didCache for placement: " + placementName);
        }
    }
 
    @Override
    public void didShow(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumRewardedAd.didShow failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumRewardedAd.didShow for placement: " + placementName);
        }
    }
 
    @Override
    public void didClick(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumRewardedAd.didClick failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumRewardedAd.didClick for placement: " + placementName);
        }
    }
 
    @Override
    public void didClose(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumRewardedAd.didClose failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumRewardedAd.didClose for placement: " + placementName);
        }
    }
 
    @Override
    public void didReceiveReward(@NonNull String placementName, @NonNull String reward) {
        Log.d(TAG,"Got Reward for RV with placementName: " + placementName + ", reward: " + reward);
    }

    @Override
    public void didRecordImpression(@NonNull String placementName) {
        Log.d(TAG,"HeliumRewardedAd.didRecordImpression for placement: " + placementName);
    }
};
```

## HeliumBannerAdListener

You can implement the `HeliumBannerAdListener` interface to receive notifications about banner ads loading, displaying, and closing.

Starting with version 3.0.0, the `HeliumBannerAdListener` no longer has the `didShow` and `didClose` callbacks. In addition, the `didReceiveWinningBid` callback is only called when the placement has auto refresh disabled.

```kotlin Helium SDK 3.0.0 (Kotlin)
val heliumBannerAdListener = object : HeliumBannerAdListener {
    override fun didReceiveWinningBid(
        placementName: String,
        bidInfo: HashMap<String, String>
    ) {
        Log.d(TAG, "HeliumBannerAd.didReceiveWinningBid for $placementName is $bidInfo")
    }

    override fun didCache(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumBannerAd.didCache failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumBannerAd.didCache for placement: $placementName")
        }
    }

    override fun didClick(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumBannerAd.didClick failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumBannerAd.didClick for placement: $placementName")
        }
    }

    override fun didRecordImpression(placementName: String) {
        Log.d(TAG,"HeliumBannerAd.didRecordImpression for placement: $placementName")
    }
}
```
```java Helium SDK 3.0.0 (Java)
HeliumBannerAdListener heliumBannerAdListener = new HeliumBannerAdListener() {
    @Override
    public void didReceiveWinningBid(@NonNull String placementName, @NonNull HashMap bindInfo) {
        Log.d(TAG, "HeliumBannerAd.didReceiveWinningBid for " + placementName + " is " + bindInfo)
    }

    @Override
    public void didCache(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumBannerAd.didCache failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumBannerAd.didCache for placement: " + placementName);
        }
    }

    @Override
    public void didClick(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumBannerAd.didClick failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumBannerAd.didClick for placement: " + placementName);
        }
    }

    @Override
    public void didRecordImpression(@NonNull String placementName) {
        Log.d(TAG,"HeliumBannerAd.didRecordImpression for placement: " + placementName);
    }
};
```
```kotlin Helium SDK 2.11.0 & below (Kotlin)
val heliumBannerAdListener = object : HeliumBannerAdListener {
    override fun didReceiveWinningBid(
        placementName: String,
        bidInfo: HashMap<String, String>
    ) {
        Log.d(TAG, "HeliumBannerAd.didReceiveWinningBid for $placementName is $bidInfo")
    }

    override fun didCache(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumBannerAd.didCache failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumBannerAd.didCache for placement: $placementName")
        }
    }

    override fun didShow(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumBannerAd.didShow failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumBannerAd.didShow for placement: $placementName")
        }
    }

    override fun didClick(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumBannerAd.didClick failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            Log.d(TAG,"HeliumBannerAd.didClick for placement: $placementName")
        }
    }

    override fun didClose(placementName: String, error: HeliumAdError?) {
        error?.let {
            Log.d(TAG,"$HeliumBannerAd.didClose failed for placement: $placementName " + 
            "reason: ${it.message}")
        } ?: run {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumBannerAd.didClose for placement: $placementName")
        }
    }
}
```
```java Helium SDK 2.11.0 & below (Java)
HeliumBannerAdListener heliumBannerAdListener = new HeliumBannerAdListener() {
    @Override
    public void didReceiveWinningBid(@NonNull String placementName, @NonNull HashMap bidInfo) {
        Log.d(TAG, "HeliumBannerAd.didReceiveWinningBid for " + placementName + " is " + bidInfo)
    }

    @Override
    public void didCache(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumBannerAd.didCache failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            // Show the ad if it's ready
            Log.d(TAG,"HeliumBannerAd.didCache for placement: " + placementName);
        }
    }

    @Override
    public void didShow(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumBannerAd.didShow failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumBannerAd.didShow for placement: " + placementName);
        }
    }

    @Override
    public void didClick(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumBannerAd.didClick failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumBannerAd.didClick for placement: " + placementName);
        }
    }

    @Override
    public void didClose(@NonNull String placementName, @Nullable HeliumAdError error) {
        if (error != null) {
            Log.d(TAG,"HeliumBannerAd.didClose failed for placement: " + placementName + " reason: " + error.getMessage());
        } else {
            Log.d(TAG,"HeliumBannerAd.didClose for placement: " + placementName);
        }
    }
};
```

**Note:** Not all partner SDKs support the `didClick`, `didRecordImpression`, `didReceiveReward` delegate.
