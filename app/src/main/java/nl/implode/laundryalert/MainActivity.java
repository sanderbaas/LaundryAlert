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

import android.os.Handler;
import android.preference.PreferenceManager;

import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class MainActivity extends AppCompatActivity {
    private PendingIntent pendingIntent;
    //private static final String MDNS_LAUNDRY_SERVICENAME = "laundry._http._tcp.local.";
    //private static final String MDNS_LAUNDRY_SERVICENAME = "_http._tcp.local";
    //private static final int DISCOVERY_TIMEOUT = 5000;
    //private PendingIntent alarmIntent;

    /*Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String text = bundle.getString("result");
            Log.d("callback", text);
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    };*/

    public void start() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        String syncInterval = sharedPref.getString("sync_frequency", "");
        if (!syncInterval.isEmpty()) {
            interval = Long.parseLong(syncInterval) * 60000L;
        }
        interval = 8000L;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Retrieve a PendingIntent that will perform a broadcast */
        /*Intent alarmIntent = new Intent(MainActivity.this, LaundryReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

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

        /*class SampleListener implements ServiceListener {
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
        }*/

        Button discover = (Button) findViewById(R.id.discover);
        discover.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("Laundry", "click discover");

                /*try {
                    // Create a JmDNS instance
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    WifiManager.MulticastLock multicastLock = null;
                    int wifiIpAddress = wifiManager.getConnectionInfo().getIpAddress();
                    InetAddress wifiInetAddress = intToInetAddress(wifiIpAddress);
                    JmDNS jmdns = JmDNS.create(wifiInetAddress);

                    // Add a service listener
                    jmdns.addServiceListener("_xbmc-jsonrpc-h._tcp.local.", new SampleListener());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }*/

                final Handler handler = new Handler();
                final Thread searchThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Laundry", "searchThread running");
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

                            /*JmDNS jmDns = (wifiInetAddress != null)?
                                    JmDNS.create(wifiInetAddress) :
                                    JmDNS.create();*/
                            //JmDNS jmDns = JmDNS.create(wifiInetAddress);
                            JmDNS jmDns = JmDNS.create(wifiInetAddress);

                            ServiceInfo serviceInfo = ServiceInfo.create("http", "example", 1234, "path=index.html");
                            jmDns.registerService(serviceInfo);
                            //_laundry._http._tcp.local._http._tcp.local
                            // Get the json rpc service list
                            final ServiceInfo[] serviceInfos =
                                    jmDns.list("laundry._http._tcp.local.", 10000);

                            Log.d("Laundry", String.valueOf(serviceInfos.length));

                            //synchronized (lock) {
                            //    // If the user didn't cancel the search, and we are sill in the activity
                            //    if (!searchCancelled && isAdded()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d("Laundry", "running handler");
                                            if ((serviceInfos == null) || (serviceInfos.length == 0)) {
                                                Log.d("Laundry", "no hosts found");
                                                noHostFound();
                                            } else {
                                                Log.d("Laundry", "found hosts");
                                                foundHosts(serviceInfos);
                                            }
                                        }
                                    });
                            //    }
                            //}
                        } catch (IOException e) {
                            Log.d("Laundry", "Got an IO Exception", e);
                        } finally {
                            if (multicastLock != null)
                                multicastLock.release();
                        }
                    }
                });

                searchThread.start();

                //Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                //MainActivity.this.startActivity(settingsIntent);
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
