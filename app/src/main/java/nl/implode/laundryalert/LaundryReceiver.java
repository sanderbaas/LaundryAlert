package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Date;
import java.util.Timer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class LaundryReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "nl.implode.laundryalert";
    public static final String CHANNEL_NAME = "Laundry Alert Channel";
    public static final String CHANNEL_DESCRIPTION = "Keep in touch with the laundry machine.";
    public String serverAddress;
    public String handlerName;
    public String notificationsRingtone;

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        serverAddress = sharedPref.getString("server_address", "http://localhost:8124/");
        handlerName = sharedPref.getString("handler_name", context.getString(R.string.pref_default_handler_name));
        notificationsRingtone = sharedPref.getString("notifications_ringtone", "");

        Log.d("Laundry", "Running");

        if (intent.getAction() != null && intent.getAction().equals("nl.implode.laundryalert.ACTION_DISMISS")) {
            Log.d("Laundry", "DISMISS");
            Long timestampStart = null;
            Integer messageId = null;
            Bundle bundle = intent.getExtras();
            timestampStart = bundle.getLong("timestampStart");
            messageId = bundle.getInt("messageId");
            postHandler(timestampStart, handlerName);
            cancelNotification(context, messageId);
            changeInterval(context, "sync_frequency");
            return;
        }

        if (intent.getAction() != null && intent.getAction().equals("nl.implode.laundryalert.ACTION_NAG")) {
            Log.d("Laundry", "NAG");
            checkAndAlert(context);
            return;
        }

        Log.d("Laundry", "DEFAULT");
        // if the intent action is not dismiss and not nag
        Boolean alerted = checkAndAlert(context);
        if (alerted) {
            changeInterval(context, "nag_frequency");
        }
    }

    public void cancelNotification(Context context, Integer messageId) {
        Log.d("Laundry", "cancelNotification");
        //remove notification
        NotificationManager notificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(messageId);
    }

    public Boolean checkAndAlert(Context context) {
        String title = context.getString(R.string.note_laundry_done);

        JSONObject laundryStatus = getLaundryStatus();
        Boolean done = false;
        Boolean handled = false;
        Integer messageId = null;
        Long timestampDone = null;
        Long timestampStart = null;

        try {
            if (laundryStatus.has("timestamp_start") && !laundryStatus.isNull("timestamp_start")) {
                timestampStart = laundryStatus.getLong("timestamp_start");
            }
            if (laundryStatus.has("timestamp_done") && !laundryStatus.isNull("timestamp_done")) {
                timestampDone = laundryStatus.getLong("timestamp_done");
                Long longMessageId = timestampDone/1000L;
                messageId = longMessageId.intValue();
                done = true;
            }
            if (laundryStatus.has("timestamp_handled") && !laundryStatus.isNull("timestamp_handled")) {
                handled = true;
            }

        } catch (Exception e) {
            return false;
        }

        if (done && !handled) {
            alertMessage(context, messageId, timestampStart, timestampDone, title);
            return true;
        }
        return false;
    }

    public void changeInterval(Context context, String prefName) {
        Log.d("Laundry", "change interval " + prefName);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Long currentTime = System.currentTimeMillis();
        Long interval = AlarmManager.INTERVAL_HALF_HOUR;

        String syncInterval = sharedPref.getString(prefName, "");
        if (!syncInterval.isEmpty()) {
            interval = Long.parseLong(syncInterval) * 60000L;
        }

        Intent oldIntent = new Intent(context, LaundryReceiver.class);
        if (prefName.equals("sync_frequency")) {
            oldIntent.setAction("nl.implode.laundryalert.ACTION_NAG");
        }
        PendingIntent oldAlarmIntent = PendingIntent.getBroadcast(context, 0, oldIntent, 0);

        //cancel current intent
        if (manager!= null) {
            manager.cancel(oldAlarmIntent);
            oldAlarmIntent.cancel();
        }

        Log.d("Laundry", interval.toString());
        Intent newIntent = new Intent(context, LaundryReceiver.class);
        if (prefName.equals("nag_frequency")) {
            newIntent.setAction("nl.implode.laundryalert.ACTION_NAG");
        }
        PendingIntent newAlarmIntent = PendingIntent.getBroadcast(context, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, currentTime + interval, interval, newAlarmIntent);
    }

    public void postHandler(Long timestampStart, String handler) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String url = serverAddress + "handle/" + timestampStart.toString();
        String json = "{\"handler\": \"" + handler + "\"}";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            Integer code = response.code();
            response.body().string();
        } catch (Exception e) {
            return;
        }
    }

    public JSONObject getLaundryStatus() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        OkHttpClient client = new OkHttpClient();
        String url = serverAddress + "status";
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            Integer code = response.code();
            JSONObject jsonObject = (JSONObject) new JSONTokener(response.body().string()).nextValue();
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    public void alertMessage(Context context, Integer messageId, Long timestampStart, Long timestampDone, String title) {
        createNotificationChannel(context);
        Intent dismissIntent = new Intent(context, LaundryReceiver.class);
        dismissIntent.setAction("nl.implode.laundryalert.ACTION_DISMISS");
        dismissIntent.putExtra("messageId", messageId);
        dismissIntent.putExtra("timestampStart", timestampStart);

        PendingIntent dismissPendingIntent =
                PendingIntent.getBroadcast(context, 0, dismissIntent, FLAG_UPDATE_CURRENT);
        NotificationCompat.Action dismissAction = new NotificationCompat.Action.Builder(
                android.R.drawable.btn_default,
                context.getString(R.string.dismiss),
                dismissPendingIntent).build();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setWhen(timestampDone)
                .setSmallIcon(R.drawable.baseline_local_laundry_service_white_48)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //.setOnlyAlertOnce(true)
                .addAction(dismissAction);

        if (!notificationsRingtone.isEmpty()) {
            mBuilder.setSound(Uri.parse(notificationsRingtone));
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Integer notificationId = messageId;
        Notification notification = mBuilder.build();
        notificationManager.notify(notificationId, notification);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = CHANNEL_NAME;
            String description = CHANNEL_DESCRIPTION;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

}
