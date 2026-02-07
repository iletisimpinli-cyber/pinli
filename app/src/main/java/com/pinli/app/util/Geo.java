// File: app/src/main/java/com/pinli/app/util/Geo.java
package com.pinli.app.util;

public final class Geo {
    private Geo() {}

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }

    public static double snapLatLng(double valueDeg, double meters) {
        double stepDeg = meters / 111111.0;
        double snapped = Math.round(valueDeg / stepDeg) * stepDeg;
        return snapped;
    }

    public static double snapLng(double lngDeg, double latDeg, double meters) {
        double metersPerDeg = 111111.0 * Math.cos(Math.toRadians(latDeg));
        if (metersPerDeg < 1.0) metersPerDeg = 1.0;
        double stepDeg = meters / metersPerDeg;
        return Math.round(lngDeg / stepDeg) * stepDeg;
    }
}
