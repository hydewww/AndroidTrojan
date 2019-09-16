package com.android.safety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootCompleteReceiver extends BroadcastReceiver {

    public void getinfo(Context context) {
        Toast.makeText(context, "Boot Complete", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(context, LongRunningService.class);
        context.startService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        getinfo(context);
    }
}
