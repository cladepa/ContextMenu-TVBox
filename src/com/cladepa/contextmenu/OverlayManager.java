package com.cladepa.contextmenu;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.widget.Toast;

public class OverlayManager {

    private static final String TAG = "CMOverlay";

    private static final int TYPE_OVERLAY = 2038; // TYPE_APPLICATION_OVERLAY, API26+
    private static final int TYPE_PHONE   = 2002; // fallback pre-26

    private static final String CLIP_CMD =
            "ANDROID_ROOT=/system ANDROID_DATA=/data CLASSPATH=/data/local/tmp/clip.jar app_process /system/bin Clip";

    private static class Item {
        String label;
        String cmd;
        Item[] children;
        boolean isBack;
        boolean isPasteAction;
        boolean isShareAction;
        boolean isHistoryAction;

        Item(String label, String cmd) {
            this.label = label;
            this.cmd = cmd;
        }

        Item(String label, Item[] children) {
            this.label = label;
            this.children = children;
        }

        static Item back() {
            Item i = new Item("\u2039 Назад", (String) null);
            i.isBack = true;
            return i;
        }
    }

    private static WindowManager wm;
    private static LinearLayout menuRoot;
    private static TextView[] itemViews;
    private static Item[] currentItems;
    private static Item[] rootItems;
    private static int selectedIndex = 0;
    private static Context appContextRef;
    private static Context uiContext;
    private static int lastX = 0, lastY = 0;

    public static void show(final Context context, int x, int y, String devicesCsv) {
        hide(context);

        appContextRef = context.getApplicationContext();
        uiContext = context;
        lastX = x;
        lastY = y;

        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        String[] devices = parseDevices(devicesCsv);
        rootItems = buildRootItems(devices);
        currentItems = rootItems;

        menuRoot = new LinearLayout(context);
        menuRoot.setOrientation(LinearLayout.VERTICAL);
        menuRoot.setBackgroundColor(Color.parseColor("#E6222222"));
        menuRoot.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        rebuildItemViews();

        int windowType = Build.VERSION.SDK_INT >= 26 ? TYPE_OVERLAY : TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        int screenW = 1920, screenH = 1080;
        int menuW = dp(context, 220);
        int maxPossibleItems = Math.max(currentItems.length, devices.length + 2);
        int menuH = dp(context, 44) * maxPossibleItems + dp(context, 8);
        params.x = Math.max(0, Math.min(x, screenW - menuW));
        params.y = Math.max(0, Math.min(y, screenH - menuH));

        menuRoot.setFocusableInTouchMode(true);
        menuRoot.setFocusable(true);

        menuRoot.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        selectedIndex = Math.max(0, selectedIndex - 1);
                        highlight();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        selectedIndex = Math.min(currentItems.length - 1, selectedIndex + 1);
                        highlight();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        selectItem(selectedIndex);
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        if (currentItems != rootItems) {
                            currentItems = rootItems;
                            selectedIndex = 0;
                            rebuildItemViews();
                        } else {
                            hide(appContextRef);
                        }
                        return true;
                }
                return false;
            }
        });

        Log.d(TAG, "attempting addView at x=" + params.x + " y=" + params.y + " type=" + windowType);
        try {
            wm.addView(menuRoot, params);
            menuRoot.requestFocus();
            Log.d(TAG, "addView OK");
        } catch (Exception e) {
            Log.e(TAG, "addView FAILED", e);
        }
    }

    private static String[] parseDevices(String csv) {
        if (csv == null || csv.trim().length() == 0) return new String[0];
        String[] raw = csv.split(",");
        String[] trimmed = new String[raw.length];
        for (int i = 0; i < raw.length; i++) trimmed[i] = raw[i].trim();
        return trimmed;
    }

    private static Item[] buildRootItems(String[] devices) {
        String[] favorites = parseDevices(Prefs.favoriteDevices(appContextRef));
        java.util.List<String> favList = new java.util.ArrayList<String>();
        java.util.List<String> otherList = new java.util.ArrayList<String>();
        for (int i = 0; i < devices.length; i++) {
            boolean isFav = false;
            for (int j = 0; j < favorites.length; j++) {
                if (favorites[j].equals(devices[i])) { isFav = true; break; }
            }
            if (isFav) favList.add(devices[i]); else otherList.add(devices[i]);
        }

        Item[] copyFrom = buildDeviceSubmenu(favList, otherList, true);
        Item[] pasteTo = buildDeviceSubmenu(favList, otherList, false);
        pasteTo = prependAllOption(pasteTo);

        Item pasteItem = new Item("Вставить", "input keyevent 279");
        pasteItem.isPasteAction = true;

        Item selectAllItem = new Item("Выделить всё", selectAllCmd());

        Item shareItem = new Item("Поделиться", (String) null);
        shareItem.isShareAction = true;

        Item historyItem = new Item("История", (String) null);
        historyItem.isHistoryAction = true;

        return new Item[]{
                new Item("Копировать", copyAndSyncCmd(278)),
                new Item("Вырезать", copyAndSyncCmd(277)),
                pasteItem,
                selectAllItem,
                shareItem,
                historyItem,
                new Item("Вставить из \u203A", copyFrom),
                new Item("Отправить в \u203A", pasteTo)
        };
    }

    private static Item[] buildDeviceSubmenu(java.util.List<String> favs,
                                              java.util.List<String> others, boolean isCopyFrom) {
        java.util.List<Item> items = new java.util.ArrayList<Item>();
        items.add(Item.back());
        if (isCopyFrom) {
            Item inboxItem = new Item("Входящие", copyFromInboxCmd());
            inboxItem.isPasteAction = true;
            items.add(inboxItem);
        }
        for (int i = 0; i < favs.size(); i++) {
            String dev = favs.get(i);
            Item it = isCopyFrom ? new Item(dev, copyFromCmd(dev)) : new Item(dev, pasteToCmd(dev));
            if (isCopyFrom) it.isPasteAction = true;
            items.add(it);
        }
        if (!others.isEmpty()) {
            Item[] otherItems = new Item[others.size() + 1];
            otherItems[0] = Item.back();
            for (int i = 0; i < others.size(); i++) {
                String dev = others.get(i);
                Item it = isCopyFrom ? new Item(dev, copyFromCmd(dev)) : new Item(dev, pasteToCmd(dev));
                if (isCopyFrom) it.isPasteAction = true;
                otherItems[i + 1] = it;
            }
            items.add(new Item("Остальные \u203A", otherItems));
        }
        return items.toArray(new Item[0]);
    }

    private static Item[] prependAllOption(Item[] items) {
        Item[] result = new Item[items.length + 1];
        result[0] = items[0];
        result[1] = new Item("Всем", pasteToCmd("all"));
        for (int i = 1; i < items.length; i++) result[i + 1] = items[i];
        return result;
    }

    private static String selectAllCmd() {
        String kbCsv = Prefs.keyboardDevices(appContextRef);
        if (kbCsv == null || kbCsv.trim().length() == 0) {
            kbCsv = "HAOBO Technology USB Composite Device Keyboard";
        }
        return "KBNAMES='" + kbCsv + "'; "
                + "for f in /sys/class/input/event*/device/name; do "
                + "n=$(cat \"$f\"); "
                + "case \",$KBNAMES,\" in "
                + "*\",$n,\"*) "
                + "d=\"${f%/device/name}\"; DEV=\"/dev/input/${d##*/}\"; "
                + "sendevent \"$DEV\" 1 29 1; sendevent \"$DEV\" 0 0 0; "
                + "sendevent \"$DEV\" 1 30 1; sendevent \"$DEV\" 0 0 0; "
                + "sendevent \"$DEV\" 1 30 0; sendevent \"$DEV\" 0 0 0; "
                + "sendevent \"$DEV\" 1 29 0; sendevent \"$DEV\" 0 0 0; "
                + ";; esac; "
                + "done";
    }

    private static String copyAndSyncCmd(int keycode) {
        String dir = Prefs.bufferDir(appContextRef);
        String name = Prefs.deviceName(appContextRef);
        return "input keyevent " + keycode + "; sleep 0.1; CONTENT=$(" + CLIP_CMD
                + "); printf '%s' \"$CONTENT\" > '" + dir + "/" + name + ".txt'; "
                + historyAppendCmd(dir, name);
    }

    private static String historyAppendCmd(String dir, String name) {
        return "TS=$(date '+%Y-%m-%dT%H:%M:%S'); "
                + "printf '\\n===ENTRY %s===\\n%s\\n' \"$TS\" \"$CONTENT\" >> '"
                + dir + "/clip_hist-" + name + ".txt'";
    }

    private static String copyFromCmd(String device) {
        String dir = Prefs.bufferDir(appContextRef);
        return "CONTENT=$(cat '" + dir + "/" + device + ".txt'); "
                + CLIP_CMD + " \"$CONTENT\"; input keyevent 279";
    }

    private static String copyFromInboxCmd() {
        String dir = Prefs.bufferDir(appContextRef);
        String name = Prefs.deviceName(appContextRef);
        return "CONTENT=$(cat '" + dir + "/to_" + name + ".txt'); "
                + CLIP_CMD + " \"$CONTENT\"; input keyevent 279";
    }

    private static String pasteToCmd(String device) {
        String dir = Prefs.bufferDir(appContextRef);
        return "input keyevent 278; sleep 0.1; CONTENT=$(" + CLIP_CMD
                + "); printf '%s' \"$CONTENT\" > '" + dir + "/to_" + device + ".txt'";
    }

    private static void rebuildItemViews() {
        menuRoot.removeAllViews();
        itemViews = new TextView[currentItems.length];
        for (int i = 0; i < currentItems.length; i++) {
            final int index = i;
            TextView tv = new TextView(uiContext);
            tv.setText(currentItems[i].label);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setPadding(dp(uiContext, 20), dp(uiContext, 12), dp(uiContext, 20), dp(uiContext, 12));
            tv.setClickable(true);
            tv.setFocusable(true);

            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectItem(index);
                }
            });

            tv.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                        selectedIndex = index;
                        highlight();
                    }
                    return false;
                }
            });

            menuRoot.addView(tv);
            itemViews[i] = tv;
        }
        selectedIndex = 0;
        highlight();
    }

    private static void selectItem(int index) {
        Item item = currentItems[index];

        if (item.isBack) {
            currentItems = rootItems;
            selectedIndex = 0;
            rebuildItemViews();
            return;
        }

        if (item.children != null) {
            currentItems = item.children;
            selectedIndex = 0;
            rebuildItemViews();
            return;
        }

        if (item.isHistoryAction) {
            final Context ctx3 = appContextRef;
            hide(ctx3);
            HistoryOverlay.show(ctx3);
            return;
        }

        if (item.isShareAction) {
            final Context ctx2 = appContextRef;
            hide(ctx2);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Root.exec("input keyevent 278");
                    try { Thread.sleep(150); } catch (InterruptedException ignored) { }
                    final String text = Root.exec(CLIP_CMD);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            shareText(ctx2, text);
                        }
                    });
                }
            }).start();
            return;
        }

        final String label = item.label;
        final String cmd = item.cmd;
        final Context ctx = appContextRef;

        hide(ctx);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Root.exec(cmd);
            }
        }).start();

        Toast.makeText(ctx, label, Toast.LENGTH_SHORT).show();
    }

    private static void shareText(Context ctx, String text) {
        if (text == null || text.length() == 0) {
            Toast.makeText(ctx, "Нечего отправлять (пусто)", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        Intent chooser = Intent.createChooser(sendIntent, "Поделиться");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(chooser);
    }

    public static void hide(Context context) {
        if (menuRoot != null && wm != null) {
            try { wm.removeView(menuRoot); } catch (Exception e) { /* ignore */ }
            menuRoot = null;
        }
    }

    public static void flash(final Context context) {
        flashWith(context, Prefs.flashCorner(context), Prefs.flashCount(context),
                Prefs.flashPeriodMs(context), Prefs.flashColor(context), Prefs.flashSizeDp(context));
    }

    public static void flashWith(final Context context, String corner, int count,
                                  int periodMs, int color, int sizeDp) {
        final WindowManager wm2 = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final View dot = new View(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        dot.setBackground(gd);

        int size = dp(context, sizeDp);
        int margin = dp(context, 12);
        int windowType = Build.VERSION.SDK_INT >= 26 ? TYPE_OVERLAY : TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size, windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        int gravity;
        if ("TR".equals(corner)) gravity = Gravity.TOP | Gravity.END;
        else if ("BL".equals(corner)) gravity = Gravity.BOTTOM | Gravity.START;
        else if ("BR".equals(corner)) gravity = Gravity.BOTTOM | Gravity.END;
        else gravity = Gravity.TOP | Gravity.START;

        params.gravity = gravity;
        params.x = margin;
        params.y = margin;

        try {
            wm2.addView(dot, params);
        } catch (Exception e) {
            Log.e(TAG, "flash addView failed", e);
            return;
        }

        final Handler handler = new Handler();
        final int[] toggle = {0};
        final int totalToggles = Math.max(1, count) * 2;
        final int stepDelay = Math.max(30, periodMs / 2);
        final Runnable blink = new Runnable() {
            @Override
            public void run() {
                toggle[0]++;
                dot.setVisibility(dot.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                if (toggle[0] < totalToggles) {
                    handler.postDelayed(this, stepDelay);
                } else {
                    try { wm2.removeView(dot); } catch (Exception e) { /* ignore */ }
                }
            }
        };
        handler.postDelayed(blink, stepDelay);
    }

    private static void highlight() {
        for (int i = 0; i < itemViews.length; i++) {
            itemViews[i].setBackgroundColor(i == selectedIndex
                    ? Color.parseColor("#4CAF50") : Color.TRANSPARENT);
        }
    }

    private static int dp(Context context, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                context.getResources().getDisplayMetrics());
    }
}
