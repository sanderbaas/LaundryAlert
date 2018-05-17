package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.os.StrictMode;
import android.preference.PreferenceManager;

import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.net.InetAddress;
import java.net.UnknownHostException;


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

    public static InetAddress intToInetAddress(int hostAddress) {
        if (hostAddress == 0)
            return null;

        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    public void noHostFound() {
        Log.d("Laundry", "no host found");
    }

    public void foundHosts(final ServiceInfo[] serviceInfos) {
        for (Integer i = 0; i < serviceInfos.length; i++ ) {
            String name = serviceInfos[i].getName();
            Integer port = serviceInfos[i].getPort();
            String host = serviceInfos[i].getServer();
            Log.d("Found host", name + ": " + host + ":" + port.toString());
        }
    }

    private static class NetworkServiceListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }

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

        Button settings = (Button) findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener(){
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
                Log.d("Laundry", "click discover");

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                WifiManager.MulticastLock multicastLock = null;
                try {
                    // Get wifi ip address
                    int wifiIpAddress = wifiManager.getConnectionInfo().getIpAddress();
                    InetAddress wifiInetAddress = intToInetAddress(wifiIpAddress);

                    // Acquire multicast lock
                    multicastLock = wifiManager.createMulticastLock("laundryalert.multicastlock");
                    multicastLock.setReferenceCounted(true);
                    multicastLock.acquire();

                    JmDNS jmDns = JmDNS.create(wifiInetAddress);
                    jmDns.addServiceListener("_http._tcp.local.", new NetworkServiceListener());
                } catch (Exception e) {
                    Log.e("Laundry", e.getMessage());
                } finally {
                    if (multicastLock != null)
                        multicastLock.release();
                }
            }
        });


        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();
                        String result = MulticastHelper.receiveMessage(MainActivity.this);
                        Log.d("multicast",result);
                        bundle.putString("result", result);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
            }
        });*/
    }
}
