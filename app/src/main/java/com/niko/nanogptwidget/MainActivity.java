package com.niko.nanogptwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText keyInput;
    private TextView balance, today, requests, status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keyInput = findViewById(R.id.keyInput);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setText(UsageRepo.apiKey(this));

        balance = findViewById(R.id.balance);
        today = findViewById(R.id.today);
        requests = findViewById(R.id.requests);
        status = findViewById(R.id.status);

        findViewById(R.id.save).setOnClickListener(v -> {
            UsageRepo.setApiKey(this, keyInput.getText().toString());
            Toast.makeText(this, "Ключ сохранён", Toast.LENGTH_SHORT).show();
            load();
        });

        findViewById(R.id.refresh).setOnClickListener(v -> load());

        findViewById(R.id.addWidget).setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                AppWidgetManager mgr = getSystemService(AppWidgetManager.class);
                if (mgr != null && mgr.isRequestPinAppWidgetSupported()) {
                    mgr.requestPinAppWidget(new ComponentName(this, NanoWidgetProvider.class), null, null);
                    return;
                }
            }
            Toast.makeText(this, "Долгое нажатие на рабочем столе → Виджеты → NanoGPT",
                    Toast.LENGTH_LONG).show();
        });

        bind();
        load();
    }

    private void bind() {
        balance.setText(UsageRepo.balanceText(this));
        today.setText(UsageRepo.todayText(this));
        requests.setText(UsageRepo.requestsText(this));
    }

    private void load() {
        status.setText("загрузка…");
        UsageRepo.refresh(this, (ok, error) -> {
            bind();
            status.setText(ok ? "обновлено: " + UsageRepo.updatedText(this)
                    : "не удалось обновить: " + (error == null ? "нет сети" : error)
                      + "\nпоказаны последние сохранённые данные");
            NanoWidgetProvider.updateAllNow(this);
            NanoWidgetProvider.schedule(this);
        });
    }
}
