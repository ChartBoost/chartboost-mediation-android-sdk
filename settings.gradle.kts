/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

include(
    ":ChartboostMediation",
    ":ChartboostMediationCanary",
    ":ChartboostMediationJavaValidator",
    ":ChartboostCore",
    ":ChartboostCoreCanary",
    ":ChartboostCoreJavaValidator",
    ":GoogleUserMessagingPlatformAdapter",
    ":UsercentricsAdapter",
    ":AdMobAdapter",
    ":AmazonPublisherServicesAdapter",
    ":AppLovinAdapter",
    ":BidMachineAdapter",
    ":ChartboostAdapter",
    ":DigitalTurbineExchangeAdapter",
    ":GoogleBiddingAdapter",
    ":HyprMXAdapter",
    ":InMobiAdapter",
    ":IronSourceAdapter",
    ":MetaAudienceNetworkAdapter",
    ":MintegralAdapter",
    ":MobileFuseAdapter",
    ":PangleAdapter",
    ":ReferenceAdapter",
    ":ReferenceConsentAdapter",
    ":UnityAdsAdapter",
    ":UnmanagedAdapter",
    ":VerveAdapter",
    ":VungleAdapter",
)

val commonMediationRepoPrefix = "./chartboost-mediation-android-adapter-"
val commonConsentRepoPrefix = "./chartboost-core-android-consent-adapter-"

project(":AdMobAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}admob/AdMobAdapter",
    )
project(":AppLovinAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}applovin/AppLovinAdapter",
    )
project(":AmazonPublisherServicesAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}amazon-publisher-services/AmazonPublisherServicesAdapter",
    )
project(":BidMachineAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}bidmachine/BidMachineAdapter",
    )
project(":ChartboostAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}chartboost/ChartboostAdapter",
    )
project(":DigitalTurbineExchangeAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}digital-turbine-exchange/DigitalTurbineExchangeAdapter",
    )
project(":MetaAudienceNetworkAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}meta-audience-network/MetaAudienceNetworkAdapter",
    )
project(":GoogleBiddingAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}google-bidding/GoogleBiddingAdapter",
    )
project(":HyprMXAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}hyprmx/HyprMXAdapter",
    )
project(":InMobiAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}inmobi/InMobiAdapter",
    )
project(":IronSourceAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}ironsource/IronSourceAdapter",
    )
project(":MintegralAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}mintegral/MintegralAdapter",
    )
project(":MobileFuseAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}mobilefuse/MobileFuseAdapter",
    )
project(":PangleAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}pangle/PangleAdapter",
    )
project(":ReferenceAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}reference/ReferenceAdapter",
    )
project(":UnityAdsAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}unity-ads/UnityAdsAdapter",
    )
project(":VerveAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}verve/VerveAdapter",
    )
project(":VungleAdapter").projectDir =
    File(
        "${commonMediationRepoPrefix}vungle/VungleAdapter",
    )
project(":GoogleUserMessagingPlatformAdapter").projectDir =
    File(
        "${commonConsentRepoPrefix}google-user-messaging-platform/GoogleUserMessagingPlatformAdapter",
    )
project(":ReferenceConsentAdapter").projectDir =
    File(
        "${commonConsentRepoPrefix}reference/ReferenceConsentAdapter",
    )
project(":UnmanagedAdapter").projectDir =
    File(
        "${commonConsentRepoPrefix}unmanaged/UnmanagedAdapter",
    )
project(":UsercentricsAdapter").projectDir =
    File(
        "${commonConsentRepoPrefix}usercentrics/UsercentricsAdapter",
    )
