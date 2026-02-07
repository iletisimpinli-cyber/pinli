// File: app/src/main/java/com/pinli/app/util/Logx.java
package com.pinli.app.util;

import android.util.Log;

public final class Logx {
    private Logx() {}
    public static void e(String tag, String msg, Throwable t) { Log.e(tag, msg, t); }
    public static void d(String tag, String msg) { Log.d(tag, msg); }
}
