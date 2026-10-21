package com.niko.nanogptwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NanoWidgetProvider.schedule(context);
        NanoWidgetProvider.refreshAll(context);
    }
}
