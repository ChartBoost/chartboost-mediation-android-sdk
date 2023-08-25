include(
    ":Helium",
    ":HeliumCanary",
    ":AdColonyAdapter",
    ":AdMobAdapter",
    ":AmazonPublisherServicesAdapter",
    ":AppLovinAdapter",
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
    ":TapjoyAdapter",
    ":UnityAdsAdapter",
    ":VerveAdapter",
    ":VungleAdapter",
    ":YahooAdapter",
)

val commonNamedRepoPrefix = "./chartboost-mediation-android-adapter-"

project(":AdColonyAdapter").projectDir = File(
    "${commonNamedRepoPrefix}adcolony/AdColonyAdapter"
)
project(":AdMobAdapter").projectDir = File(
    "${commonNamedRepoPrefix}admob/AdMobAdapter"
)
project(":AppLovinAdapter").projectDir = File(
    "${commonNamedRepoPrefix}applovin/AppLovinAdapter"
)
project(":AmazonPublisherServicesAdapter").projectDir = File(
    "${commonNamedRepoPrefix}amazon-publisher-services/AmazonPublisherServicesAdapter"
)
project(":ChartboostAdapter").projectDir = File(
    "${commonNamedRepoPrefix}chartboost/ChartboostAdapter"
)
project(":DigitalTurbineExchangeAdapter").projectDir = File(
    "${commonNamedRepoPrefix}digital-turbine-exchange/DigitalTurbineExchangeAdapter"
)
project(":MetaAudienceNetworkAdapter").projectDir = File(
    "${commonNamedRepoPrefix}meta-audience-network/MetaAudienceNetworkAdapter"
)
project(":GoogleBiddingAdapter").projectDir = File(
    "${commonNamedRepoPrefix}google-bidding/GoogleBiddingAdapter"
)
project(":HyprMXAdapter").projectDir = File(
    "${commonNamedRepoPrefix}hyprmx/HyprMXAdapter"
)
project(":InMobiAdapter").projectDir = File(
    "${commonNamedRepoPrefix}inmobi/InMobiAdapter"
)
project(":IronSourceAdapter").projectDir = File(
    "${commonNamedRepoPrefix}ironsource/IronSourceAdapter"
)
project(":MintegralAdapter").projectDir = File(
    "${commonNamedRepoPrefix}mintegral/MintegralAdapter"
)
project(":MobileFuseAdapter").projectDir = File(
    "${commonNamedRepoPrefix}mobilefuse/MobileFuseAdapter"
)
project(":PangleAdapter").projectDir = File(
    "${commonNamedRepoPrefix}pangle/PangleAdapter"
)
project(":ReferenceAdapter").projectDir = File(
    "${commonNamedRepoPrefix}reference/ReferenceAdapter"
)
project(":TapjoyAdapter").projectDir = File(
    "${commonNamedRepoPrefix}tapjoy/TapjoyAdapter"
)
project(":UnityAdsAdapter").projectDir = File(
    "${commonNamedRepoPrefix}unity-ads/UnityAdsAdapter"
)
project(":VerveAdapter").projectDir = File(
    "${commonNamedRepoPrefix}verve/VerveAdapter"
)
project(":VungleAdapter").projectDir = File(
    "${commonNamedRepoPrefix}vungle/VungleAdapter"
)
project(":YahooAdapter").projectDir = File(
    "${commonNamedRepoPrefix}yahoo/YahooAdapter"
)
