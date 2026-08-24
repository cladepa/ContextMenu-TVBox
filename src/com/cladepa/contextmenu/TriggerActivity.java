package com.cladepa.contextmenu;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class TriggerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context appContext = getApplicationContext();
        MenuTrigger.showViaDiscovery(appContext, new MenuTrigger.Callback() {
            @Override
            public void onDone() {
                finish();
            }
        });
    }
}
