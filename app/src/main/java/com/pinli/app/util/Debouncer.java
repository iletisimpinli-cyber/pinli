// File: app/src/main/java/com/pinli/app/util/Debouncer.java
package com.pinli.app.util;

import android.os.Handler;
import android.os.Looper;

public final class Debouncer {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable last;

    public void submit(long delayMs, Runnable action) {
        if (last != null) handler.removeCallbacks(last);
        last = action;
        handler.postDelayed(last, delayMs);
    }

    public void cancel() {
        if (last != null) handler.removeCallbacks(last);
        last = null;
    }
}
