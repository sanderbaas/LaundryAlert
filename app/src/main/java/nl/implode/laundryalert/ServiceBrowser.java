package nl.implode.laundryalert;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class ServiceBrowser {
    public ServiceBrowser (Context context, ServiceListener serviceListener) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

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
            //jmDns.addServiceListener("_http._tcp.local.", new BrowserListener());
            jmDns.addServiceListener("_http._tcp.local.", serviceListener);
        } catch (Exception e) {
            Log.e("Laundry", e.getMessage());
        } finally {
            if (multicastLock != null)
                multicastLock.release();
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

}
