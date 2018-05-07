package nl.implode.laundryalert;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class MulticastService extends Service {
    public MulticastService() {
        Log.d("MulticastService","initialize");
        // Acquire multicast lock
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        InetAddress group;
        MulticastSocket socket;
        DatagramPacket packet;

        //Do some mutlicast job here
        try {
            group = InetAddress.getByName("0.0.0.0");
        } catch(UnknownHostException e) {
            Log.d("MessageReceiver", "Impossible to create a new group on with ip 0.0.0.0");
            e.printStackTrace();
            return;
        }

        try {
            socket = new MulticastSocket(8999);
        } catch(IOException e) {
            Log.d("MessageReceiver", "Impossible to create a new MulticastSocket on port 8999");
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (IOException e1) {
            Log.d("MessageReceiver", "There was a problem receiving the incoming message.");
            return;
        }

        Log.d("MessageReceiver", "Received Message! Processing...");
        byte data[] = packet.getData();
        try {
            //String message = Message.deserialize(data);
            String message = new String(packet.getData());
            Log.d("MessageReceiver", "Received: " + message);
        } catch (IllegalArgumentException ex) {
            Log.d("MessageReceiver", "There was a problem processing the message " + Arrays.toString(data));
            return;
        }


        // Once your finish using it, release multicast lock
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
