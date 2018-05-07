package nl.implode.laundryalert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent multiCastIntent = new Intent(context,MulticastService.class);
        context.startService(multiCastIntent);
        Log.i("Autostart", "started");
    }
}
