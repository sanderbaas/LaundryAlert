package nl.implode.laundryalert;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class LaundryReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "nl.implode.laundryalert";
    public static final String CHANNEL_NAME = "Laundry Alert Channel";
    public static final String CHANNEL_DESCRIPTION = "Keep in touch with the laundry machine.";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (intent.getAction() != null && intent.getAction() == "nl.implode.laundryalert.ACTION_DISMISS") {
            Long timestampStart = null;
            Integer messageId = null;
            Bundle bundle = intent.getExtras();
            timestampStart = bundle.getLong("timestampStart");
            messageId = bundle.getInt("messageId");
            dismissAlert(context, messageId, timestampStart);
            return;
        }
        //Log.d("Intent", intent.toString());
        Log.d("Laundry", "I'm running");
        checkAndAlert(context);
    }

    public void dismissAlert(Context context, Integer messageId, Long timestampStart) {
        Log.d("Laundry", "dismissing");
        postHandler(timestampStart, "Sander");
        //remove notification
        NotificationManager notificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(messageId);
    }

    public void checkAndAlert(Context context) {
        String title = "Laundry title";
        String message = "Laundry message";

        JSONObject laundryStatus = getLaundryStatus();
        Boolean done = false;
        Date doneTime = null;
        Boolean handled = false;
        String handledBy = null;
        Date handledTime = null;
        Integer messageId = null;
        Long timestampDone = null;
        Long timestampStart = null;

        try {
            if (laundryStatus.has("timestamp_start") && !laundryStatus.isNull("timestamp_start")) {
                timestampStart = laundryStatus.getLong("timestamp_start");
            }
            if (laundryStatus.has("timestamp_done") && !laundryStatus.isNull("timestamp_done")) {
                Log.d("Laundry", "is done");
                timestampDone = laundryStatus.getLong("timestamp_done");
                Long longMessageId = timestampDone/1000L;
                messageId = longMessageId.intValue();
                Log.d("timestamp long", timestampDone.toString());
                Log.d("timestamp int", messageId.toString());
                done = true;
                doneTime = new Date(timestampDone);
            }
            if (laundryStatus.has("timestamp_handled") && !laundryStatus.isNull("timestamp_handled")) {
                Log.d("Laundry", "is handled");
                Long timestampHandled = laundryStatus.getLong("timestamp_handled");
                handled = true;
                handledTime = new Date(timestampHandled);
            }
            if (laundryStatus.has("handler") && !laundryStatus.isNull("handler")) {
                handledBy = laundryStatus.getString("handler");
            }

        } catch (Exception e) {
            return;
        }

        if (done && !handled) {
            title = "De was is klaar";
            //SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d MMMM HH:mm");
            //message = "Done since: " + dateFormat.format(doneTime);
            alertMessage(context, messageId, timestampStart, timestampDone, title);
        }
    }

    public void postHandler(Long timestampStart, String handler) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String url = "http://kelder.implode.nl:8124/handle/" + timestampStart.toString();
        String json = "{\"handler\": \"" + handler + "\"}";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            Integer code = response.code();
            Log.d("dismiss code", code.toString());
            response.body().string();
        } catch (Exception e) {
            return;
        }
    }

    public JSONObject getLaundryStatus() {
        Log.d("getLaundryStatus", "begin");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        OkHttpClient client = new OkHttpClient();
        String url = "http://kelder.implode.nl:8124/status";
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            Integer code = response.code();
            Log.d("getLaundryStatus code", code.toString());
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
        Log.d("alert timestampStart", timestampStart.toString());
        PendingIntent dismissPendingIntent =
                PendingIntent.getBroadcast(context, 0, dismissIntent, FLAG_UPDATE_CURRENT);
        NotificationCompat.Action dismissAction = new NotificationCompat.Action.Builder(
                android.R.drawable.btn_default,
                context.getString(R.string.dismiss),
                dismissPendingIntent).build();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setWhen(timestampDone)
                .setSmallIcon(R.drawable.wash)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(dismissAction);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Integer notificationId = messageId;
        notificationManager.notify(notificationId, mBuilder.build());
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
