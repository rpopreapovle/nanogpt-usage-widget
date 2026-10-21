package com.niko.nanogptwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

public class NanoWidgetProvider extends AppWidgetProvider {

    static final String ACTION_REFRESH = "com.niko.nanogptwidget.REFRESH";
    private static final long INTERVAL_MS = 15 * 60 * 1000L;

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, buildViews(context, id));
        schedule(context);
        refreshAll(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (ACTION_REFRESH.equals(a)) {
            refreshAll(context);
            schedule(context);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        schedule(context);
        refreshAll(context);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(alarm(context));
    }

    static PendingIntent alarm(Context context) {
        Intent i = new Intent(context, NanoWidgetProvider.class).setAction(ACTION_REFRESH);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 0, i, flags);
    }

    static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = SystemClock.elapsedRealtime() + INTERVAL_MS;
        try {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, at, INTERVAL_MS, alarm(context));
        } catch (Exception ignored) {
        }
    }

    static void refreshAll(final Context context) {
        final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (mgr == null) return;
        final int[] ids = mgr.getAppWidgetIds(new ComponentName(context, NanoWidgetProvider.class));
        if (ids.length == 0) return;
        UsageRepo.refresh(context, (ok, error) -> {
            for (int id : ids) mgr.updateAppWidget(id, buildViews(context, id));
        });
    }

    static void updateAllNow(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, NanoWidgetProvider.class));
        for (int id : ids) mgr.updateAppWidget(id, buildViews(context, id));
    }

    static RemoteViews buildViews(Context context, int widgetId) {
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_nano);
        boolean hasKey = !UsageRepo.apiKey(context).isEmpty();

        v.setTextViewText(R.id.w_balance, UsageRepo.balanceText(context));
        v.setTextViewText(R.id.w_today, UsageRepo.todayText(context));
        v.setTextViewText(R.id.w_requests, UsageRepo.requestsText(context));
        v.setTextViewText(R.id.w_updated, hasKey ? UsageRepo.updatedText(context) : "откройте приложение и вставьте ключ");

        double today = 0;
        try {
            today = Double.parseDouble(UsageRepo.todayText(context).replace("$", ""));
        } catch (Exception ignored) {
        }
        v.setTextColor(R.id.w_today,
                Color.parseColor(today > 0 ? "#FFB86B" : "#8A97A6"));

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        Intent open = new Intent(context, MainActivity.class);
        v.setOnClickPendingIntent(R.id.widget_root,
                PendingIntent.getActivity(context, 0, open, flags));

        Intent refresh = new Intent(context, NanoWidgetProvider.class).setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.w_today,
                PendingIntent.getBroadcast(context, 1, refresh, flags));
        return v;
    }
}
