// File: app/src/main/java/com/pinli/app/util/Geohash.java
package com.pinli.app.util;

public final class Geohash {
    private static final char[] BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray();

    private Geohash() {}

    public static String encode(double lat, double lon, int precision) {
        boolean evenBit = true;
        int bit = 0;
        int ch = 0;
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        StringBuilder geohash = new StringBuilder();

        while (geohash.length() < precision) {
            double mid;
            if (evenBit) {
                mid = (lonRange[0] + lonRange[1]) / 2.0;
                if (lon >= mid) {
                    ch |= 1 << (4 - bit);
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                mid = (latRange[0] + latRange[1]) / 2.0;
                if (lat >= mid) {
                    ch |= 1 << (4 - bit);
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }

            evenBit = !evenBit;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32[ch]);
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }

    public static String prefixForRadiusMeters(double radiusMeters) {
        // Conservative mapping (approx cell sizes). Smaller radius => longer prefix.
        if (radiusMeters <= 150) return "8";
        if (radiusMeters <= 600) return "7";
        if (radiusMeters <= 2500) return "6";
        if (radiusMeters <= 20000) return "5";
        return "4";
    }

    public static String[] rangeForPrefix(String prefix) {
        // Firestore lexicographic range: [prefix, prefix + '\uf8ff']
        return new String[]{prefix, prefix + "\uf8ff"};
    }
}
