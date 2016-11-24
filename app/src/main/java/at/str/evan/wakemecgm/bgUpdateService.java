package at.str.evan.wakemecgm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BgUpdateService extends FirebaseMessagingService {
    public BgUpdateService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d("WAKEMECGM", "From: " + remoteMessage.getFrom());
        //Vibrator vibrate = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        //vibrate.vibrate(300);
        Log.d("WAKEMECGM", remoteMessage.getData().toString());

        int vibrate;
        int bg = Integer.parseInt(remoteMessage.getData().get("latest_bg"));
        if (bg < 70) {
            vibrate = 1;
        } else if (bg > 240) {
            vibrate = 2;
        } else {
            vibrate = 3;
        }

        sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"),vibrate);
    }

    private void sendNotification(String messageTitle, String messageBody,int pattern) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long[] vibrateLow = {400, 400, 400, 400, 400, 400};
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setVibrate(vibrateLow)
                .setAutoCancel(true)
                .setLights(Color.BLUE, 1, 1)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);


/*        long[] vibrateLow = {400, 400, 400, 400, 400, 400};
        long[] vibrateHigh = {500, 400, 500, 400};
        long[] vibrateOther = {1000};
        long[] vibrate;
        if (pattern == 1) {
            notificationBuilder.setVibrate(vibrateLow);
        } else if (pattern == 2) {
            notificationBuilder.setVibrate(vibrateHigh);
        } else {
            notificationBuilder
        }*/

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
