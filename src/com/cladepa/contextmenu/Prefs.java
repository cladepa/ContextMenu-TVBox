package com.cladepa.contextmenu;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String PREFS_NAME = "cm_prefs";
    public static final String DEFAULT_BUFFER_DIR = "/storage/emulated/0/Zametki_ALL/claude_bufer";

    public static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String bufferDir(Context ctx) {
        return get(ctx).getString("buffer_dir", DEFAULT_BUFFER_DIR);
    }

    public static void setBufferDir(Context ctx, String path) {
        get(ctx).edit().putString("buffer_dir", path).commit();
    }

    public static String stfolderPath(Context ctx) {
        String def = get(ctx).getString("stfolder_path", null);
        if (def != null) return def;
        String bufDir = bufferDir(ctx);
        int idx = bufDir.lastIndexOf('/');
        String parent = idx > 0 ? bufDir.substring(0, idx) : bufDir;
        return parent + "/.stfolder";
    }

    public static void setStfolderPath(Context ctx, String path) {
        get(ctx).edit().putString("stfolder_path", path).commit();
    }

    public static int historyFontSizeSp(Context ctx) {
        return get(ctx).getInt("history_font_sp", 14);
    }

    public static void setHistoryFontSizeSp(Context ctx, int sp) {
        get(ctx).edit().putInt("history_font_sp", sp).commit();
    }

    public static String deviceName(Context ctx) {
        String saved = get(ctx).getString("device_name", null);
        if (saved != null) return saved;
        try {
            String sys = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), "device_name");
            if (sys != null && sys.trim().length() > 0) return sys.trim().replace(" ", "_");
        } catch (Exception ignored) { }
        return "tvbox";
    }

    public static void setDeviceName(Context ctx, String name) {
        get(ctx).edit().putString("device_name", name).commit();
    }

    public static String mouseDevices(Context ctx) {
        return get(ctx).getString("mouse_devices", "");
    }

    public static void setMouseDevices(Context ctx, String csv) {
        get(ctx).edit().putString("mouse_devices", csv).commit();
    }

    public static String keyboardDevices(Context ctx) {
        return get(ctx).getString("keyboard_devices", "");
    }

    public static void setKeyboardDevices(Context ctx, String csv) {
        get(ctx).edit().putString("keyboard_devices", csv).commit();
    }

    public static String[] csvToArray(String csv) {
        if (csv == null || csv.trim().length() == 0) return new String[0];
        String[] raw = csv.split(",");
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (String r : raw) {
            String t = r.trim();
            if (t.length() > 0) out.add(t);
        }
        return out.toArray(new String[0]);
    }

    public static String arrayToCsv(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /** Добавляет новые имена в список, если их там ещё нет. Возвращает true если что-то изменилось. */
    public static boolean mergeDeviceNames(Context ctx, boolean isMouse, String[] discovered) {
        String csv = isMouse ? mouseDevices(ctx) : keyboardDevices(ctx);
        java.util.List<String> current = new java.util.ArrayList<String>(java.util.Arrays.asList(csvToArray(csv)));
        boolean changed = false;
        for (String d : discovered) {
            if (!current.contains(d)) {
                current.add(d);
                changed = true;
            }
        }
        if (changed) {
            String result = arrayToCsv(current);
            if (isMouse) setMouseDevices(ctx, result); else setKeyboardDevices(ctx, result);
        }
        return changed;
    }

    /** Убирает из списка имена, которых больше нет среди currentlyPresent. Возвращает true если что-то изменилось. */
    public static boolean pruneMissingDevices(Context ctx, boolean isMouse, String[] currentlyPresent) {
        String csv = isMouse ? mouseDevices(ctx) : keyboardDevices(ctx);
        java.util.List<String> current = new java.util.ArrayList<String>(java.util.Arrays.asList(csvToArray(csv)));
        java.util.Set<String> presentSet = new java.util.HashSet<String>(java.util.Arrays.asList(currentlyPresent));
        boolean changed = current.retainAll(presentSet);
        if (changed) {
            String result = arrayToCsv(current);
            if (isMouse) setMouseDevices(ctx, result); else setKeyboardDevices(ctx, result);
        }
        return changed;
    }

    public static String flashCorner(Context ctx) {
        return get(ctx).getString("flash_corner", "TL");
    }

    public static void setFlashCorner(Context ctx, String corner) {
        get(ctx).edit().putString("flash_corner", corner).commit();
    }

    public static String favoriteDevices(Context ctx) {
        return get(ctx).getString("favorite_devices", "");
    }

    public static void setFavoriteDevices(Context ctx, String csv) {
        get(ctx).edit().putString("favorite_devices", csv).commit();
    }

    public static int flashCount(Context ctx) {
        return get(ctx).getInt("flash_count", 4);
    }

    public static void setFlashCount(Context ctx, int count) {
        get(ctx).edit().putInt("flash_count", count).commit();
    }

    public static int flashPeriodMs(Context ctx) {
        return get(ctx).getInt("flash_period_ms", 300);
    }

    public static void setFlashPeriodMs(Context ctx, int ms) {
        get(ctx).edit().putInt("flash_period_ms", ms).commit();
    }

    public static int flashColor(Context ctx) {
        return get(ctx).getInt("flash_color", 0xFF4CAF50);
    }

    public static void setFlashColor(Context ctx, int color) {
        get(ctx).edit().putInt("flash_color", color).commit();
    }

    public static int flashSizeDp(Context ctx) {
        return get(ctx).getInt("flash_size_dp", 16);
    }

    public static void setFlashSizeDp(Context ctx, int dp) {
        get(ctx).edit().putInt("flash_size_dp", dp).commit();
    }
}
