package com.niko.nanogptwidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Тянет баланс и траты за сегодня с API NanoGPT и кэширует результат.
 * Ключ хранится только локально в SharedPreferences.
 */
final class UsageRepo {

    interface Callback {
        void onResult(boolean ok, String error);
    }

    static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static final String PREFS = "nanogpt";
    private static final String UA = "NanoGptWidget/1.1 (Android)";

    private UsageRepo() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String apiKey(Context c) {
        return prefs(c).getString("key", "").trim();
    }

    static void setApiKey(Context c, String key) {
        prefs(c).edit().putString("key", key == null ? "" : key.trim()).apply();
    }

    static String balanceText(Context c) {
        String b = prefs(c).getString("balance", null);
        if (b == null) return "$—";
        return "$" + b;
    }

    static String todayText(Context c) {
        String t = prefs(c).getString("today", null);
        if (t == null) return "—";
        return "$" + t;
    }

    static String requestsText(Context c) {
        long r = prefs(c).getLong("requests", -1);
        return r < 0 ? "—" : String.valueOf(r);
    }

    static String updatedText(Context c) {
        long t = prefs(c).getLong("ts", 0);
        if (t == 0) return "нет данных";
        return "обновлено " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date(t));
    }

    static void refresh(final Context ctx, final Callback cb) {
        final String key = apiKey(ctx);
        POOL.execute(() -> {
            String err = null;
            boolean ok = false;
            if (key.isEmpty()) {
                err = "нет API-ключа";
            } else {
                try {
                    fetchBalance(ctx, key);
                    ok = true;
                } catch (Exception e) {
                    err = e.getMessage();
                }
                // траты за день — отдельно, чтобы сбой одного запроса не ломал второй
                try {
                    fetchToday(ctx, key);
                } catch (Exception e) {
                    if (err == null) err = e.getMessage();
                }
            }
            final boolean fok = ok;
            final String ferr = err;
            if (cb != null) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> cb.onResult(fok, ferr));
            }
        });
    }

    /** POST /api/check-balance — именно POST, GET даёт 405. */
    private static void fetchBalance(Context ctx, String key) throws Exception {
        String body = post("https://nano-gpt.com/api/check-balance", key, "{}");
        JSONObject o = new JSONObject(body);
        String usd = o.optString("usd_balance", null);
        if (usd == null || usd.isEmpty()) throw new Exception("нет usd_balance в ответе");
        prefs(ctx).edit()
                .putString("balance", fmt(usd))
                .putLong("ts", System.currentTimeMillis())
                .apply();
    }

    private static void fetchToday(Context ctx, String key) throws Exception {
        String today = utcDate(0);
        String body = get("https://api.nano-gpt.com/api/v1/usage?from=" + today
                + "&to=" + today + "&group_by=day", key);
        JSONObject o = new JSONObject(body);
        JSONObject totals = o.optJSONObject("totals");
        double cost = 0;
        long requests = 0;
        if (totals != null) {
            cost = totals.optDouble("netCostUsd", 0);
            requests = totals.optLong("requests", 0);
        } else {
            JSONArray byDay = o.optJSONArray("byDay");
            if (byDay != null && byDay.length() > 0) {
                JSONObject d = byDay.getJSONObject(0);
                cost = d.optDouble("netCostUsd", 0);
                requests = d.optLong("requests", 0);
            }
        }
        prefs(ctx).edit()
                .putString("today", fmtNum(cost))
                .putLong("requests", requests)
                .putLong("ts", System.currentTimeMillis())
                .apply();
    }

    static String utcDate(int offsetDays) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(System.currentTimeMillis() + offsetDays * 86400000L));
    }

    static String fmt(String usd) {
        try {
            return fmtNum(Double.parseDouble(usd));
        } catch (Exception e) {
            return usd;
        }
    }

    static String fmtNum(double v) {
        if (v == 0) return "0.00";
        if (v < 0.01) return String.format(Locale.US, "%.4f", v);
        return String.format(Locale.US, "%.2f", v);
    }

    private static String get(String url, String key) throws Exception {
        return call("GET", url, key, null);
    }

    private static String post(String url, String key, String json) throws Exception {
        return call("POST", url, key, json);
    }

    private static String call(String method, String url, String key, String json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.setRequestMethod(method);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + key);
        c.setRequestProperty("x-api-key", key);
        if (json != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setFixedLengthStreamingMode(json.getBytes("UTF-8").length);
            try (OutputStream os = c.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }
        }
        try {
            int code = c.getResponseCode();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream()))) {
                String line;
                while (r != null && (line = r.readLine()) != null) sb.append(line);
            }
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + (sb.length() > 0 ? ": " + shorten(sb.toString()) : ""));
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    private static String shorten(String s) {
        s = s.replace('\n', ' ').trim();
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
