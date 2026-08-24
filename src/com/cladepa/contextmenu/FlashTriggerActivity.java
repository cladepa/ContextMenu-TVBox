package com.cladepa.contextmenu;

import android.app.Activity;
import android.os.Bundle;

public class FlashTriggerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OverlayManager.flash(getApplicationContext());
        finish();
    }
}
