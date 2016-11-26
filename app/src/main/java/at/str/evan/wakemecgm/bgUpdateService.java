package at.str.evan.wakemecgm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class BgUpdateService extends FirebaseMessagingService {
    public static Thread runningAlert;
    public BgUpdateService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d("WAKEMECGM", "From: " + remoteMessage.getFrom());
        //Vibrator vibrate = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        //vibrate.vibrate(300);
        Log.d("WAKEMECGM", remoteMessage.getData().toString());

        //startAlertService();
        /*MediaPlayer mp = MediaPlayer.create(this,R.raw.lowAlert);
        mp.setLooping(false);
        mp.start();*/



        Map<String, String> msgData = remoteMessage.getData();
        int bg = Integer.parseInt(msgData.get("latest_bg"));
        String trendVal = msgData.get("trend");
        String trendSymbol = "";

        Runnable alertSound = new Runnable() {
            @Override
            public void run() {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.lowalert));
                Log.d("wakemecgm","running");
                while(!Thread.interrupted()) {
                    ringtone.play();
                    try {
                        Thread.sleep(4500);
                    } catch (InterruptedException e) {
                        ringtone.stop();
                        Thread.currentThread().interrupt();
                    }
                }
            }

        };
        runningAlert = new Thread (alertSound);
        if (bg <= 65) {
            runningAlert.start();
        }
        switch (trendVal) {
            case "FLAT":
                trendSymbol = "→";
                break;
            case "UP_45":
                trendSymbol = "↗";
                break;
            case "SINGLE_UP":
                trendSymbol = "↑";
                break;
            case "DOUBLE_UP":
                trendSymbol = "⇈";
                break;
            case "DOWN_45":
                trendSymbol = "↘";
                break;
            case "SINGLE_DOWN":
                trendSymbol = "↓";
                break;
            case "DOUBLE_DOWN":
                trendSymbol = "⇊";
                break;
            case "NOT_COMPUTABLE":
                trendSymbol = "";
                break;
        }

        String title = bg + " " + trendSymbol;
        title.trim(); //remove the space after BG if the trend is NOT_COMPUTABLE
        String body;

        //only notify for out-of-range BGs
        if (bg < 65) {
            body = "Low BG";
            notifyLow(title, body);
        } else if (bg > 240) {
            body = "High BG";
            notifyHigh(title, body);
        }


    }

    private NotificationCompat.Builder constructNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setLights(Color.BLUE, 1, 1)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
    }

    private void notifyLow(String messageTitle, String messageBody) {
        long[] vibrateLow = {400, 400, 400, 400, 400, 400};

       /* if (bg < 130) {
            defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }*/
        final int soundResId = R.raw.lowalert;

        NotificationCompat.Builder notificationBuilder = constructNotification(messageTitle, messageBody);
        notificationBuilder.setVibrate(vibrateLow)
                .setColor(Color.RED);
        Notification bgAlert = notificationBuilder.build();
        bgAlert.flags = Notification.FLAG_INSISTENT;

        sendNotification(notificationBuilder);
    }

    private void notifyNorm(String messageTitle, String messageBody) {
        long[] vibrateNorm = {};

        NotificationCompat.Builder notificationBuilder = constructNotification(messageTitle, messageBody);
        notificationBuilder.setVibrate(vibrateNorm);
        //.setSound(RingtoneManager.getDefaultUri(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT));
        //Notification bgAlert = notificationBuilder.build();

        sendNotification(notificationBuilder);
    }

    private void notifyHigh(String messageTitle, String messageBody) {
        long[] vibrateHigh = {500, 500, 500, 500};

        NotificationCompat.Builder notificationBuilder = constructNotification(messageTitle, messageBody);
        notificationBuilder.setVibrate(vibrateHigh)
                .setColor(Color.YELLOW);
        //Notification bgAlert = notificationBuilder.build();

        sendNotification(notificationBuilder);
    }

    private void sendNotification(NotificationCompat.Builder notificationBuilder) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
