// File: app/src/main/java/com/pinli/app/util/Time.java
package com.pinli.app.util;

public final class Time {
    private Time() {}
    public static long now() { return System.currentTimeMillis(); }
    public static boolean isExpired(long expiresAt) { return expiresAt > 0 && expiresAt <= now(); }
}
