package com.gathin.tvhome;

import android.content.SharedPreferences;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;

public class AutomotiveTileMigrationTest {
    @Test public void v3TileValuesBackedUpOnceAndOtherPreferencesSurvive() {
        Map<String, Object> values = new HashMap<>();
        values.put("tileSchema", 3);
        values.put("tileGlow", false);
        values.put("tileMode", 2);
        values.put("plate", 20);
        values.put("wall", 4);
        values.put("header", 37);
        Map<String, Object> pending = new HashMap<>();
        SharedPreferences.Editor editor = (SharedPreferences.Editor) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{SharedPreferences.Editor.class}, (self, method, args) -> {
                    if (method.getName().equals("apply")) {
                        values.putAll(pending);
                        pending.clear();
                        return null;
                    }
                    if (method.getName().equals("putInt") || method.getName().equals("putBoolean")) {
                        pending.put((String) args[0], args[1]);
                        return self;
                    }
                    throw new AssertionError(method);
                });
        SharedPreferences prefs = (SharedPreferences) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{SharedPreferences.class}, (self, method, args) -> {
                    if (method.getName().equals("edit")) return editor;
                    if (method.getName().equals("getInt")) return values.getOrDefault(args[0], args[1]);
                    throw new AssertionError(method);
                });
        Automotive.migrateTiles(prefs);
        assertEquals(0, values.get("tileMode"));
        assertEquals(100, values.get("plate"));
        assertEquals(false, values.get("tileGlow"));
        assertEquals(2, values.get("v3TileMode"));
        assertEquals(20, values.get("v3Plate"));
        assertEquals(4, values.get("tileSchema"));
        assertEquals(4, values.get("wall"));
        assertEquals(37, values.get("header"));
        values.put("tileMode", 1);
        values.put("plate", 0);
        values.put("tileGlow", false);
        Automotive.migrateTiles(prefs);
        assertEquals(1, values.get("tileMode"));
        assertEquals(0, values.get("plate"));
        assertEquals(false, values.get("tileGlow"));
    }
}
