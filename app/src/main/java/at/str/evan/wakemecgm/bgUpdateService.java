package at.str.evan.wakemecgm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

import io.realm.Realm;

public class BgUpdateService extends FirebaseMessagingService {
    final static String TAG = "WAKEMECGM";
    public static Thread runningAlert;
    public BgUpdateService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d("WAKEMECGM", "From: " + remoteMessage.getFrom());

        Log.d("WAKEMECGM", remoteMessage.getData().toString());

        //save the last received BG value
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(
                "com.at.str.evan.wakemecgm.BG_DATA", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();

        Map<String, String> msgData = remoteMessage.getData();
        int bg = Integer.parseInt(msgData.get("latest_bg"));
        String trendVal = msgData.get("trend");
        String trendSymbol = "";
        String bg_date = msgData.get("display_time");

        DateTime bgTime = new DateTime(bg_date);
        DateTime now = new DateTime();

        if (bgTime.isBefore(now.minusMinutes(6))) {
            Log.i(TAG, "onMessageReceived: Ignoring old reading");
            return;
        }

        Runnable alertSound = new Runnable() {
            @Override
            public void run() {
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
        if (bg <= 65 && bg >= 39) { //39 is the lowest reading - when CGM displays LOW, but 30 is used
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

        editor.putInt("lastBg", bg);
        editor.putString("lastTrend",trendSymbol);
        Log.d("WAKEMECGM", "now: " + bg_date);

        editor.putString("lastReadDate",bg_date);
        editor.apply();

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        BGReading bgReading = realm.createObject(BGReading.class); // Create a new object
        long startDate = sharedPref.getLong("startDate",-1);
        bgReading.setTimestamp(new Date(),startDate); //TODO: use the date passed in the object
        bgReading.setReading(bg);
        bgReading.setTrend(trendSymbol);
        realm.commitTransaction();

        String title = bg + " " + trendSymbol;
        title = title.trim(); //remove the space after BG if the trend is NOT_COMPUTABLE
        String body;

        //only notify for out-of-range BGs
        if (bg <= 65 && bg >= 39) {
            body = "Low BG";
            notifyLow(title, body);
        } else if (bg >= 240) {
            body = "High BG";
            notifyHigh(title, body);
        }


    }

    private NotificationCompat.Builder constructNotification(String title, String body, String channelId) {
        Log.i(TAG, "constructNotification: entered");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return new NotificationCompat.Builder(getApplicationContext(), channelId)
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

        NotificationCompat.Builder notificationBuilder = constructNotification(messageTitle, messageBody, "LOW_ALERTS");
        notificationBuilder.setVibrate(vibrateLow)
                .setColor(Color.RED);
        Notification bgAlert = notificationBuilder.build();
        bgAlert.flags = Notification.FLAG_INSISTENT;

        sendNotification(notificationBuilder);
    }

    private void notifyHigh(String messageTitle, String messageBody) {
        long[] vibrateHigh = {500, 500, 500, 500};

        NotificationCompat.Builder notificationBuilder = constructNotification(messageTitle, messageBody, "HIGH_ALERTS");
        notificationBuilder.setVibrate(vibrateHigh)
                .setColor(Color.YELLOW);

        sendNotification(notificationBuilder);
    }

    private void sendNotification(NotificationCompat.Builder notificationBuilder) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
    }
}
