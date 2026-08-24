package com.cladepa.contextmenu;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class MenuTrigger {
    private static final String TAG = "CMOverlay";

    public interface Callback {
        void onDone();
    }

    public static void showViaDiscovery(final Context appContext, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String dir = Prefs.bufferDir(appContext);
                    String mice = Prefs.mouseDevices(appContext);
                    String myName = Prefs.deviceName(appContext);
                    String cmd = "sh /data/local/tmp/discover_menu.sh '" + dir + "' '"
                            + mice + "' '" + myName + "'";
                    String out = Root.exec(cmd);
                    Log.d(TAG, "discover_menu.sh -> " + out);
                    String[] parts = out.split("\\|", -1);
                    if (parts.length < 2) {
                        Log.e(TAG, "discover_menu.sh output malformed");
                        if (callback != null) callback.onDone();
                        return;
                    }
                    final int x = Integer.parseInt(parts[0].trim());
                    final int y = Integer.parseInt(parts[1].trim());
                    final String devices = parts.length > 2 ? parts[2].trim() : "";
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            OverlayManager.show(appContext, x, y, devices);
                            if (callback != null) callback.onDone();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "SHOW discovery failed", e);
                    if (callback != null) callback.onDone();
                }
            }
        }).start();
    }
}
