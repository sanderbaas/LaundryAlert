package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    private PendingIntent pendingIntent;

    public void start() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        String syncInterval = sharedPref.getString("sync_frequency", "");
        if (!syncInterval.isEmpty()) {
            interval = Long.parseLong(syncInterval) * 60000L;
        }

        Intent intent = new Intent(MainActivity.this, LaundryReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, alarmIntent);
    }

    public void stop() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(MainActivity.this, LaundryReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        // If the alarm has been set, cancel it.
        if (manager!= null) {
            manager.cancel(alarmIntent);
        }
    }

    private ServiceListener serviceListener = new ServiceListener(){
        @Override
        public void serviceAdded(ServiceEvent event) {
            Log.d("Laundry", "Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            Log.d("Laundry", "Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            Log.d("Laundry", "Service resolved: " + event.getInfo());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startService = (Button) findViewById(R.id.startService);
        final Context context = getApplicationContext();
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

        Button discover = (Button) findViewById(R.id.discover);
        discover.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("Laundry", "start discovery");
                ServiceBrowser serviceBrowser = new ServiceBrowser(context, serviceListener);
            }
        });
    }
}
