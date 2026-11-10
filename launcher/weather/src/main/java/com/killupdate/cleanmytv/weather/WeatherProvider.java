package com.killupdate.cleanmytv.weather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

public final class WeatherProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        int caller = Binder.getCallingUid();
        PackageManager pm = getContext().getPackageManager();
        try {
            if (caller != pm.getApplicationInfo("com.killupdate.cleanmytv", 0).uid
                    || pm.checkSignatures(caller, Process.myUid()) != PackageManager.SIGNATURE_MATCH) {
                throw new SecurityException("Untrusted caller");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Launcher missing", e);
        }
        if (!"current".equals(method)) throw new IllegalArgumentException("Unknown method");
        SharedPreferences cache = getContext().getSharedPreferences("weather", 0);
        Bundle result = new Bundle();
        result.putString("city", cache.getString("city", ""));
        result.putInt("temp", cache.getInt("temp", 0));
        result.putInt("code", cache.getInt("code", 0));
        result.putLong("updated", cache.getLong("updated", 0));
        return result;
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String order) { throw unsupported(); }
    @Override public String getType(Uri uri) { throw unsupported(); }
    @Override public Uri insert(Uri uri, ContentValues values) { throw unsupported(); }
    @Override public int update(Uri uri, ContentValues values, String s, String[] a) { throw unsupported(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw unsupported(); }
    private UnsupportedOperationException unsupported() { return new UnsupportedOperationException("Read cache via call"); }
}
