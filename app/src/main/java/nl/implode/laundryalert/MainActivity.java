package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    public void start() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Long currentTime = System.currentTimeMillis();

        Long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

        String syncInterval = sharedPref.getString("sync_frequency", "");
        if (!syncInterval.isEmpty()) {
            interval = Long.parseLong(syncInterval) * 60000L;
        }

        Intent intent = new Intent(MainActivity.this, LaundryReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, currentTime, interval, alarmIntent);
    }

    public void stop() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(MainActivity.this, LaundryReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        Intent nagIntent = new Intent(MainActivity.this, LaundryReceiver.class);
        nagIntent.setAction("nl.implode.laundryalert.ACTION_NAG");
        PendingIntent nagAlarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, nagIntent,PendingIntent.FLAG_NO_CREATE);

        // If the alarm has been set, cancel it.
        if (manager!= null && alarmIntent != null) {
            manager.cancel(alarmIntent);
            alarmIntent.cancel();
        }

        if (manager != null && nagAlarmIntent != null) {
            manager.cancel(nagAlarmIntent);
            nagAlarmIntent.cancel();
        }
    }

    public boolean isRunning() {
        Intent intent = new Intent(MainActivity.this, LaundryReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_NO_CREATE);
        Intent nagIntent = new Intent(MainActivity.this, LaundryReceiver.class);
        nagIntent.setAction("nl.implode.laundryalert.ACTION_NAG");
        PendingIntent nagAlarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, nagIntent,PendingIntent.FLAG_NO_CREATE);

        return alarmIntent != null || nagAlarmIntent != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        String title = context.getString(R.string.app_name);
                        String active = context.getString(R.string.active);
                        String inactive = context.getString(R.string.inactive);
                        title += isRunning() ? " (" + active + ")" : " (" + inactive + ")";
                        toolbar.setTitle(title);
                    }
                });
            }
        },0,5000);

        Button startService = (Button) findViewById(R.id.startService);
        startService.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                start();
            }
        });

        Button stopService = (Button) findViewById(R.id.stopService);
        stopService.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                stop();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(settingsIntent);
            }
        });
    }
}
