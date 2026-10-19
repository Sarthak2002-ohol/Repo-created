package in.ahilyanagardjs.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** MediaSession mirrors the ONE existing WebView audio player; it NEVER creates another player. */
public class AdcMediaService extends Service {
    static final String ACTION_UPDATE = "in.ahilyanagardjs.app.MEDIA_UPDATE";
    static final String ACTION_STOP = "in.ahilyanagardjs.app.MEDIA_STOP";
    static final String ACTION_MEDIA_COMMAND = "in.ahilyanagardjs.app.MEDIA_COMMAND";
    private static final String ACTION_CONTROL = "in.ahilyanagardjs.app.MEDIA_CONTROL";
    private static final String CHANNEL = "adc_playback";
    private static final int NOTIFICATION_ID = 11010;
    private final ExecutorService artWorker = Executors.newSingleThreadExecutor();

    static volatile boolean isRunning = false;
    private MediaSession session;
    private NotificationManager manager;
    private String title = "", artist = "", cover = "";
    private Bitmap artwork;
    private long position = 0, duration = 0;
    private boolean playing = false;
    private boolean hasStarted = false;

    @Override public void onCreate() {
        super.onCreate();
        isRunning = true;
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Music playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Song controls while AhilyanagarDJ's is playing");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        session = new MediaSession(this, "AhilyanagarDJsMedia");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { command("play"); }
            @Override public void onPause() { command("pause"); }
            @Override public void onSkipToPrevious() { command("previous"); }
            @Override public void onSkipToNext() { command("next"); }
            @Override public void onStop() { command("stop"); }
        });
        session.setActive(true);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        if (ACTION_CONTROL.equals(action)) {
            String cmd = intent.getStringExtra("command");
            if (cmd != null && cmd.matches("play|pause|next|previous|stop")) command(cmd);
            return START_NOT_STICKY;
        }
        if (!ACTION_UPDATE.equals(action)) return START_NOT_STICKY;
        String newTitle = intent.getStringExtra("title");
        if (newTitle == null || newTitle.isEmpty()) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        title = newTitle;
        artist = intent.getStringExtra("artist");
        if (artist == null) artist = "AhilyanagarDJ's";
        playing = intent.getBooleanExtra("playing", false);
        position = intent.getLongExtra("position", 0);
        duration = intent.getLongExtra("duration", 0);
        String newCover = intent.getStringExtra("cover");
        if (newCover == null) newCover = "";
        if (!newCover.equals(cover)) {
            cover = newCover;
            artwork = null;
            loadArtworkAsync(cover);
        }
        updateSession();
        Notification notification = buildNotification();
        if (!hasStarted) {
            startForeground(NOTIFICATION_ID, notification);
            hasStarted = true;
        } else {
            manager.notify(NOTIFICATION_ID, notification);
        }
        return START_NOT_STICKY;
    }

    private void command(String command) {
        Intent toActivity = new Intent(ACTION_MEDIA_COMMAND);
        toActivity.setPackage(getPackageName());
        toActivity.putExtra("command", command);
        sendBroadcast(toActivity);
    }

    private PendingIntent pendingCommand(String command, int requestCode) {
        Intent intent = new Intent(this, AdcMediaService.class);
        intent.setAction(ACTION_CONTROL);
        intent.putExtra("command", command);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void updateSession() {
        int state = playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_STOP;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, Math.max(0, position), playing ? 1f : 0f,
                        SystemClock.elapsedRealtime()).build());
        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "AhilyanagarDJ's Music")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, Math.max(0, duration));
        if (artwork != null) metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork);
        session.setMetadata(metadata.build());
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.MediaStyle style = new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat_music)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(openPending)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setOnlyAlertOnce(true)
                .setOngoing(playing)
                .setShowWhen(false)
                .setStyle(style)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_previous, "Previous", pendingCommand("previous", 1)).build())
                .addAction(new Notification.Action.Builder(
                        playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pause" : "Play", pendingCommand(playing ? "pause" : "play", 2)).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_next, "Next", pendingCommand("next", 3)).build());
        if (artwork != null) b.setLargeIcon(artwork);
        return b.build();
    }

    private void loadArtworkAsync(final String urlString) {
        artWorker.execute(() -> {
            Bitmap image = fetchArtwork(urlString);
            android.os.Handler main = new android.os.Handler(getMainLooper());
            main.post(() -> {
                if (!urlString.equals(cover) || !hasStarted) return;
                artwork = image;
                updateSession();
                manager.notify(NOTIFICATION_ID, buildNotification());
            });
        });
    }

    private Bitmap fetchArtwork(String urlString) {
        HttpURLConnection con = null;
        try {
            Uri parsed = Uri.parse(urlString);
            String host = parsed.getHost();
            if (!"https".equalsIgnoreCase(parsed.getScheme()) || host == null
                    || !(host.equalsIgnoreCase("ahilyanagardjs.in")
                       || host.equalsIgnoreCase("www.ahilyanagardjs.in"))) return null;
            con = (HttpURLConnection) new URL(urlString).openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(8000);
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Accept", "image/*");
            if (con.getResponseCode() != 200) return null;
            String type = con.getContentType();
            if (type == null || !type.toLowerCase().startsWith("image/")) return null;
            if (con.getContentLength() > 2 * 1024 * 1024) return null;
            try (InputStream in = con.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    if (out.size() + n > 2 * 1024 * 1024) return null;
                    out.write(buf, 0, n);
                }
                byte[] data = out.toByteArray();
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                opts.inSampleSize = 1;
                while (opts.outWidth / opts.inSampleSize > 512 || opts.outHeight / opts.inSampleSize > 512)
                    opts.inSampleSize *= 2;
                opts.inJustDecodeBounds = false;
                return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            }
        } catch (Exception ignored) { return null; }
        finally { if (con != null) con.disconnect(); }
    }

    private void stopPlayback() {
        playing = false;
        if (session != null) session.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, 0, 0f).build());
        if (hasStarted) { stopForeground(true); hasStarted = false; }
        if (manager != null) manager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        // Playback is owned by the website WebView, not a duplicate native player.
        // Avoid leaving misleading media controls after Android removes that owner.
        stopPlayback();
    }
    @Override public void onDestroy() {
        isRunning = false;
        if (session != null) { session.setActive(false); session.release(); session = null; }
        artWorker.shutdownNow();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
