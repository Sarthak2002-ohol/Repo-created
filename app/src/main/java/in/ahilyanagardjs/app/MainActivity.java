package in.ahilyanagardjs.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://ahilyanagardjs.in/";
    private static final String INTERNAL_HOST = "ahilyanagardjs.in";
    private static final long MIN_SPLASH_MS = 2000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private WebView webView;
    private ProgressBar progressBar;
    private ImageView splashView;
    private LinearLayout offlineView;

    private long splashStartedAt;
    private boolean initialPageFinished = false;
    private boolean splashDismissed = false;
    private boolean mainFrameError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        splashStartedAt = SystemClock.elapsedRealtime();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);

        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(webView, webParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(3)
        );
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        offlineView = createOfflineView();
        offlineView.setVisibility(View.GONE);
        root.addView(offlineView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        splashView = new ImageView(this);
        splashView.setImageResource(R.drawable.splash_screen);
        splashView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        splashView.setBackgroundColor(Color.BLACK);
        splashView.setContentDescription(getString(R.string.app_name));
        root.addView(splashView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
        configureWebView();

        if (savedInstanceState == null) {
            loadHome();
        } else {
            webView.restoreState(savedInstanceState);
            initialPageFinished = true;
            hideSplashWhenReady();
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (splashDismissed) {
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mainFrameError = false;
                offlineView.setVisibility(View.GONE);
                if (splashDismissed) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!initialPageFinished && !mainFrameError) {
                    initialPageFinished = true;
                    hideSplashWhenReady();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    mainFrameError = true;
                    showOfflineWhenReady();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                mainFrameError = true;
                showOfflineWhenReady();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                openExternal(Uri.parse(url));
            }
        });
    }

    private void loadHome() {
        if (!isNetworkAvailable()) {
            mainFrameError = true;
            showOfflineWhenReady();
            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;
        webView.loadUrl(HOME_URL);
    }

    private void retryCurrentPage() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_SHORT).show();
            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;

        String currentUrl = webView.getUrl();
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.reload();
        }
    }

    private void hideSplashWhenReady() {
        long elapsed = SystemClock.elapsedRealtime() - splashStartedAt;
        long remaining = Math.max(0L, MIN_SPLASH_MS - elapsed);

        handler.postDelayed(() -> {
            if (splashDismissed || mainFrameError || !initialPageFinished) {
                return;
            }

            splashDismissed = true;
            splashView.animate()
                    .alpha(0f)
                    .setDuration(300L)
                    .withEndAction(() -> {
                        splashView.setVisibility(View.GONE);
                        splashView.setAlpha(1f);
                        progressBar.setVisibility(View.GONE);
                    })
                    .start();
        }, remaining);
    }

    private void showOfflineWhenReady() {
        long elapsed = SystemClock.elapsedRealtime() - splashStartedAt;
        long remaining = Math.max(0L, MIN_SPLASH_MS - elapsed);

        handler.postDelayed(() -> {
            splashDismissed = true;
            splashView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            offlineView.setVisibility(View.VISIBLE);
        }, remaining);
    }

    private LinearLayout createOfflineView() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dpToPx(28), dpToPx(28), dpToPx(28), dpToPx(28));
        container.setBackgroundColor(Color.BLACK);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dpToPx(150), dpToPx(150));
        logoParams.bottomMargin = dpToPx(20);
        container.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText(R.string.offline_title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        container.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView message = new TextView(this);
        message.setText(R.string.offline_message);
        message.setTextColor(0xFFBDBDBD);
        message.setTextSize(15f);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dpToPx(8);
        messageParams.bottomMargin = dpToPx(24);
        container.addView(message, messageParams);

        Button retryButton = new Button(this);
        retryButton.setText(R.string.retry);
        retryButton.setTextColor(Color.BLACK);
        retryButton.setTextSize(16f);
        retryButton.setAllCaps(false);
        retryButton.setBackgroundColor(0xFFF4A623);
        retryButton.setOnClickListener(v -> retryCurrentPage());

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                dpToPx(180),
                dpToPx(52)
        );
        container.addView(retryButton, buttonParams);

        return container;
    }

    private boolean handleUrl(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if (("http".equals(scheme) || "https".equals(scheme)) &&
                (host.equals(INTERNAL_HOST) || host.endsWith("." + INTERNAL_HOST))) {
            return false;
        }

        openExternal(uri);
        return true;
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this link.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (offlineView.getVisibility() == View.VISIBLE) {
            offlineView.setVisibility(View.GONE);
            if (webView.getUrl() == null) {
                finish();
            }
            return;
        }

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
