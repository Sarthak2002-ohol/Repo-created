package in.ahilyanagardjs.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Push announcements are independent of the Android media playback notification. */
public class AdcFirebaseService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "adc_announcements";

    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "App updates and music releases", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("New DJ music, app updates and announcements");
            manager.createNotificationChannel(channel);
        }
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        // For Firebase console notification campaigns: the SDK automatically shows
        // background notifications; foreground notifications arrive here.
        RemoteMessage.Notification remote = message.getNotification();
        String title = remote != null ? remote.getTitle() : message.getData().get("title");
        String body = remote != null ? remote.getBody() : message.getData().get("body");
        if (title == null || title.trim().isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) return;
        createChannel(this);
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        String url = message.getData().get("target_url");
        if (url != null && isTrustedUrl(url)) open.putExtra("target_url", url);
        PendingIntent pending = PendingIntent.getActivity(this, (int)(System.currentTimeMillis() & 0x7fffffff),
                open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder.setSmallIcon(R.drawable.ic_stat_music)
                .setContentTitle(title).setContentText(body == null ? "" : body)
                .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                .setContentIntent(pending).setAutoCancel(true).build();
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify((int)(System.currentTimeMillis() & 0x7fffffff), notification);
    }

    private static boolean isTrustedUrl(String url) {
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "ahilyanagardjs.in".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null && uri.getPath().startsWith("/music-app/");
        } catch (Exception ignored) { return false; }
    }
}
