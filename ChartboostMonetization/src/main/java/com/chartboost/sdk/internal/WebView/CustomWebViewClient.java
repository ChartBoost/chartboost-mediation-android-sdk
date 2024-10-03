package com.chartboost.sdk.internal.WebView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.chartboost.sdk.internal.logging.Logger;
import com.chartboost.sdk.tracking.ErrorEvent;
import com.chartboost.sdk.tracking.EventTracker;
import com.chartboost.sdk.tracking.TrackingEventName;

public class CustomWebViewClient extends WebViewClient {
    private final CustomWebViewInterface callback;
    private final EventTracker eventTracker;

    public CustomWebViewClient(CustomWebViewInterface callback, EventTracker eventTracker) {
        this.callback = callback;
        this.eventTracker = eventTracker;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WebViewApiAvailability")
            PackageInfo pkg = WebView.getCurrentWebViewPackage();

            if (pkg != null) {
                Logger.d("WebView version: " + pkg.versionName, null);
            } else {
                error("Device was not set up correctly.");
            }
        }
        callback.onPageStarted();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (callback != null) {
            callback.onPageFinished();
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        error("Error loading " + failingUrl + ": " + description);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        eventTracker.track(
                ErrorEvent.instance(
                        TrackingEventName.Show.WEBVIEW_SSL_ERROR,
                        error.toString()
                )
        );
        super.onReceivedSslError(view, handler, error);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        // Note that unlike the deprecated version of the callback, the new version will be called for any resource (iframe, image, etc.), not just for the main page.
        if (request.isForMainFrame()) {
            error("Error loading " + request.getUrl().toString() + ": " + error.getDescription());
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        Logger.d("Error loading " + request.getUrl().toString() + ": " + (errorResponse == null ? "unknown error" : errorResponse.getReasonPhrase()), null);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String url) {
        return false;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.O)
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        // By using this API, you allow your app to continue executing, even though the renderer process has gone away.
        // To cause an render process crash for test purpose, the application can call loadUrl("chrome://crash") on the WebView.
        error(detail.didCrash() ? "Webview crashed: " + detail : "Webview killed, likely due to low memory");
        if (view != null && view.getContext() instanceof Activity) {
            ((Activity) view.getContext()).finish();
        }
        return true;
    }

    private void error(String message) {
        if (callback != null) {
            callback.onError(message);
        }
    }
}
