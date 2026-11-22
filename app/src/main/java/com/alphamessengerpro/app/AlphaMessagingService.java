package com.alphamessengerpro.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AlphaMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL = "alpha_messages_calls";

    @Override public void onNewToken(String token) {
        getSharedPreferences("alpha_fcm", MODE_PRIVATE).edit().putString("token", token).apply();
        // Token is registered with the authenticated PHP session when the app opens.
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : message.getData().getOrDefault("sender_name", "Alpha Messenger");
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : message.getData().getOrDefault("preview", "New message");
        String chatId = message.getData().getOrDefault("open_chat", message.getData().getOrDefault("sender_id", ""));
        createChannel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("open_chat_id", chatId);
        PendingIntent pending = PendingIntent.getActivity(this, chatId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        try {
            NotificationManagerCompat.from(this).notify((int)System.currentTimeMillis(), notification.build());
        } catch (SecurityException ignored) { }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL, "Messages and calls", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Incoming Alpha Messenger messages and call notifications");
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
    }
}
