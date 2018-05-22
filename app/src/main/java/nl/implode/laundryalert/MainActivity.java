package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RunnableScheduledFuture;


public class MainActivity extends AppCompatActivity {
    private PendingIntent pendingIntent;
    Boolean foundServices = false;
    Boolean showedNoServiceAlert = false;

    @Override
    protected void onResume() {
        super.onResume();
        ServiceBrowser serviceBrowser = new ServiceBrowser(getApplicationContext(), serviceListener);
    }

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
        if (manager!= null) {
            manager.cancel(alarmIntent);
            alarmIntent.cancel();
            manager.cancel(nagAlarmIntent);
            nagAlarmIntent.cancel();
        }
    }

    public boolean isRunning() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(MainActivity.this, LaundryReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_NO_CREATE);
        Intent nagIntent = new Intent(MainActivity.this, LaundryReceiver.class);
        nagIntent.setAction("nl.implode.laundryalert.ACTION_NAG");
        PendingIntent nagAlarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, nagIntent,PendingIntent.FLAG_NO_CREATE);

        return alarmIntent != null || nagAlarmIntent != null;
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
            final String name = event.getName();
            final String endPoint = event.getInfo().getPropertyString("endpoint");
            final String message1 = getApplicationContext().getString(R.string.found_laundry_service);
            final String message2 = getApplicationContext().getString(R.string.connect_to_service);
            final String no_services = getApplicationContext().getString(R.string.no_services);
            final String yes = getApplicationContext().getString(R.string.yes);
            final String no = getApplicationContext().getString(R.string.no);
            final String ok = getApplicationContext().getString(R.string.ok);
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

            final String currentEndpoint = sharedPref.getString("server_address", "");

            if (name.equals("laundryApi") && !endPoint.equals(currentEndpoint)) {
                foundServices = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle(message1);
                        alertDialog.setMessage(message2 + "\n\n" + endPoint);
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putString("server_address",endPoint);
                                        editor.apply();
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                });
            }
        }
    };

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
