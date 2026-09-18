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
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://ahilyanagardjs.in/";
    private static final String DOWNLOADS_URL = "https://ahilyanagardjs.in/newitems/1.html";
    private static final String PREMIUM_URL = "https://superprofile.bio/ahilyanagardjs";
    private static final String WHATSAPP_URL =
            "https://whatsapp.com/channel/0029Va9XieuJ93wa8LJJjb3O";
    private static final String YOUTUBE_URL =
            "https://www.youtube.com/@ahilyanagardjs";
    private static final String ABOUT_URL =
            "https://ahilyanagardjs.in/info/about";
    private static final String INTERNAL_HOST = "ahilyanagardjs.in";

    private static final long MIN_SPLASH_MS = 2000L;
    private static final int REQUEST_SAVE_FILE = 9001;

    private static final Pattern CONTENT_DISPOSITION_FILENAME =
            Pattern.compile("(?i)filename\\*?\\s*=\\s*(?:UTF-8''|\"?)([^\";]+)");

    private static final int NAV_HOME = 0;
    private static final int NAV_UPDATES = 1;

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
    private boolean initialPageFinished;
    private boolean splashDismissed;
    private boolean mainFrameError;
    private int selectedNavIndex = NAV_HOME;

    private float webTouchDownX;
    private float webTouchDownY;
    private int webTouchSlop;

    private String pendingDownloadUrl;
    private String pendingDownloadUserAgent;
    private String pendingDownloadFileName;
    private String pendingDownloadCookie;
    private String pendingDownloadReferer;

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
        appShell.addView(contentContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setColorSchemeColors(0xFFF4A623);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(0xFF111111);
        swipeRefreshLayout.setDistanceToTriggerSync(dpToPx(140));
        swipeRefreshLayout.setOnRefreshListener(this::refreshFromSwipe);
        swipeRefreshLayout.setOnChildScrollUpCallback(
                (parent, child) -> webView != null && webView.canScrollVertically(-1)
        );

        contentContainer.addView(swipeRefreshLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setClickable(true);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        installWebViewTouchRouting();

        swipeRefreshLayout.addView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(3));
        progressParams.gravity = Gravity.TOP;
        contentContainer.addView(progressBar, progressParams);

        offlineView = createOfflineView();
        offlineView.setVisibility(View.GONE);
        contentContainer.addView(offlineView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        bottomNavigation = createBottomNavigation();
        bottomNavigation.setVisibility(View.GONE);
        appShell.addView(bottomNavigation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(66)));

        root.addView(appShell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        splashView = new ImageView(this);
        splashView.setImageResource(R.drawable.splash_screen);
        splashView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        splashView.setBackgroundColor(Color.BLACK);
        splashView.setContentDescription(getString(R.string.app_name));

        root.addView(splashView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

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
            v.setPadding(
                    0,
                    insets.getSystemWindowInsetTop(),
                    0,
                    insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        view.requestApplyInsets();
    }

    private void installWebViewTouchRouting() {
        webTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        webView.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    webTouchDownX = event.getX();
                    webTouchDownY = event.getY();

                    // Normal taps belong to the website, not SwipeRefreshLayout.
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - webTouchDownX;
                    float dy = event.getY() - webTouchDownY;

                    boolean atPageTop = !webView.canScrollVertically(-1);
                    boolean verticalDownGesture =
                            dy > (webTouchSlop * 2f) &&
                            Math.abs(dy) > Math.abs(dx);

                    // Only hand the gesture to SwipeRefreshLayout when the user
                    // deliberately pulls down from the absolute top of the page.
                    boolean allowRefreshIntercept =
                            atPageTop && verticalDownGesture;

                    v.getParent().requestDisallowInterceptTouchEvent(
                            !allowRefreshIntercept);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            // Never consume the event here. The WebView must receive the tap.
            return false;
        });
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

        // Keep new-window requests in the existing WebView.
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);

                if (splashDismissed && !swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(
                            newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(
                    WebView view, String url, Bitmap favicon) {
                mainFrameError = false;
                offlineView.setVisibility(View.GONE);

                // Restore interaction state on every navigation.
                view.setEnabled(true);
                view.setClickable(true);
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                view.requestFocus(View.FOCUS_DOWN);

                if (splashDismissed && !swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                view.setEnabled(true);
                view.setClickable(true);
                view.requestFocus(View.FOCUS_DOWN);

                // One-time rewrite only. No MutationObserver is left running.
                normalizeStaticTargetBlankLinks(view);

                if (!initialPageFinished && !mainFrameError) {
                    initialPageFinished = true;
                    hideSplashWhenReady();
                }
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                if (request.isForMainFrame()) {
                    swipeRefreshLayout.setRefreshing(false);
                    mainFrameError = true;
                    showOfflineWhenReady();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl) {

                swipeRefreshLayout.setRefreshing(false);
                mainFrameError = true;
                showOfflineWhenReady();
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimetype,
                    long contentLength) {

                promptSaveDownload(
                        url,
                        userAgent,
                        contentDisposition,
                        mimetype);
            }
        });
    }

    private void normalizeStaticTargetBlankLinks(WebView view) {
        String script =
                "(function(){" +
                "var a=document.querySelectorAll('a[target=\"_blank\"]');" +
                "for(var i=0;i<a.length;i++){a[i].target='_self';}" +
                "})();";

        try {
            view.evaluateJavascript(script, null);
        } catch (Exception ignored) {
        }
    }

    private void promptSaveDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType) {

        if (!isNetworkAvailable()) {
            Toast.makeText(
                    this,
                    R.string.offline_message,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Uri sourceUri = Uri.parse(url);
        String scheme = sourceUri.getScheme() == null
                ? ""
                : sourceUri.getScheme().toLowerCase(Locale.US);

        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            openExternal(sourceUri);
            return;
        }

        String fileName =
                resolveDownloadFileName(
                        url,
                        contentDisposition,
                        mimeType);

        String safeMimeType =
                resolveDownloadMimeType(
                        mimeType,
                        fileName,
                        url);

        fileName = ensureMatchingExtension(fileName, safeMimeType);

        pendingDownloadUrl = url;
        pendingDownloadUserAgent = userAgent;
        pendingDownloadFileName = fileName;
        pendingDownloadCookie =
                CookieManager.getInstance().getCookie(url);
        pendingDownloadReferer = webView.getUrl();

        Intent saveIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        saveIntent.addCategory(Intent.CATEGORY_OPENABLE);
        saveIntent.setType(safeMimeType);
        saveIntent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            Toast.makeText(
                    this,
                    R.string.choose_save_location,
                    Toast.LENGTH_SHORT).show();

            startActivityForResult(saveIntent, REQUEST_SAVE_FILE);

        } catch (ActivityNotFoundException e) {
            clearPendingDownload();
            openExternal(sourceUri);
        }
    }

    private String resolveDownloadFileName(
            String url,
            String contentDisposition,
            String mimeType) {

        String fromDisposition =
                extractFileNameFromContentDisposition(contentDisposition);

        if (hasUsefulExtension(fromDisposition)) {
            return sanitizeFileName(fromDisposition);
        }

        String urlName = null;

        try {
            String last = Uri.parse(url).getLastPathSegment();

            if (last != null) {
                urlName = URLDecoder.decode(last, "UTF-8");
            }
        } catch (Exception ignored) {
        }

        if (hasUsefulExtension(urlName)) {
            return sanitizeFileName(urlName);
        }

        String title = cleanPageTitle(webView.getTitle());
        String extension =
                inferExtension(
                        mimeType,
                        fromDisposition,
                        url,
                        webView.getTitle());

        if (title == null || title.trim().isEmpty()) {
            title = "AhilyanagarDJs_Music";
        }

        if (extension == null || extension.isEmpty()) {
            extension = "mp3";
        }

        return sanitizeFileName(title) + "." + extension;
    }

    private String extractFileNameFromContentDisposition(
            String contentDisposition) {

        if (contentDisposition == null ||
                contentDisposition.trim().isEmpty()) {
            return null;
        }

        try {
            Matcher matcher =
                    CONTENT_DISPOSITION_FILENAME.matcher(
                            contentDisposition);

            if (matcher.find()) {
                String value = matcher.group(1);

                if (value != null) {
                    value = value.trim();

                    if (value.endsWith("\"")) {
                        value =
                                value.substring(
                                        0,
                                        value.length() - 1);
                    }

                    return URLDecoder.decode(value, "UTF-8");
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String cleanPageTitle(String title) {
        if (title == null) {
            return null;
        }

        return title
                .replaceAll(
                        "(?i)\\s*Mp3\\s+Song\\s+Download.*$",
                        "")
                .replaceAll(
                        "(?i)\\s*[-|]\\s*AhilyanagarDjs.*$",
                        "")
                .replaceAll(
                        "(?i)\\s*[-|]\\s*AhilyanagarDJ\\'s.*$",
                        "")
                .trim();
    }

    private boolean hasUsefulExtension(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lower =
                fileName.toLowerCase(Locale.US);

        if (lower.endsWith(".bin") ||
                lower.endsWith(".html") ||
                lower.endsWith(".htm") ||
                lower.endsWith(".php")) {
            return false;
        }

        int dot = lower.lastIndexOf('.');

        return dot > 0 &&
                dot < lower.length() - 1 &&
                lower.length() - dot <= 8;
    }

    private String inferExtension(
            String mimeType,
            String dispositionName,
            String url,
            String title) {

        String ext =
                extensionFromName(dispositionName);

        if (ext != null) {
            return ext;
        }

        try {
            String urlExt =
                    MimeTypeMap.getFileExtensionFromUrl(url);

            if (urlExt != null &&
                    !urlExt.isEmpty() &&
                    !"html".equalsIgnoreCase(urlExt) &&
                    !"php".equalsIgnoreCase(urlExt)) {
                return urlExt.toLowerCase(Locale.US);
            }
        } catch (Exception ignored) {
        }

        if (mimeType != null &&
                !mimeType.trim().isEmpty() &&
                !"application/octet-stream".equalsIgnoreCase(mimeType) &&
                !"binary/octet-stream".equalsIgnoreCase(mimeType)) {

            String mimeExt =
                    MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(
                                    mimeType.split(";")[0].trim());

            if (mimeExt != null && !mimeExt.isEmpty()) {
                return mimeExt;
            }
        }

        String lowerTitle =
                title == null
                        ? ""
                        : title.toLowerCase(Locale.US);

        if (lowerTitle.contains("wav")) {
            return "wav";
        }

        if (lowerTitle.contains("m4a")) {
            return "m4a";
        }

        if (lowerTitle.contains("zip") ||
                lowerTitle.contains("megapack") ||
                lowerTitle.contains("powerpack") ||
                lowerTitle.contains("pack ")) {
            return "zip";
        }

        if (lowerTitle.contains("mp3") ||
                lowerTitle.contains("mix") ||
                lowerTitle.contains("song")) {
            return "mp3";
        }

        return null;
    }

    private String extensionFromName(String fileName) {
        if (!hasUsefulExtension(fileName)) {
            return null;
        }

        int dot = fileName.lastIndexOf('.');

        if (dot < 0 || dot >= fileName.length() - 1) {
            return null;
        }

        return fileName
                .substring(dot + 1)
                .toLowerCase(Locale.US);
    }

    private String resolveDownloadMimeType(
            String mimeType,
            String fileName,
            String url) {

        if (mimeType != null) {
            String cleaned =
                    mimeType.split(";")[0]
                            .trim()
                            .toLowerCase(Locale.US);

            if (!cleaned.isEmpty() &&
                    !"application/octet-stream".equals(cleaned) &&
                    !"binary/octet-stream".equals(cleaned)) {
                return cleaned;
            }
        }

        String ext = extensionFromName(fileName);

        if (ext == null) {
            ext =
                    inferExtension(
                            mimeType,
                            null,
                            url,
                            webView.getTitle());
        }

        if (ext != null) {
            if ("mp3".equals(ext)) {
                return "audio/mpeg";
            }

            if ("wav".equals(ext)) {
                return "audio/wav";
            }

            if ("m4a".equals(ext)) {
                return "audio/mp4";
            }

            if ("zip".equals(ext)) {
                return "application/zip";
            }

            if ("rar".equals(ext)) {
                return "application/vnd.rar";
            }

            String mapped =
                    MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(ext);

            if (mapped != null) {
                return mapped;
            }
        }

        return "application/octet-stream";
    }

    private String ensureMatchingExtension(
            String fileName,
            String mimeType) {

        if (fileName == null ||
                fileName.trim().isEmpty()) {
            fileName = "AhilyanagarDJs_Music";
        }

        String lower =
                fileName.toLowerCase(Locale.US);

        if (lower.endsWith(".bin")) {
            fileName =
                    fileName.substring(
                            0,
                            fileName.length() - 4);
        }

        if (hasUsefulExtension(fileName)) {
            return sanitizeFileName(fileName);
        }

        String ext = null;

        if ("audio/mpeg".equalsIgnoreCase(mimeType)) {
            ext = "mp3";
        } else if ("audio/wav".equalsIgnoreCase(mimeType)) {
            ext = "wav";
        } else if ("audio/mp4".equalsIgnoreCase(mimeType)) {
            ext = "m4a";
        } else if ("application/zip".equalsIgnoreCase(mimeType)) {
            ext = "zip";
        } else if ("application/vnd.rar".equalsIgnoreCase(mimeType)) {
            ext = "rar";
        }

        if (ext == null) {
            ext = "mp3";
        }

        return sanitizeFileName(fileName) + "." + ext;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "AhilyanagarDJs_Music.mp3";
        }

        String clean =
                fileName
                        .replaceAll(
                                "[\\\\/:*?\"<>|\\p{Cntrl}]",
                                " ")
                        .replaceAll("\\s+", " ")
                        .trim();

        if (clean.length() > 140) {
            int dot = clean.lastIndexOf('.');
            String ext =
                    dot > 0
                            ? clean.substring(dot)
                            : "";

            int maxBase =
                    Math.max(
                            1,
                            140 - ext.length());

            clean =
                    clean.substring(
                            0,
                            Math.min(
                                    maxBase,
                                    clean.length()))
                            + ext;
        }

        if (clean.isEmpty()) {
            return "AhilyanagarDJs_Music.mp3";
        }

        return clean;
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        if (requestCode == REQUEST_SAVE_FILE) {
            if (resultCode == RESULT_OK &&
                    data != null &&
                    data.getData() != null) {

                downloadToSelectedFile(data.getData());
            } else {
                clearPendingDownload();
            }

            // Reassert WebView interaction after returning from Files.
            handler.postDelayed(() -> {
                if (webView != null) {
                    webView.setEnabled(true);
                    webView.setClickable(true);
                    webView.requestFocus(View.FOCUS_DOWN);
                }
            }, 100L);

            return;
        }

        super.onActivityResult(
                requestCode,
                resultCode,
                data);
    }

    private void downloadToSelectedFile(Uri destinationUri) {
        final String downloadUrl = pendingDownloadUrl;
        final String userAgent = pendingDownloadUserAgent;
        final String cookie = pendingDownloadCookie;
        final String referer = pendingDownloadReferer;

        if (downloadUrl == null ||
                downloadUrl.trim().isEmpty()) {

            clearPendingDownload();

            Toast.makeText(
                    this,
                    R.string.download_failed,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(
                this,
                R.string.download_starting,
                Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL remoteUrl = new URL(downloadUrl);

                connection =
                        (HttpURLConnection)
                                remoteUrl.openConnection();

                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);

                if (userAgent != null &&
                        !userAgent.trim().isEmpty()) {
                    connection.setRequestProperty(
                            "User-Agent",
                            userAgent);
                }

                if (cookie != null &&
                        !cookie.trim().isEmpty()) {
                    connection.setRequestProperty(
                            "Cookie",
                            cookie);
                }

                if (referer != null &&
                        !referer.trim().isEmpty()) {
                    connection.setRequestProperty(
                            "Referer",
                            referer);
                }

                connection.setRequestProperty(
                        "Accept",
                        "*/*");

                connection.setRequestProperty(
                        "Accept-Encoding",
                        "identity");

                connection.connect();

                int responseCode =
                        connection.getResponseCode();

                if (responseCode < 200 ||
                        responseCode >= 400) {
                    throw new Exception(
                            "HTTP " + responseCode);
                }

                Uri outputUri = destinationUri;

                try {
                    String responseDisposition =
                            connection.getHeaderField(
                                    "Content-Disposition");

                    String responseMime =
                            connection.getContentType();

                    String actualName =
                            resolveDownloadFileName(
                                    connection.getURL().toString(),
                                    responseDisposition,
                                    responseMime);

                    String actualMime =
                            resolveDownloadMimeType(
                                    responseMime,
                                    actualName,
                                    connection.getURL().toString());

                    actualName =
                            ensureMatchingExtension(
                                    actualName,
                                    actualMime);

                    if (actualName != null &&
                            pendingDownloadFileName != null &&
                            !actualName.equals(
                                    pendingDownloadFileName)) {

                        Uri renamed =
                                DocumentsContract.renameDocument(
                                        getContentResolver(),
                                        destinationUri,
                                        actualName);

                        if (renamed != null) {
                            outputUri = renamed;
                        }
                    }
                } catch (Exception ignored) {
                }

                try (InputStream input =
                             connection.getInputStream();
                     OutputStream output =
                             getContentResolver()
                                     .openOutputStream(
                                             outputUri,
                                             "w")) {

                    if (output == null) {
                        throw new Exception(
                                "Unable to open destination file");
                    }

                    byte[] buffer =
                            new byte[16 * 1024];

                    int read;

                    while ((read =
                                    input.read(buffer)) != -1) {
                        output.write(
                                buffer,
                                0,
                                read);
                    }

                    output.flush();
                }

                runOnUiThread(() -> {
                    Toast.makeText(
                            MainActivity.this,
                            R.string.download_saved,
                            Toast.LENGTH_LONG).show();

                    if (webView != null) {
                        webView.setEnabled(true);
                        webView.setClickable(true);
                        webView.requestFocus(
                                View.FOCUS_DOWN);
                    }
                });

            } catch (Exception e) {
                try {
                    getContentResolver().delete(
                            destinationUri,
                            null,
                            null);
                } catch (Exception ignored) {
                }

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                R.string.download_failed,
                                Toast.LENGTH_LONG).show());

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }

                runOnUiThread(
                        this::clearPendingDownload);
            }
        }).start();
    }

    private void clearPendingDownload() {
        pendingDownloadUrl = null;
        pendingDownloadUserAgent = null;
        pendingDownloadFileName = null;
        pendingDownloadCookie = null;
        pendingDownloadReferer = null;
    }

    private LinearLayout createBottomNavigation() {
        LinearLayout navigation =
                new LinearLayout(this);

        navigation.setOrientation(
                LinearLayout.HORIZONTAL);

        navigation.setGravity(Gravity.CENTER);

        navigation.setPadding(
                dpToPx(3),
                dpToPx(2),
                dpToPx(3),
                dpToPx(3));

        navigation.setBackground(
                createNavBackground());

        addNavItem(
                navigation,
                0,
                "⌂",
                getString(R.string.nav_home),
                v -> loadUrlInternal(
                        HOME_URL,
                        NAV_HOME));

        addNavItem(
                navigation,
                1,
                "⇩",
                getString(R.string.nav_downloads),
                v -> loadUrlInternal(
                        DOWNLOADS_URL,
                        NAV_UPDATES));

        addNavItem(
                navigation,
                2,
                "★",
                getString(R.string.nav_premium),
                v -> openExternal(
                        Uri.parse(PREMIUM_URL)));

        addNavItem(
                navigation,
                3,
                "↻",
                getString(R.string.nav_refresh),
                v -> refreshCurrentPage());

        addNavItem(
                navigation,
                4,
                "•••",
                getString(R.string.nav_more),
                v -> showMoreMenu());

        return navigation;
    }

    private void addNavItem(
            LinearLayout parent,
            int index,
            String iconText,
            String labelText,
            View.OnClickListener listener) {

        LinearLayout item =
                new LinearLayout(this);

        item.setOrientation(
                LinearLayout.VERTICAL);

        item.setGravity(Gravity.CENTER);

        item.setPadding(
                dpToPx(2),
                dpToPx(3),
                dpToPx(2),
                dpToPx(3));

        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);

        TextView icon =
                new TextView(this);

        icon.setText(iconText);
        icon.setTextSize(index == 4 ? 19f : 22f);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(Color.WHITE);

        item.addView(
                icon,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(30)));

        TextView label =
                new TextView(this);

        label.setText(labelText);
        label.setTextSize(9.2f);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setTextColor(0xFFD7D7D7);

        item.addView(
                label,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        parent.addView(
                item,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f));

        navItems[index] = item;
        navIcons[index] = icon;
        navLabels[index] = label;
    }

    private GradientDrawable createNavBackground() {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(0xFF090909);
        drawable.setStroke(
                dpToPx(1),
                0xFF262626);

        return drawable;
    }

    private void setNavSelection(int index) {
        selectedNavIndex = index;

        for (int i = 0;
             i < navItems.length;
             i++) {

            if (navIcons[i] == null ||
                    navLabels[i] == null) {
                continue;
            }

            boolean active = i == index;

            int color =
                    active
                            ? 0xFFF4A623
                            : 0xFFD7D7D7;

            navIcons[i].setTextColor(color);
            navLabels[i].setTextColor(color);

            navLabels[i].setTypeface(
                    navLabels[i].getTypeface(),
                    active
                            ? android.graphics.Typeface.BOLD
                            : android.graphics.Typeface.NORMAL);
        }
    }

    private void showMoreMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE);

        LinearLayout sheet =
                new LinearLayout(this);

        sheet.setOrientation(
                LinearLayout.VERTICAL);

        sheet.setPadding(
                dpToPx(20),
                dpToPx(14),
                dpToPx(20),
                dpToPx(22));

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(0xFF111111);

        float radius = dpToPx(24);

        background.setCornerRadii(
                new float[]{
                        radius, radius,
                        radius, radius,
                        0, 0,
                        0, 0
                });

        sheet.setBackground(background);

        View handle = new View(this);

        GradientDrawable handleBg =
                new GradientDrawable();

        handleBg.setColor(0xFF555555);
        handleBg.setCornerRadius(dpToPx(3));

        handle.setBackground(handleBg);

        LinearLayout.LayoutParams handleParams =
                new LinearLayout.LayoutParams(
                        dpToPx(42),
                        dpToPx(4));

        handleParams.gravity =
                Gravity.CENTER_HORIZONTAL;

        handleParams.bottomMargin =
                dpToPx(13);

        sheet.addView(
                handle,
                handleParams);

        TextView title =
                new TextView(this);

        title.setText(R.string.more_title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);

        title.setTypeface(
                title.getTypeface(),
                android.graphics.Typeface.BOLD);

        title.setPadding(
                dpToPx(6),
                0,
                0,
                dpToPx(10));

        sheet.addView(title);

        sheet.addView(
                createMoreAction(
                        R.drawable.ic_more_whatsapp,
                        getString(
                                R.string.more_whatsapp),
                        getString(
                                R.string.more_whatsapp_desc),
                        v -> {
                            dialog.dismiss();
                            openExternal(
                                    Uri.parse(
                                            WHATSAPP_URL));
                        }));

        sheet.addView(
                createMoreAction(
                        R.drawable.ic_more_youtube,
                        getString(
                                R.string.more_youtube),
                        getString(
                                R.string.more_youtube_desc),
                        v -> {
                            dialog.dismiss();
                            openExternal(
                                    Uri.parse(
                                            YOUTUBE_URL));
                        }));

        sheet.addView(
                createMoreAction(
                        R.drawable.ic_more_about,
                        getString(
                                R.string.more_about),
                        getString(
                                R.string.more_about_desc),
                        v -> {
                            dialog.dismiss();
                            loadUrlInternal(
                                    ABOUT_URL,
                                    selectedNavIndex);
                        }));

        sheet.addView(
                createMoreAction(
                        R.drawable.ic_more_exit,
                        getString(
                                R.string.more_exit),
                        getString(
                                R.string.more_exit_desc),
                        v -> {
                            dialog.dismiss();
                            showExitConfirmation();
                        }));

        dialog.setContentView(sheet);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(
                            Color.TRANSPARENT));

            window.setGravity(Gravity.BOTTOM);

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.width =
                    WindowManager.LayoutParams.MATCH_PARENT;

            params.dimAmount = 0.62f;

            window.setAttributes(params);

            window.addFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);

            window.setGravity(Gravity.BOTTOM);
        }
    }

    private LinearLayout createMoreAction(
            int iconRes,
            String labelText,
            String descriptionText,
            View.OnClickListener listener) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL);

        row.setGravity(
                Gravity.CENTER_VERTICAL);

        row.setPadding(
                dpToPx(10),
                dpToPx(9),
                dpToPx(10),
                dpToPx(9));

        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        ImageView icon =
                new ImageView(this);

        icon.setImageResource(iconRes);

        icon.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE);

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dpToPx(42),
                        dpToPx(42));

        iconParams.rightMargin =
                dpToPx(12);

        row.addView(
                icon,
                iconParams);

        LinearLayout textBlock =
                new LinearLayout(this);

        textBlock.setOrientation(
                LinearLayout.VERTICAL);

        textBlock.setGravity(
                Gravity.CENTER_VERTICAL);

        TextView label =
                new TextView(this);

        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16f);

        label.setTypeface(
                label.getTypeface(),
                android.graphics.Typeface.BOLD);

        label.setMaxLines(1);

        textBlock.addView(
                label,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView description =
                new TextView(this);

        description.setText(descriptionText);
        description.setTextColor(0xFFA8A8A8);
        description.setTextSize(12.5f);
        description.setMaxLines(2);

        LinearLayout.LayoutParams descriptionParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        descriptionParams.topMargin =
                dpToPx(2);

        textBlock.addView(
                description,
                descriptionParams);

        row.addView(
                textBlock,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));

        TextView arrow =
                new TextView(this);

        arrow.setText("›");
        arrow.setTextSize(27f);
        arrow.setTextColor(0xFF777777);
        arrow.setGravity(Gravity.CENTER);

        row.addView(
                arrow,
                new LinearLayout.LayoutParams(
                        dpToPx(30),
                        dpToPx(54)));

        return row;
    }

    private void showExitConfirmation() {
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.exit_title)
                        .setMessage(
                                R.string.exit_message)
                        .setNegativeButton(
                                R.string.cancel,
                                null)
                        .setPositiveButton(
                                R.string.exit,
                                (d, which) ->
                                        finishAffinity())
                        .create();

        dialog.setOnShowListener(d -> {
            Button positive =
                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE);

            Button negative =
                    dialog.getButton(
                            AlertDialog.BUTTON_NEGATIVE);

            if (positive != null) {
                positive.setTextColor(0xFFF4A623);
            }

            if (negative != null) {
                negative.setTextColor(0xFFF4A623);
            }
        });

        dialog.show();
    }

    private void loadUrlInternal(
            String url,
            int navIndex) {

        if (!isNetworkAvailable()) {
            mainFrameError = true;
            swipeRefreshLayout.setRefreshing(false);
            showOfflineWhenReady();
            return;
        }

        setNavSelection(navIndex);

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;

        webView.setEnabled(true);
        webView.setClickable(true);
        webView.requestFocus(View.FOCUS_DOWN);

        webView.loadUrl(url);
    }

    private void refreshFromSwipe() {
        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);

            Toast.makeText(
                    this,
                    R.string.offline_message,
                    Toast.LENGTH_SHORT).show();

            offlineView.setVisibility(View.VISIBLE);
            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;

        String currentUrl = webView.getUrl();

        if (currentUrl == null ||
                currentUrl.trim().isEmpty()) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.reload();
        }
    }

    private void refreshCurrentPage() {
        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);

            Toast.makeText(
                    this,
                    R.string.offline_message,
                    Toast.LENGTH_SHORT).show();

            offlineView.setVisibility(View.VISIBLE);
            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;

        swipeRefreshLayout.setRefreshing(true);

        String currentUrl = webView.getUrl();

        if (currentUrl == null ||
                currentUrl.trim().isEmpty()) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.reload();
        }
    }

    private void retryCurrentPage() {
        if (!isNetworkAvailable()) {
            swipeRefreshLayout.setRefreshing(false);

            Toast.makeText(
                    this,
                    R.string.offline_message,
                    Toast.LENGTH_SHORT).show();

            return;
        }

        offlineView.setVisibility(View.GONE);
        mainFrameError = false;

        swipeRefreshLayout.setRefreshing(true);

        String currentUrl = webView.getUrl();

        if (currentUrl == null ||
                currentUrl.trim().isEmpty()) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.reload();
        }
    }

    private void hideSplashWhenReady() {
        long elapsed =
                SystemClock.elapsedRealtime()
                        - splashStartedAt;

        long remaining =
                Math.max(
                        0L,
                        MIN_SPLASH_MS - elapsed);

        handler.postDelayed(() -> {
            if (splashDismissed ||
                    mainFrameError ||
                    !initialPageFinished) {
                return;
            }

            splashDismissed = true;

            splashView.animate()
                    .alpha(0f)
                    .setDuration(300L)
                    .withEndAction(() -> {
                        splashView.setVisibility(
                                View.GONE);

                        splashView.setAlpha(1f);

                        progressBar.setVisibility(
                                View.GONE);

                        bottomNavigation.setVisibility(
                                View.VISIBLE);
                    })
                    .start();

        }, remaining);
    }

    private void showOfflineWhenReady() {
        long elapsed =
                SystemClock.elapsedRealtime()
                        - splashStartedAt;

        long remaining =
                Math.max(
                        0L,
                        MIN_SPLASH_MS - elapsed);

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
        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL);

        container.setGravity(Gravity.CENTER);

        container.setPadding(
                dpToPx(28),
                dpToPx(28),
                dpToPx(28),
                dpToPx(28));

        container.setBackgroundColor(Color.BLACK);

        ImageView logo =
                new ImageView(this);

        logo.setImageResource(
                R.drawable.ic_launcher_logo);

        logo.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE);

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(
                        dpToPx(150),
                        dpToPx(150));

        logoParams.bottomMargin =
                dpToPx(20);

        container.addView(
                logo,
                logoParams);

        TextView title =
                new TextView(this);

        title.setText(
                R.string.offline_title);

        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        title.setGravity(Gravity.CENTER);

        title.setTypeface(
                title.getTypeface(),
                android.graphics.Typeface.BOLD);

        container.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView message =
                new TextView(this);

        message.setText(
                R.string.offline_message);

        message.setTextColor(0xFFBDBDBD);
        message.setTextSize(15f);
        message.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        messageParams.topMargin = dpToPx(8);
        messageParams.bottomMargin = dpToPx(24);

        container.addView(
                message,
                messageParams);

        Button retryButton =
                new Button(this);

        retryButton.setText(R.string.retry);
        retryButton.setTextColor(Color.BLACK);
        retryButton.setTextSize(16f);
        retryButton.setAllCaps(false);
        retryButton.setBackgroundColor(0xFFF4A623);
        retryButton.setOnClickListener(
                v -> retryCurrentPage());

        container.addView(
                retryButton,
                new LinearLayout.LayoutParams(
                        dpToPx(180),
                        dpToPx(52)));

        return container;
    }

    private boolean handleUrl(Uri uri) {
        String scheme =
                uri.getScheme() == null
                        ? ""
                        : uri.getScheme()
                                .toLowerCase(Locale.US);

        String host =
                uri.getHost() == null
                        ? ""
                        : uri.getHost()
                                .toLowerCase(Locale.US);

        if (("http".equals(scheme) ||
                "https".equals(scheme)) &&
                (host.equals(INTERNAL_HOST) ||
                        host.endsWith(
                                "." + INTERNAL_HOST))) {

            return false;
        }

        openExternal(uri);
        return true;
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri);

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    this,
                    R.string.no_app_found,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo info =
                manager.getActiveNetworkInfo();

        return info != null &&
                info.isConnected();
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density);
    }

    @Override
    public void onBackPressed() {
        if (offlineView.getVisibility() ==
                View.VISIBLE) {

            offlineView.setVisibility(View.GONE);

            if (webView.getUrl() == null) {
                finish();
            }

            return;
        }

        if (webView != null &&
                webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState) {

        webView.saveState(outState);

        super.onSaveInstanceState(
                outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (webView != null) {
            webView.setOnTouchListener(null);
            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
