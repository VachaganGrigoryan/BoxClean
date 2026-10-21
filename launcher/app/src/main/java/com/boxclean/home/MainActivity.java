package com.boxclean.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * The home activity. It shows the layout picked with the Layout button (Layouts, LayoutPicker) and forwards
 * its lifecycle to it. Plain framework views, no AndroidX, no network.
 */
public class MainActivity extends Activity {
    /** Set by KeyService for the remote's Apps button: open with the "all apps" part focused. */
    static final String EXTRA_SHOW_APPS = "com.boxclean.home.SHOW_APPS";

    private HomeLayout home;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        switch (Layouts.saved(this)) {
            case Layouts.CLASSIC: home = new ClassicHome(this); break;
            case Layouts.RIBBON: home = new RibbonHome(this); break;
            case Layouts.SPOTLIGHT: home = new SpotlightHome(this); break;
            case Layouts.EDITORIAL: home = new EditorialHome(this); break;
            case Layouts.RAIL: home = new RailHome(this); break;
            default: home = new BentoHome(this); break;
        }
        home.handleIntent(getIntent());
    }

    /**
     * Restores nothing: after the picker's recreate() the saved views are the old layout's, matched by id (a
     * ScrollView's state handed to a HorizontalScrollView crashes), and every layout rebuilds itself on resume.
     */
    @Override
    protected void onRestoreInstanceState(Bundle state) {}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        home.handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        home.resume();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        home.back();
    }
}
