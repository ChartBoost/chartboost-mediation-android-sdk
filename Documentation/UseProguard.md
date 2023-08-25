# Use Proguard

For Helium SDK 3.1.0+:

- The usage of Proguard is no longer required for the Helium SDK and its adapters; however, partner network SDKs may still require Proguard. Please refer to their respective SDK documentation as to whether you need to ammend proguard rules to your application.

Prior to Helium SDK 3.1.0:

- If using proguard, add the following to your `proguard-rules.pro` file.

```java
-keep class com.chartboost.** { *; }
```

And for Tapjoy, add the following:

```java
-keep class com.tapjoy.** { *; }
-keep class com.moat.** { *; }
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keep class * extends java.util.ListResourceBundle {
protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
@com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
public static final ** CREATOR;
}
-keep class com.google.android.gms.ads.identifier.** { *; }
-dontwarn com.tapjoy.**
```
