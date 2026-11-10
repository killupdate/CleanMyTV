package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

final class Automotive {
    static final int IMPORT_BACKGROUND = 410, IMPORT_FOREGROUND = 411, IMPORT_ICON = 412;
    static final String HELPER = "com.killupdate.cleanmytv.weather";
    private static final String[] WALLS = {"Blue Fog", "Rain Reflections", "Mountain Night", "Blue Headlights", "Industrial Dock"};
    private final ImageView background, foreground;
    private final int mode, intensity;

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("dark_automotive", Context.MODE_PRIVATE);
    }

    static void migrateTiles(SharedPreferences p) {
        if (p.getInt("tileSchema", 0) < 3) {
            p.edit().putInt("tileMode", 0).putInt("plate", 100)
                    .putBoolean("tileGlow", true).putInt("tileSchema", 3).apply();
        }
    }

    static void tileSettings(Activity activity, Runnable changed) {
        SharedPreferences p = prefs(activity);
        String glow = p.getBoolean("tileGlow", true) ? "Подсветка: включена" : "Подсветка: выключена";
        new AlertDialog.Builder(activity).setTitle("Плитки приложений")
                .setSingleChoiceItems(new String[]{"Классические", "Полупрозрачные", "Только логотипы",
                        "Непрозрачность фона: " + p.getInt("plate", 100) + "%", glow},
                        p.getInt("tileMode", 0), (d, i) -> {
                            d.dismiss();
                            if (i == 3) slider(activity, "Непрозрачность фона", "plate", 100, changed);
                            else {
                                if (i == 4) p.edit().putBoolean("tileGlow", !p.getBoolean("tileGlow", true)).apply();
                                else p.edit().putInt("tileMode", i).apply();
                                changed.run();
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    static void settings(Activity activity, Runnable changed, Runnable legacy) {
        SharedPreferences p = prefs(activity);
        new AlertDialog.Builder(activity).setTitle("Фон и обои · Dark Automotive")
                .setItems(new String[]{"Выбрать автомобильные обои", "Импорт фона из памяти или USB",
                        "Импорт прозрачного переднего плана", "Режим глубины", "Интенсивность",
                        "Внешний вид → Плитки приложений", "Часы и погода → Прозрачность",
                        "Открыть Weather Helper", "Прежние обои и оформление", "Затемнение обоев", "Формат часов", "Показ погоды", "Сбросить к Blue Fog", "Локальный PNG-логотип"}, (dialog, choice) -> {
                    switch (choice) {
                        case 0:
                            gallery(activity, changed);
                            break;
                        case 1: case 2:
                            try {
                                activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .setType(choice == 1 ? "image/*" : "image/png"),
                                        choice == 1 ? IMPORT_BACKGROUND : IMPORT_FOREGROUND);
                            } catch (android.content.ActivityNotFoundException e) {
                                try {
                                    activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT)
                                            .addCategory(Intent.CATEGORY_OPENABLE)
                                            .setType(choice == 1 ? "image/*" : "image/png"),
                                            choice == 1 ? IMPORT_BACKGROUND : IMPORT_FOREGROUND);
                                } catch (android.content.ActivityNotFoundException unavailable) {
                                    android.widget.Toast.makeText(activity, "Нет системного выбора файлов. Используйте встроенную галерею.",
                                            android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                            break;
                        case 3:
                            new AlertDialog.Builder(activity).setTitle("Режим глубины")
                                    .setSingleChoiceItems(new String[]{"2D / выкл.", "Параллакс", p.getBoolean("foreground", false) ? "3D / два слоя" : "3D → параллакс (нет слоя)"},
                                            p.getInt("mode", 0), (d, i) -> {
                                                p.edit().putInt("mode", i).apply();
                                                changed.run();
                                                d.dismiss();
                                            }).show();
                            break;
                        case 4: slider(activity, "Интенсивность", "intensity", 35, changed); break;
                        case 5: tileSettings(activity, changed); break;
                        case 6: slider(activity, "Непрозрачность часов и погоды", "header", 85, changed); break;
                        case 7:
                            Intent helper = activity.getPackageManager().getLaunchIntentForPackage(HELPER);
                            if (helper != null) activity.startActivity(helper);
                            else android.widget.Toast.makeText(activity, "Weather Helper не установлен", android.widget.Toast.LENGTH_SHORT).show();
                            break;
                        case 8:
                            p.edit().putInt("wall", -2).apply();
                            changed.run();
                            legacy.run();
                            break;
                        case 9: slider(activity, "Затемнение обоев", "dim", 18, changed); break;
                        case 10:
                            new AlertDialog.Builder(activity).setTitle("Формат часов")
                                    .setItems(new String[]{"Системный", "12 часов", "24 часа"}, (d, i) -> {
                                        p.edit().putInt("clock", i).apply();
                                        changed.run();
                                    }).show();
                            break;
                        case 11:
                            new AlertDialog.Builder(activity).setTitle("Погода")
                                    .setItems(new String[]{"Показывать", "Скрыть"}, (d, i) -> {
                                        p.edit().putBoolean("weather", i == 0).apply();
                                        changed.run();
                                    }).show();
                            break;
                        case 12: select(activity, 0, changed); break;
                        case 13: chooseIcon(activity); break;
                        default: break;
                    }
                }).show();
    }

    private static void chooseIcon(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        java.util.List<android.content.pm.ApplicationInfo> apps = new java.util.ArrayList<>();
        for (android.content.pm.ApplicationInfo info : pm.getInstalledApplications(0)) {
            if (pm.getLaunchIntentForPackage(info.packageName) != null
                    || pm.getLeanbackLaunchIntentForPackage(info.packageName) != null) apps.add(info);
        }
        apps.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        String[] labels = new String[apps.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = apps.get(i).loadLabel(pm).toString();
        new AlertDialog.Builder(activity).setTitle("Логотип приложения").setItems(labels, (d, i) -> {
            try {
                activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE).setType("image/png"), IMPORT_ICON);
                prefs(activity).edit().putString("pendingIcon", apps.get(i).packageName).apply();
            } catch (android.content.ActivityNotFoundException ignored) {
                android.widget.Toast.makeText(activity, "Системный выбор файлов недоступен",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    static File iconFile(Context c, String pkg) {
        if (pkg == null || !pkg.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+") || pkg.length() > 180) return null;
        return new File(c.getFilesDir(), "auto-icon-" + pkg + ".png");
    }

    private static void select(Activity activity, int index, Runnable changed) {
        prefs(activity).edit().putInt("wall", index).remove("foreground").apply();
        changed.run();
    }

    private static void gallery(Activity activity, Runnable changed) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(activity);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Фон и обои")
                .setView(row).setNegativeButton(android.R.string.cancel, null).create();
        for (int i = 0; i < WALLS.length; i++) {
            int index = i;
            ImageView tile = new ImageView(activity);
            tile.setContentDescription(WALLS[i]);
            tile.setFocusable(true);
            tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.setPadding(4, 4, 4, 4);
            tile.setOnFocusChangeListener((v, focused) -> v.setBackgroundColor(focused ? 0xff57c7ff : 0xff0a141e));
            tile.setImageBitmap(asset(activity, i, 8));
            row.addView(tile, new android.widget.LinearLayout.LayoutParams(0, dp(activity, 100), 1));
            tile.setOnClickListener(v -> {
                ImageView preview = new ImageView(activity);
                preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                preview.setImageBitmap(asset(activity, index, 1));
                AlertDialog full = new AlertDialog.Builder(activity).setTitle(WALLS[index]).setView(preview)
                        .setPositiveButton("Применить", (d, which) -> { select(activity, index, changed); dialog.dismiss(); })
                        .setNegativeButton("Назад", null).create();
                full.show();
                full.getWindow().setLayout(-1, -1);
            });
        }
        dialog.show();
        dialog.getWindow().setLayout(dp(activity, 960), -2);
        row.getChildAt(0).requestFocus();
    }

    private static Bitmap asset(Context context, int index, int sample) {
        try (InputStream in = context.getAssets().open("automotive/" + index + ".jpg")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = sample;
            return BitmapFactory.decodeStream(in, null, options);
        } catch (java.io.IOException | OutOfMemoryError ignored) {
            return null;
        }
    }

    private static void slider(Activity a, String title, String key, int initial, Runnable changed) {
        SeekBar bar = new SeekBar(a);
        bar.setMax(100);
        bar.setKeyProgressIncrement(5);
        bar.setProgress(prefs(a).getInt(key, initial));
        bar.setContentDescription(title);
        new AlertDialog.Builder(a).setTitle(title + " · 0–100%").setView(bar)
                .setPositiveButton(android.R.string.ok, (d, i) -> {
                    prefs(a).edit().putInt(key, bar.getProgress()).apply();
                    changed.run();
                }).setNegativeButton(android.R.string.cancel, null).show();
        bar.requestFocus();
    }

    static boolean importImage(Context c, Uri uri, int request) {
        boolean icon = request == IMPORT_ICON;
        boolean layer = request == IMPORT_FOREGROUND;
        File target = icon ? iconFile(c, prefs(c).getString("pendingIcon", null))
                : new File(c.getFilesDir(), layer ? "auto-layer.png" : "auto-wall.jpg");
        if (target == null) return false;
        if (uri == null || !"content".equals(uri.getScheme())) return false;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            if (o.outWidth < 1 || o.outHeight < 1 || o.outWidth > 16000 || o.outHeight > 16000) return false;
            o.inSampleSize = 1;
            while (o.outWidth / o.inSampleSize > (icon ? 192 : 1920) || o.outHeight / o.inSampleSize > (icon ? 192 : 1080)) o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            Bitmap image;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                image = BitmapFactory.decodeStream(in, null, o);
            }
            if (image == null) return false;
            if ((layer || icon) && (!"image/png".equals(o.outMimeType) || !image.hasAlpha())) {
                image.recycle();
                return false;
            }
            AtomicFile destination = new AtomicFile(target);
            FileOutputStream out = null;
            try {
                out = destination.startWrite();
                if (!image.compress(layer || icon ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 88, out)) {
                    destination.failWrite(out);
                    return false;
                }
                destination.finishWrite(out);
            } catch (java.io.IOException e) {
                destination.failWrite(out);
                return false;
            } finally {
                image.recycle();
            }
            SharedPreferences.Editor edit = prefs(c).edit();
            if (icon) edit.remove("pendingIcon");
            else if (layer) edit.putBoolean("foreground", true);
            else edit.putInt("wall", -1).remove("foreground");
            edit.apply();
            return true;
        } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) {
            return false;
        }
    }

    Automotive(Activity a, FrameLayout root, boolean motionAllowed) {
        SharedPreferences p = prefs(a);
        int wall = p.getInt("wall", 0);
        mode = motionAllowed && !((android.app.ActivityManager) a.getSystemService(Context.ACTIVITY_SERVICE)).isLowRamDevice()
                ? p.getInt("mode", 0) : 0;
        intensity = p.getInt("intensity", 35);
        root.setBackgroundColor(0xff05090e);
        background = new ImageView(a);
        foreground = new ImageView(a);
        Bitmap picture = null;
        if (wall == -1) picture = BitmapFactory.decodeFile(new File(a.getFilesDir(), "auto-wall.jpg").getPath());
        else if (wall >= 0 && wall < WALLS.length) {
            try (InputStream in = a.getAssets().open("automotive/" + wall + ".jpg")) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                picture = BitmapFactory.decodeStream(in, null, options);
            } catch (java.io.IOException ignored) { }
        }
        if (picture != null) {
            addLayer(root, background, picture, 0);
            background.setAlpha(1f - p.getInt("dim", 18) / 100f);
            if (mode == 2 && intensity > 0 && p.getBoolean("foreground", false)) {
                Bitmap layer = BitmapFactory.decodeFile(new File(a.getFilesDir(), "auto-layer.png").getPath());
                if (layer != null) addLayer(root, foreground, layer, 1);
            }
        }
        FrameLayout header = new FrameLayout(a);
        header.setAlpha(p.getInt("header", 85) / 100f);
        TextClock clock = new TextClock(a);
        int format = p.getInt("clock", 0);
        clock.setFormat12Hour(format == 2 ? "HH:mm" : "h:mm a");
        clock.setFormat24Hour(format == 1 ? "h:mm a" : "HH:mm");
        clock.setTextSize(36);
        clock.setTextColor(0xffeef8ff);
        clock.setGravity(Gravity.CENTER);
        header.addView(clock, new FrameLayout.LayoutParams(-1, -1));
        TextView weather = new TextView(a);
        weather.setTextColor(0xff9bdfff);
        weather.setTextSize(12);
        weather.setMaxLines(2);
        FrameLayout.LayoutParams weatherLayout = new FrameLayout.LayoutParams(dp(a, 105), -2, Gravity.TOP | Gravity.RIGHT);
        header.addView(weather, weatherLayout);
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(dp(a, 332), dp(a, 58), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        top.topMargin = dp(a, 16);
        root.addView(header, top);
        View original = a.findViewById(R.id.clock);
        View date = a.findViewById(R.id.date);
        if (date instanceof TextClock && date.isShown()) {
            TextClock oldDate = (TextClock) date;
            TextClock copy = new TextClock(a);
            copy.setFormat12Hour(oldDate.getFormat12Hour());
            copy.setFormat24Hour(oldDate.getFormat24Hour());
            copy.setGravity(Gravity.CENTER);
            copy.setTextColor(0xffc8d9e7);
            copy.setTextSize(11);
            header.addView(copy, new FrameLayout.LayoutParams(-1, dp(a, 16), Gravity.BOTTOM));
        }
        if (original != null && original.getParent() instanceof View) {
            ((View) original.getParent()).setVisibility(View.INVISIBLE);
        }
        this.weather = weather;
    }

    private final TextView weather;

    void refreshWeather(Activity a) {
        if (!prefs(a).getBoolean("weather", true)) return;
        new Thread(() -> {
            String text = "";
            try {
                if (a.getPackageManager().checkSignatures(a.getPackageName(), HELPER) == PackageManager.SIGNATURE_MATCH) {
                    Bundle data = a.getContentResolver().call(Uri.parse("content://" + HELPER + ".data"), "current", null, null);
                    if (data != null && System.currentTimeMillis() - data.getLong("updated") < 6 * 3600000L) {
                        int code = data.getInt("code");
                        String icon = code == 0 ? "☀" : code >= 95 ? "⚡" : code >= 71 && code <= 77 ? "❄" : code >= 51 ? "☂" : "☁";
                        text = icon + " " + data.getInt("temp") + " °C\n" + data.getString("city");
                    }
                }
            } catch (RuntimeException ignored) { }
            String label = text;
            a.runOnUiThread(() -> { if (!a.isDestroyed()) weather.setText(label); });
        }, "weather-cache").start();
    }

    private void addLayer(FrameLayout root, ImageView view, Bitmap bitmap, int index) {
        view.setImageBitmap(bitmap);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (mode != 0 && intensity > 0) {
            view.setScaleX(1.06f);
            view.setScaleY(1.06f);
        }
        root.addView(view, index, new FrameLayout.LayoutParams(-1, -1));
    }

    void move(KeyEvent event) {
        if (background.getParent() == null || mode == 0 || intensity == 0
                || event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() != 0) return;
        int key = event.getKeyCode();
        float distance = intensity * 0.16f;
        float x = key == KeyEvent.KEYCODE_DPAD_LEFT ? -distance : key == KeyEvent.KEYCODE_DPAD_RIGHT ? distance : 0;
        float y = key == KeyEvent.KEYCODE_DPAD_UP ? -distance : key == KeyEvent.KEYCODE_DPAD_DOWN ? distance : 0;
        if (x == 0 && y == 0) return;
        background.animate().translationX(x).translationY(y).setDuration(160).start();
        if (foreground.getParent() != null) foreground.animate().translationX(-x).translationY(-y).setDuration(160).start();
    }

    private static int dp(Context context, int size) {
        return Math.round(size * context.getResources().getDisplayMetrics().density);
    }
}
