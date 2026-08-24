package com.cladepa.contextmenu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CommandReceiver extends BroadcastReceiver {
    private static final String TAG = "CMOverlay";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "CommandReceiver.onReceive action=" + action);
        if (action == null) return;

        final Context appContext = context.getApplicationContext();

        if (action.equals("tv.contextmenu.SHOW")) {
            if (intent.hasExtra("x") && intent.hasExtra("y")) {
                int x = intent.getIntExtra("x", 100);
                int y = intent.getIntExtra("y", 100);
                String devices = intent.getStringExtra("devices");
                OverlayManager.show(appContext, x, y, devices);
                return;
            }

            final PendingResult pendingResult = goAsync();
            MenuTrigger.showViaDiscovery(appContext, new MenuTrigger.Callback() {
                @Override
                public void onDone() {
                    pendingResult.finish();
                }
            });

        } else if (action.equals("tv.contextmenu.HIDE")) {
            OverlayManager.hide(appContext);
        } else if (action.equals("tv.contextmenu.FLASH")) {
            OverlayManager.flash(appContext);
        }
    }
}
