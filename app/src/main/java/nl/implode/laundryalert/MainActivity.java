package nl.implode.laundryalert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private PendingIntent pendingIntent;

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
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
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
