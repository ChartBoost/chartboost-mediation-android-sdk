package com.chartboost.sdk.internal.Networking;

import com.chartboost.sdk.internal.logging.Logger;
import java.net.URL;

public class NetworkHelper {

    private static String debugEndpoint;
    private static boolean isforceSDKToAcceptAllSSLCertsEnabled = false;

    public static String normalizedUrl(String endpoint, String uri) {
        if (debugEndpoint != null && !debugEndpoint.isEmpty()) {
            Logger.w("normalizedUrl: " + endpoint + " to: " + debugEndpoint, null);
            endpoint = debugEndpoint;
        }

        return String.format("%s%s%s",
                endpoint,
                uri != null && uri.startsWith("/") ? "" : "/",
                uri != null ? uri : "");
    }

    public static String getEndpointFromUrl(String urlString) {
        URL url = stringToURL(urlString);
        if (url != null) {
            try {
                return url.getProtocol() + "://" + url.getHost();
            } catch (Exception e) {
                Logger.d("getEndpointFromUrl: " + urlString + " : " + e, null);
            }
        }
        return "";
    }

    public static String getPathFromUrl(String urlString) {
        URL url = stringToURL(urlString);
        if (url != null) {
            try {
                return url.getPath();
            } catch (Exception e) {
                Logger.d("getPathFromUrl: " + urlString + " : " + e, null);
            }
        }
        return "";
    }

    public static URL stringToURL(String urlString) {
        if (urlString != null && !urlString.isEmpty()) {
            try {
                return new URL(urlString);
            } catch (Exception e) {
                Logger.d("stringToURL: " + urlString + " : " + e, null);
            }
        }
        return null;
    }

    public static boolean isForceSDKToAcceptAllSSLCertsEnabled() {
        return isforceSDKToAcceptAllSSLCertsEnabled;
    }

    private static void setDebugUrl(String url) {
        debugEndpoint = url;
    }

    private static void forceSDKToAcceptAllSSLCerts() {
        isforceSDKToAcceptAllSSLCertsEnabled = true;
    }
}
