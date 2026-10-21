package com.boxclean.home;

import android.content.Intent;

/** One home screen design. MainActivity builds the chosen one and forwards its lifecycle to it. */
interface HomeLayout {
    /** Home pressed again, or the remote's Apps button ({@link MainActivity#EXTRA_SHOW_APPS}). */
    void handleIntent(Intent intent);

    void resume();

    /** This is Home: Back returns to the layout's first app. */
    void back();
}
