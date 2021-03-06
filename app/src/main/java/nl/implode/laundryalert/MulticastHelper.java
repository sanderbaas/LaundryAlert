package nl.implode.laundryalert;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class MulticastHelper {
    public static String receiveMessage(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        InetAddress group;
        MulticastSocket socket;
        DatagramPacket packet;
        String message = "";

        //Do some mutlicast job here
        try {
            group = InetAddress.getByName("224.0.0.1");
        } catch(UnknownHostException e) {
            Log.d("MessageReceiver", "Impossible to create a new group on with ip 0.0.0.0");
            e.printStackTrace();
            return "";
        }

        try {
            socket = new MulticastSocket(8999);
            socket.joinGroup(group);
        } catch(IOException e) {
            Log.d("MessageReceiver", "Impossible to create a new MulticastSocket on port 8999");
            e.printStackTrace();
            return "";
        }

        byte[] buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (IOException e1) {
            Log.d("MessageReceiver", "There was a problem receiving the incoming message.");
            return "";
        }

        Log.d("MessageReceiver", "Received Message! Processing...");
        byte data[] = packet.getData();
        try {
            //String message = Message.deserialize(data);
            message = new String(packet.getData());
            Log.d("MessageReceiver", "Received: " + message);
        } catch (IllegalArgumentException ex) {
            Log.d("MessageReceiver", "There was a problem processing the message " + Arrays.toString(data));
            return "";
        }


        // Once your finish using it, release multicast lock
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
        return message;
    }
}
