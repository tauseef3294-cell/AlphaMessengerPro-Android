package com.alphamessengerpro.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private ProgressBar progressBar;
    private TextView offlineView;
    private ValueCallback<Uri[]> uploadCallback;
    private static final int FILE_CHOOSER_REQ = 5001;
    private static final int WEB_PERMISSION_REQ = 5002;
    private static final int NOTIFICATION_PERMISSION_REQ = 5003;
    private static final String MESSAGE_CHANNEL_ID = "alpha_messages_calls";
    private PermissionRequest pendingWebPermission;
    private static final String HOME_URL = "https://alpha.gt.tc/";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF171A2B);
        getWindow().setNavigationBarColor(0xFF10121D);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF10121D);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF10121D);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(3));
        root.addView(progressBar, progressParams);

        offlineView = new TextView(this);
        offlineView.setText("Unable to connect to Alpha Messenger Pro\\n\\nCheck your internet connection and tap to retry.");
        offlineView.setTextColor(Color.WHITE);
        offlineView.setTextSize(17);
        offlineView.setGravity(android.view.Gravity.CENTER);
        offlineView.setPadding(dp(24), dp(24), dp(24), dp(24));
        offlineView.setBackgroundColor(0xFF10121D);
        offlineView.setVisibility(View.GONE);
        offlineView.setOnClickListener(v -> loadHome());
        root.addView(offlineView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        setupNotifications();

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " AlphaMessengerAndroid/1.0.1");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if ("https".equalsIgnoreCase(scheme)) return false;
                if ("tel".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                    return true;
                }
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                offlineView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progressBar.setProgress(100);
                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                offlineView.setVisibility(View.GONE);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (Build.VERSION.SDK_INT >= 23 && request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    offlineView.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER_REQ); }
                catch (Exception e) {
                    uploadCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "File picker unavailable", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= 23) {
                        String[] needed = new String[0];
                        for (String resource : request.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                                    && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                                needed = append(needed, Manifest.permission.CAMERA);
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                                needed = append(needed, Manifest.permission.RECORD_AUDIO);
                        }
                        if (needed.length > 0) {
                            pendingWebPermission = request;
                            requestPermissions(needed, WEB_PERMISSION_REQ);
                        } else {
                            request.grant(request.getResources());
                        }
                    } else {
                        request.grant(request.getResources());
                    }
                });
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, false, false);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception e) { Toast.makeText(this, "Unable to open download", Toast.LENGTH_SHORT).show(); }
        });

        // Keep cookies, PHP sessions, cache and local web storage across launches.
        // Clearing these on every launch previously broke persistent sign-in.
        loadHome();
    }

    private void setupNotifications() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(MESSAGE_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(MESSAGE_CHANNEL_ID,
                        "Messages and calls", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Incoming chat messages and call alerts");
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQ);
        }
    }


    private void loadHome() {
        if (webView == null) return;
        offlineView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        webView.loadUrl(HOME_URL);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String[] append(String[] arr, String val) {
        String[] next = new String[arr.length + 1];
        System.arraycopy(arr, 0, next, 0, arr.length);
        next[arr.length] = val;
        return next;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQ && uploadCallback != null) {
            Uri[] result = (resultCode == Activity.RESULT_OK && data != null && data.getData() != null)
                    ? new Uri[]{data.getData()} : null;
            uploadCallback.onReceiveValue(result);
            uploadCallback = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == WEB_PERMISSION_REQ && pendingWebPermission != null) {
            boolean granted = results.length > 0;
            for (int result : results) if (result != PackageManager.PERMISSION_GRANTED) granted = false;
            if (granted) pendingWebPermission.grant(pendingWebPermission.getResources());
            else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
    }

    @Override public void onBackPressed() {
        if (offlineView != null && offlineView.getVisibility() == View.VISIBLE) {
            loadHome();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        if (uploadCallback != null) {
            uploadCallback.onReceiveValue(null);
            uploadCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
