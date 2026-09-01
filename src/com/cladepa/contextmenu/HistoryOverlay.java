package com.cladepa.contextmenu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryOverlay {

    private static final String TAG = "CMOverlay";
    private static final int TYPE_OVERLAY = 2038;
    private static final int TYPE_PHONE   = 2002;

    private static WindowManager wm;
    private static View rootView;
    private static WindowManager.LayoutParams params;
    private static Context appContextRef;

    private static List<TextView> entryViews = new ArrayList<TextView>();
    private static List<String> entryTexts = new ArrayList<String>();
    private static int selectedIndex = -1;

    public static void show(final Context context) {
        hide(context);
        final Context ctx = context.getApplicationContext();
        appContextRef = ctx;
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#EE1A1A1A"));

        LinearLayout titleBar = new LinearLayout(ctx);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setBackgroundColor(Color.parseColor("#333333"));
        titleBar.setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8));

        TextView title = new TextView(ctx);
        title.setText("\u2261 История");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleBar.addView(title, titleLp);

        TextView closeBtn = new TextView(ctx);
        closeBtn.setText("\u2715");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        closeBtn.setPadding(dp(ctx, 12), 0, dp(ctx, 4), 0);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { hide(ctx); }
        });
        titleBar.addView(closeBtn);

        root.addView(titleBar);

        ScrollView scroll = new ScrollView(ctx);
        final LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scrollLp);

        final TextView resizeHandle = new TextView(ctx);
        resizeHandle.setText("\u25E2");
        resizeHandle.setTextColor(Color.parseColor("#888888"));
        resizeHandle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        handleLp.gravity = Gravity.END;
        root.addView(resizeHandle, handleLp);

        rootView = root;

        int windowType = Build.VERSION.SDK_INT >= 26 ? TYPE_OVERLAY : TYPE_PHONE;
        int savedW = Prefs.historyWinW(ctx);
        int savedH = Prefs.historyWinH(ctx);
        int savedX = Prefs.historyWinX(ctx);
        int savedY = Prefs.historyWinY(ctx);
        params = new WindowManager.LayoutParams(
                savedW > 0 ? savedW : dp(ctx, 420),
                savedH > 0 ? savedH : dp(ctx, 480),
                windowType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = savedX >= 0 ? savedX : dp(ctx, 200);
        params.y = savedY >= 0 ? savedY : dp(ctx, 100);

        root.setFocusableInTouchMode(true);
        root.setFocusable(true);

        titleBar.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x; startY = params.y;
                        touchX = event.getRawX(); touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = startX + (int) (event.getRawX() - touchX);
                        params.y = startY + (int) (event.getRawY() - touchY);
                        try { wm.updateViewLayout(rootView, params); } catch (Exception ignored) { }
                        return true;
                    case MotionEvent.ACTION_UP:
                        Prefs.setHistoryWindow(ctx, params.x, params.y, params.width, params.height);
                        return true;
                }
                return false;
            }
        });

        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            private int startW, startH;
            private float touchX, touchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startW = params.width; startH = params.height;
                        touchX = event.getRawX(); touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int newW = startW + (int) (event.getRawX() - touchX);
                        int newH = startH + (int) (event.getRawY() - touchY);
                        params.width = Math.max(dp(ctx, 220), newW);
                        params.height = Math.max(dp(ctx, 180), newH);
                        try { wm.updateViewLayout(rootView, params); } catch (Exception ignored) { }
                        return true;
                    case MotionEvent.ACTION_UP:
                        Prefs.setHistoryWindow(ctx, params.x, params.y, params.width, params.height);
                        return true;
                }
                return false;
            }
        });

        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        if (!entryViews.isEmpty()) {
                            selectedIndex = Math.max(0, selectedIndex - 1);
                            highlight();
                        }
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        if (!entryViews.isEmpty()) {
                            selectedIndex = Math.min(entryViews.size() - 1, selectedIndex + 1);
                            highlight();
                        }
                        return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        if (selectedIndex >= 0 && selectedIndex < entryTexts.size()) {
                            selectEntry(ctx, entryTexts.get(selectedIndex));
                        }
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        hide(ctx);
                        return true;
                }
                return false;
            }
        });

        Log.d(TAG, "history addView");
        try {
            wm.addView(root, params);
            root.requestFocus();
        } catch (Exception e) {
            Log.e(TAG, "history addView failed", e);
            return;
        }

        loadEntries(ctx, listContainer);
    }

    private static void loadEntries(final Context ctx, final LinearLayout listContainer) {
        final String dir = Prefs.bufferDir(ctx);
        final String name = Prefs.deviceName(ctx);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String content = Root.exec(
                        "cat '" + dir + "/clip_hist-" + name + ".txt' 2>/dev/null");
                final List<String[]> parsed = parseHistory(content);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        renderEntries(ctx, listContainer, parsed);
                    }
                });
            }
        }).start();
    }

    private static List<String[]> parseHistory(String content) {
        List<String[]> result = new ArrayList<String[]>();
        if (content == null || content.trim().length() == 0) return result;
        Pattern p = Pattern.compile("===ENTRY ([^=\\n]+)===\\n");
        Matcher m = p.matcher(content);
        List<int[]> spans = new ArrayList<int[]>();
        List<String> timestamps = new ArrayList<String>();
        while (m.find()) {
            spans.add(new int[]{m.end()});
            timestamps.add(m.group(1));
        }
        for (int i = 0; i < spans.size(); i++) {
            int start = spans.get(i)[0];
            int end = (i + 1 < spans.size())
                    ? findMarkerStart(content, spans.get(i + 1)[0])
                    : content.length();
            String text = content.substring(start, Math.max(start, end)).trim();
            if (text.length() > 0) {
                result.add(new String[]{timestamps.get(i), text});
            }
        }
        java.util.Collections.reverse(result);
        return result;
    }

    private static int findMarkerStart(String content, int nextEntryTextStart) {
        int idx = content.lastIndexOf("===ENTRY", nextEntryTextStart);
        return idx >= 0 ? idx : nextEntryTextStart;
    }

    private static void renderEntries(Context ctx, LinearLayout listContainer, List<String[]> entries) {
        listContainer.removeAllViews();
        entryViews.clear();
        entryTexts.clear();
        selectedIndex = entries.isEmpty() ? -1 : 0;

        int fontSp = Prefs.historyFontSizeSp(ctx);

        if (entries.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("История пуста");
            empty.setTextColor(Color.parseColor("#888888"));
            empty.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));
            listContainer.addView(empty);
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            final int index = i;
            String[] entry = entries.get(i);
            String ts = entry[0];
            String text = entry[1];

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8));
            row.setClickable(true);
            row.setFocusable(true);

            TextView tsView = new TextView(ctx);
            tsView.setText(ts);
            tsView.setTextColor(Color.parseColor("#4CAF50"));
            tsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(9, fontSp - 3));
            row.addView(tsView);

            TextView textView = new TextView(ctx);
            textView.setText(text);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp);
            textView.setMaxLines(3);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(textView);

            View divider = new View(ctx);
            divider.setBackgroundColor(Color.parseColor("#333333"));
            LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1));
            dividerLp.topMargin = dp(ctx, 8);
            row.addView(divider, dividerLp);

            final Context fctx = ctx;
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedIndex = index;
                    selectEntry(fctx, entryTexts.get(index));
                }
            });

            listContainer.addView(row);
            entryViews.add(textView);
            entryTexts.add(text);
        }
        highlight();
    }

    private static void highlight() {
        for (int i = 0; i < entryViews.size(); i++) {
            View row = (View) entryViews.get(i).getParent();
            row.setBackgroundColor(i == selectedIndex ? Color.parseColor("#2E7D32") : Color.TRANSPARENT);
        }
    }

    private static void selectEntry(final Context ctx, final String text) {
        hide(ctx);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String b64 = android.util.Base64.encodeToString(
                        text.getBytes(), android.util.Base64.NO_WRAP);
                String cmd = "echo '" + b64 + "' | base64 -d > /data/local/tmp/cm_hist_paste.txt; "
                        + "CONTENT=$(cat /data/local/tmp/cm_hist_paste.txt); "
                        + "ANDROID_ROOT=/system ANDROID_DATA=/data "
                        + "CLASSPATH=/data/local/tmp/clip.jar app_process /system/bin Clip \"$CONTENT\"; "
                        + "input keyevent 279";
                Root.exec(cmd);
            }
        }).start();
        Toast.makeText(ctx, "Вставлено из истории", Toast.LENGTH_SHORT).show();
    }

    public static void hide(Context context) {
        if (rootView != null && wm != null) {
            try { wm.removeView(rootView); } catch (Exception e) { /* ignore */ }
            rootView = null;
        }
    }

    private static int dp(Context context, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                context.getResources().getDisplayMetrics());
    }
}
