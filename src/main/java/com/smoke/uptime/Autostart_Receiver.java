package com.smoke.uptime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Autostart_Receiver extends BroadcastReceiver {
    public Autostart_Receiver() {
    }
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals("service_restart")){
            Toast.makeText(context.getApplicationContext(), "Восстановление Uptime", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(context, clock_service.class);
            context.startService(serviceIntent);
        }
    }
}