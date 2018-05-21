package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent alarmIntent = new Intent(context, LaundryReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

            Long currentTime = System.currentTimeMillis();

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

            Long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
            String syncInterval = sharedPref.getString("sync_frequency", "");
            if (!syncInterval.isEmpty()) {
                interval = Long.parseLong(syncInterval) * 60000L;
            }

            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, currentTime, interval, pendingIntent);

            Log.d("Laundry", "Alarm set from autostart");
            //Toast.makeText(context, "Alarm Set", Toast.LENGTH_SHORT).show();
        }
    }
}
