# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-printmapping build/outputs/mapping/release/mapping.txt

-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,EnclosingMethod

-optimizations !code/allocation/variable

-keepattributes *Annotation*

-keep public interface androidx.annotation.* { *; }

-keep class com.chartboost.chartboostmediationsdk.domain.Ad$AdType { *; }
-keep class com.chartboost.chartboostmediationsdk.domain.Ad$State { *; }
-keep class com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAd$ChartboostMediationBannerSize { *; }

-keep public class com.chartboost.chartboostmediationsdk.ChartboostMediationSdk {
    public <methods>;
}

-keep public class com.chartboost.chartboostmediationsdk.Ilrd { *; }
-keep public interface com.chartboost.chartboostmediationsdk.ChartboostMediationIlrdObserver { *; }
-keep public class com.chartboost.chartboostmediationsdk.ChartboostMediationImpressionData { *; }
-keep public class com.chartboost.chartboostmediationsdk.PartnerConsents { *; }

-keep public class com.chartboost.chartboostmediationsdk.domain.Keywords { *; }

#Adapters won't be able to make bids. We need to check what to keep.
-keep public class com.chartboost.chartboostmediationsdk.domain.Bid {
    public *;
}

-keep public class com.chartboost.chartboostmediationsdk.domain.Bids {
    public *;
}

-keep public class com.chartboost.chartboostmediationsdk.domain.requests.* {
    *;
}

-keep public class com.chartboost.chartboostmediationsdk.domain.PartnerAdLoadRequest {
    public *;
}


-keep public class com.chartboost.chartboostmediationsdk.proxies.* {
    public <methods>;
}

-keep public interface com.chartboost.chartboostmediationsdk.ChartboostMediationSdk$ChartboostMediationSdkListener {
    public <methods>;
    public <fields>;
}

-keep public class com.chartboost.chartboostmediationsdk.domain.AdIdentifier { *; }

-keep class com.chartboost.chartboostmediationsdk.domain.AppConfig { *; }

-keep public class com.chartboost.chartboostmediationsdk.ad.* {
    public <methods>;
    public <fields>;
}

# Also used by networks for logging
-keep public class com.chartboost.chartboostmediationsdk.utils.LogController {
    public *;
}

# This is to keep Kotlin metadata which is useful for properties when obfuscated
-keep class kotlin.Metadata { *; }

# Mediation
-keep class com.chartboost.mediation.** { *; }

# Keep kotlinx.serialization annotations
-keepattributes *Annotation*

# Keep the names of kotlinx.serialization classes
-keep,includedescriptorclasses class kotlinx.serialization.** {
    *;
}

# Keep the names of classes with @Serializable annotation
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class * {
    *;
}

# Keep kotlinx.serialization internal implementation classes
-keepclassmembers class kotlinx.serialization.internal.** {
    *;
}

# Keep all jakewharton Kotlin Serialization intact
-keep class com.jakewharton.retrofit2.converter.kotlinx.serialization.** { *; }

# Keep all OkHttp3
-keep class okhttp3.** { *; }

# Keep necessary Retrofit2 classes
-keep class retrofit2.** { *; }
-keep class retrofit2.converter.** { *; }
