package com.cladepa.contextmenu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UsbEventReceiver extends BroadcastReceiver {
    private static final String TAG = "CMOverlay";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        final Context appContext = context.getApplicationContext();
        final boolean attached = action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        final boolean detached = action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED");
        if (!attached && !detached) return;

        Log.d(TAG, "UsbEventReceiver " + (attached ? "ATTACHED" : "DETACHED"));

        final PendingResult pendingResult = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (attached) {
                        try { Thread.sleep(500); } catch (InterruptedException ignored) { }
                        String[] mice = InputDeviceScanner.scanMouseCapable();
                        String[] kbds = InputDeviceScanner.scanKeyboardCapable();
                        boolean c1 = Prefs.mergeDeviceNames(appContext, true, mice);
                        boolean c2 = Prefs.mergeDeviceNames(appContext, false, kbds);
                        Log.d(TAG, "USB attach rescan: mouseChanged=" + c1 + " kbdChanged=" + c2);
                    } else {
                        String[] present = InputDeviceScanner.scanAllPresentNames();
                        boolean c1 = Prefs.pruneMissingDevices(appContext, true, present);
                        boolean c2 = Prefs.pruneMissingDevices(appContext, false, present);
                        Log.d(TAG, "USB detach prune: mouseChanged=" + c1 + " kbdChanged=" + c2);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "UsbEventReceiver failed", e);
                } finally {
                    pendingResult.finish();
                }
            }
        }).start();
    }
}
