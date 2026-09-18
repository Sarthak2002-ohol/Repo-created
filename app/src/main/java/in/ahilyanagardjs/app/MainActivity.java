package in.ahilyanagardjs.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.view.Window;
import android.view.WindowManager;
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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://ahilyanagardjs.in/";
    private static final String DOWNLOADS_URL = "https://ahilyanagardjs.in/newitems/1.html";
    private static final String PREMIUM_URL = "https://superprofile.bio/ahilyanagardjs";
    private static final String WHATSAPP_URL = "https://whatsapp.com/channel/0029Va9XieuJ93wa8LJJjb3O";
    private static final String YOUTUBE_URL = "https://www.youtube.com/@ahilyanagardjs";
    private static final String ABOUT_URL = "https://ahilyanagardjs.in/info/about";
    private static final String INTERNAL_HOST = "ahilyanagardjs.in";
    private static final long MIN_SPLASH_MS = 2000L;

    private static final int NAV_HOME = 0;
    private static final int NAV_DOWNLOADS = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private ImageView splashView;
    private LinearLayout offlineView;
    private LinearLayout bottomNavigation;
    private final LinearLayout[] navItems = new LinearLayout[5];
    private final TextView[] navIcons = new TextView[5];
    private final TextView[] navLabels = new TextView[5];

    private long splashStartedAt;
    private boolean initialPageFinished = false;
    private boolean splashDismissed = false;
    private boolean mainFrameError = false;
    private int selectedNavIndex = NAV_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        splashStartedAt = SystemClock.elapsedRealtime();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout appShell = new LinearLayout(this);
        appShell.setOrientation(LinearLayout.VERTICAL);
        appShell.setBackgroundColor(Color.BLACK);
        applySystemBarInsets(appShell);

        FrameLayout contentContainer = new FrameLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        appShell.addView(contentContainer, contentParams);

        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setColorSchemeColors(0xFFF4A623);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(0xFF111111);
        swipeRefreshLayout.setOnRefreshListener(this::refreshFromSwipe);
        contentContainer.addView(swipeRefreshLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        swipeRefreshLayout.addView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(3)
        );
        progressParams.gravity = Gravity.TOP;
        contentContainer.addView(progressBar, progressParams);

        offlineView = createOfflineView();
        offlineView.setVisibility(View.GONE);
        contentContainer.addView(offlineView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        bottomNavigation = createBottomNavigation();
        bottomNavigation.setVisibility(View.GONE);
        appShell.addView(bottomNavigation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(66)
        ));

        root.addView(appShell, new FrameLayout.LayoutParams(
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
        setNavSelection(NAV_HOME);

        if (savedInstanceState == null) {
            loadUrlInternal(HOME_URL, NAV_HOME);
        } else {
            webView.restoreState(savedInstanceState);
            initialPageFinished = true;
            hideSplashWhenReady();
        }
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            v.setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
        view.requestApplyInsets();
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
                if (splashDismissed && !swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mainFrameError = false;
                offlineView.setVisibility(View.GONE);
                if (splashDismissed && !swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                if (!initialPageFinished && !mainFrameError) {
                    initialPageFinished = true;
                    hideSplashWhenReady();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    swipeRefreshLayout.setRefreshing(false);
                    mainFrameError = true;
                    showOfflineWhenReady();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                swipeRefreshLayout.setRefreshing(false);
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

    private LinearLayout createBottomNavigation() {
        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dpToPx(3), dpToPx(2), dpToPx(3), dpToPx(3));
        navigation.setBackground(createNavBackground());

        addNavItem(navigation, 0, "⌂", getString(R.string.nav_home), v ->
                loadUrlInternal(HOME_URL, NAV_HOME));

        addNavItem(navigation, 1, "⇩", getString(R.string.nav_downloads), v ->
                loadUrlInternal(DOWNLOADS_URL, NAV_DOWNLOADS));

        addNavItem(navigation, 2, "★", getString(R.string.nav_premium), v ->
                openExternal(Uri.parse(PREMIUM_URL)));

        addNavItem(navigation, 3, "↻", getString(R.string.nav_refresh), v ->
                refreshCurrentPage());

        addNavItem(navigation, 4, "•••", getString(R.string.nav_more), v ->
                showMoreMenu());

        return navigation;
    }

    private void addNavItem(LinearLayout parent, int index, String iconText,
                            String labelText, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3));
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);

        TextView icon = new TextView(this);
        icon.setText(iconText);
        icon.setTextSize(index == 4 ? 19f : 22f);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(Color.WHITE);
        item.addView(icon, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(30)
        ));

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(10.5f);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setTextColor(0xFFD7D7D7);
        item.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        parent.addView(item, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
        ));

        navItems[index] = item;
        navIcons[index] = icon;
        navLabels[index] = label;
    }

    private GradientDrawable createNavBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFF090909);
        drawable.setStroke(dpToPx(1), 0xFF262626);
        return drawable;
    }

    private void setNavSelection(int index) {
        selectedNavIndex = index;
        for (int i = 0; i < navItems.length; i++) {
            if (navIcons[i] == null || navLabels[i] == null) {
                continue;
            }
            boolean active = i == index;
            int color = active ? 0xFFF4A623 : 0xFFD7D7D7;
            navIcons[i].setTextColor(color);
            navLabels[i].setTextColor(color);
            navLabels[i].setTypeface(navLabels[i].getTypeface(),
                    active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void showMoreMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(22));

        GradientDrawable sheetBackground = new GradientDrawable();
        sheetBackground.setColor(0xFF111111);
        float radius = dpToPx(24);
        sheetBackground.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        sheet.setBackground(sheetBackground);

        View handle = new View(this);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0xFF555555);
        handleBg.setCornerRadius(dpToPx(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dpToPx(42), dpToPx(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dpToPx(13);
        sheet.addView(handle, handleParams);

        TextView title = new TextView(this);
        title.setText(R.string.more_title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setPadding(dpToPx(6), 0, 0, dpToPx(10));
        sheet.addView(title);

        sheet.addView(createMoreAction("◉", getString(R.string.more_whatsapp), v -> {
            dialog.dismiss();
            openExternal(Uri.parse(WHATSAPP_URL));
        }));

        sheet.addView(createMoreAction("▶", getString(R.string.more_youtube), v -> {
            dialog.dismiss();
            openExternal(Uri.parse(YOUTUBE_URL));
        }));

        sheet.addView(createMoreAction("ⓘ", getString(R.string.more_about), v -> {
            dialog.dismiss();
            loadUrlInternal(ABOUT_URL, selectedNavIndex);
        }));

        sheet.addView(createMoreAction("⏻", getString(R.string.more_exit), v -> {
            dialog.dismiss();
            showExitConfirmation();
        }));

        dialog.setContentView(sheet);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.dimAmount = 0.62f;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }
    }

    private LinearLayout createMoreAction(String iconText, String labelText, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        TextView icon = new TextView(this);
        icon.setText(iconText);
        icon.setTextSize(22f);
        icon.setTextColor(0xFFF4A623);
        icon.setGravity(Gravity.CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dpToPx(46), dpToPx(42)));

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(
                0,
                dpToPx(46),
                1f
        ));

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(27f);
        arrow.setTextColor(0xFF777777);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dpToPx(30), dpToPx(46)));

        return row;
    }

    private void showExitConfirmation() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.exit, (d, which) -> finishAffinity())
                .create();
        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (positive != null) positive.setTextColor(0xFFF4A623);
            if (negative != null) negative.setTextColor(0xFFF4A623);
        });
        dialog.show();
    }

    private void loadUrlInternal(String url, int navIndex) {
        if (!isNetworkAvailable()) {
            mainFrameError = true;
            swipeRefreshLayout.setRefreshing(false);
            showOfflineWhenReady();
            return;
        }

        setNavSelection(navIndex);
        offlineView.setVisibility(View.GONE);
        mainFrameError = false;
        webView.loadUrl(url);
    }

    private void refreshFromSwipe() {
        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_SHORT).show();
            offlineView.setVisibility(View.VISIBLE);
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

    private void refreshCurrentPage() {
        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_SHORT).show();
            offlineView.setVisibility(View.VISIBLE);
            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;
        swipeRefreshLayout.setRefreshing(true);
        String currentUrl = webView.getUrl();
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.reload();
        }
    }

    private void retryCurrentPage() {
        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_SHORT).show();
            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;
        swipeRefreshLayout.setRefreshing(true);

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
                        bottomNavigation.setVisibility(View.VISIBLE);
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
            swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);
            offlineView.setVisibility(View.VISIBLE);
            bottomNavigation.setVisibility(View.VISIBLE);
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
            Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_SHORT).show();
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
