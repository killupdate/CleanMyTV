package com.killupdate.cleanmytv.weather;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;

public final class WeatherActivity extends Activity {
    private static final long INTERVAL_MS = 45 * 60_000L;
    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable foregroundRefresh;

    @Override protected void onStart() {
        super.onStart();
        refreshHandler.post(foregroundRefresh);
    }

    @Override protected void onStop() {
        refreshHandler.removeCallbacks(foregroundRefresh);
        super.onStop();
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(32 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        EditText city = new EditText(this);
        city.setSingleLine(true);
        city.setImeOptions(EditorInfo.IME_ACTION_DONE);
        city.setHint("Город, регион или страна");
        city.setText(getSharedPreferences("weather", 0).getString("query", ""));
        Button refresh = new Button(this);
        refresh.setText("Получить погоду");
        TextView status = new TextView(this);
        status.setText("Open-Meteo получит введённый город и координаты найденного места. Обновление по кнопке и каждые 45 минут, пока помощник открыт.");
        long updated = getSharedPreferences("weather", 0).getLong("updated", 0);
        if (updated > 0) status.append("\nПоследнее обновление: " + java.text.DateFormat
                .getDateTimeInstance().format(new java.util.Date(updated)));
        body.addView(city);
        body.addView(refresh);
        body.addView(status);
        setContentView(body);
        refresh.setOnClickListener(v -> {
            String query = city.getText().toString().trim();
            if (query.length() < 2 || query.length() > 100) {
                status.setText("Введите название от 2 до 100 символов");
                return;
            }
            refresh.setEnabled(false);
            status.setText("Загрузка…");
            new Thread(() -> {
                String result;
                try {
                    JSONObject place = request("https://geocoding-api.open-meteo.com/v1/search?count=1&language=ru&name="
                            + URLEncoder.encode(query, "UTF-8")).getJSONArray("results").getJSONObject(0);
                    double latitude = place.getDouble("latitude"), longitude = place.getDouble("longitude");
                    if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                            || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) throw new IllegalArgumentException();
                    JSONObject now = request("https://api.open-meteo.com/v1/forecast?current=temperature_2m,weather_code"
                            + "&latitude=" + latitude + "&longitude=" + longitude).getJSONObject("current");
                    int temp = (int) Math.round(now.getDouble("temperature_2m"));
                    int code = now.getInt("weather_code");
                    if (temp < -100 || temp > 70 || code < 0 || code > 99) throw new IllegalArgumentException();
                    String name = place.getString("name");
                    if (name.isEmpty() || name.length() > 100) throw new IllegalArgumentException();
                    getSharedPreferences("weather", 0).edit().putString("query", query).putString("city", name)
                            .putInt("temp", temp).putInt("code", code).putLong("updated", System.currentTimeMillis()).apply();
                    result = name + " · " + temp + " °C\nОбновлено: "
                            + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
                } catch (Exception e) {
                    result = "Не удалось получить погоду. Проверьте город и подключение.";
                }
                String label = result;
                runOnUiThread(() -> {
                    if (!isDestroyed()) {
                        status.setText(label);
                        refresh.setEnabled(true);
                    }
                });
            }, "weather-https").start();
        });
        foregroundRefresh = new Runnable() {
            @Override public void run() {
                android.content.SharedPreferences cache = getSharedPreferences("weather", 0);
                String saved = cache.getString("query", "");
                if (!saved.isEmpty() && saved.equals(city.getText().toString().trim())
                        && refresh.isEnabled() && System.currentTimeMillis() - cache.getLong("updated", 0) >= INTERVAL_MS) {
                    refresh.performClick();
                }
                refreshHandler.postDelayed(this, INTERVAL_MS);
            }
        };
    }

    private static JSONObject request(String address) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        try {
            if (connection.getResponseCode() != 200) throw new java.io.IOException("Unexpected status");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] block = new byte[4096];
                int count;
                while ((count = input.read(block)) != -1) {
                    if (bytes.size() + count > 65536) throw new java.io.IOException("Oversized response");
                    bytes.write(block, 0, count);
                }
                return new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }
}
