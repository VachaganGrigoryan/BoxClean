package com.boxclean.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Remote buttons whose stock handler is gone: app_keys (config.xml) open an app, and Apps (KEYCODE_ALL_APPS)
 * opens this launcher's app grid. Key filtering only: it gets no accessibility events and cannot read the
 * screen. Every other key passes through untouched. Adapted from CleanYourTV's HomeKeyService (MIT, see NOTICE).
 * build-install.sh enables it; a reinstall disables it, so the script re-enables it every time.
 */
public class KeyService extends AccessibilityService {
    private Map<Integer, String> appKeys = Collections.emptyMap();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        appKeys = RemoteKeys.parseAppKeys(
                Arrays.asList(getResources().getStringArray(R.array.app_keys)), KeyEvent::keyCodeFromString);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        String app = appKeys.get(event.getKeyCode());
        boolean allApps = event.getKeyCode() == KeyEvent.KEYCODE_ALL_APPS;
        if (app == null && !allApps) return false;
        // Consume DOWN and UP so nothing else sees the key; act once, on UP.
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            if (app != null) {
                RemoteKeys.launch(this, app);
            } else {
                startActivity(new Intent(this, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_SHOW_APPS, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
