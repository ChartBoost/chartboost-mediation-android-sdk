package com.chartboost.sdk.internal.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;

import com.chartboost.sdk.internal.Libraries.CBConstants;

import java.util.UUID;

/**
 * Calculates device information, e.g. the actual device type with possible values being:
 * phone|tablet|chromebook|tv|watch (campaigns will have an option to target or exclude these).
 * If aggressive==true, popular phones that are "phablets" will be categorized as tablets.
 * If aggressive==false, smaller, 7" tablets will be categorized as phones (except for Kindle).
 * I think the non-aggressive mode is the least of evil, as tablets are quickly dying out, ref:
 * http://www.businessinsider.com/google-android-tablets-future-chrome-os-ipad-amazon-2017-6
 */

public class DeviceInfo {
    /**
     * emulators / some old phones / custom ROMs / rooted devices
     */
    private static final String INVALID_ANDROID_ID = "9774d56d682e549c";

    private static final String SECRET_FLAG = "cb.limit.aid";

    public static int OPENRTB_DEVICE_PHONE = 4;
    public static int OPENRTB_DEVICE_TABLET = 5;

    private static final boolean aggressive = false;

    /**
     * TODO figure out what it does and refactor, looks like it is trying to figure out device type
     * although all the edge cases and assumption in this code make it unreliable
     *
     * @param context
     * @return
     */
    public static String getType(Context context) {
        if (context == null) {
            return "phone";
        }

        Resources resources = context.getResources();
        if (resources == null) {
            return "phone";
        }

        Configuration configuration = resources.getConfiguration();
        if (configuration == null) {
            return "phone";
        }

        int uim = configuration.uiMode & Configuration.UI_MODE_TYPE_MASK;
        int screenType = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return "phone";
        }
        // from https://github.com/google/talkback/blob/master/src/main/java/com/google/android/marvin/talkback/TalkBackService.java
        // and https://developer.chrome.com/apps/getstarted_arc
        // and https://stackoverflow.com/a/39843396
        // note: cannot check for PackageManager.FEATURE_PC as we don't target api 27 yet
        if (pm.hasSystemFeature("org.chromium.arc.device_management") ||
                (Build.BRAND != null && Build.BRAND.equals("chromium") && Build.MANUFACTURER.equals("chromium")) ||
                (Build.DEVICE != null && Build.DEVICE.matches(".+_cheets")))
            return "chromebook";

        if (pm.hasSystemFeature(PackageManager.FEATURE_WATCH) || uim == Configuration.UI_MODE_TYPE_WATCH)
            return "watch";

        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION) || uim == Configuration.UI_MODE_TYPE_TELEVISION)
            return "tv";

        if ((Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase("Amazon")) ||
                screenType == Configuration.SCREENLAYOUT_SIZE_XLARGE) //TODO most likely this is invalid assumption cause new phones may have xlarge screens
            return "tablet";

        if (aggressive && screenType == Configuration.SCREENLAYOUT_SIZE_LARGE)
            return "tablet";

        return "phone";
    }

    public static Integer getOpenRTBDeviceType(Context context) {
        // Anything bigger than a 6.5 screen is considered a tablet
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float yInches = metrics.heightPixels / metrics.ydpi;
        float xInches = metrics.widthPixels / metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        if (diagonalInches >= 6.5) {
            return OPENRTB_DEVICE_TABLET; // Tablet
        } else {
            return OPENRTB_DEVICE_PHONE; // Phone
        }
    }

    /**
     * Create or get already created unique device id. Based on Android Id when available,
     * and not limited by tracking. In case tracking is limited use random generated UUID.
     *
     * @param context The current context
     * @param isTrackingLimited If the tracking is limited
     * @return The unique device id
     */
    public static String getUniqueId(Context context, boolean isTrackingLimited) {
        String id = getAndroidId(context);
        if (isTrackingLimited || id == null) {
            return getRandomUUID(context);
        }
        return id;
    }

    private static String getRandomUUID(Context context) {
        SharedPreferences sp = context.getSharedPreferences(CBConstants.PREFERENCES_FILE_DEFAULT, Context.MODE_PRIVATE);
        if (sp == null) {
            return UUID.randomUUID().toString();
        }

        String uuid = sp.getString(CBConstants.PREFERENCES_KEY_UUID, null);
        if (uuid != null) {
            return uuid;
        }

        uuid = UUID.randomUUID().toString();
        SharedPreferences.Editor editor = sp.edit();
        if (editor != null) {
            editor.putString(CBConstants.PREFERENCES_KEY_UUID, uuid).apply();
        }
        return uuid;
    }

    /**
     * Android id will be null if the secret cb.limit.aid flag, added as per MO-3294, is on
     * Requires READ_PHONE_STATE permission to be granted
     *
     * @return ANDROID_ID
     */
    @SuppressLint("HardwareIds")
    private static String getAndroidId(Context context) {
        if (context == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return null;
        }

        if (isSecretFlagActive(context)) {
            return null;
        }

        String id = null;
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            try {
                id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
                if (INVALID_ANDROID_ID.equals(id)) {
                    id = null;
                }
            } catch (Exception e) {
                //accessing settings secure might be different on each device, ignore exception
            }
        }
        return id;
    }

    /**
     * Secret cb.limit.aid flag, added as per MO-3294
     *
     * @param context
     * @return true if flag exits and has value 1 otherwise false
     */
    private static boolean isSecretFlagActive(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Object o = info.metaData.get(SECRET_FLAG);
            if (o instanceof Integer && (Integer) o == 1) {
                return true;
            }
        } catch (Exception e) {
            //ignore exception
        }
        return false;
    }
}
