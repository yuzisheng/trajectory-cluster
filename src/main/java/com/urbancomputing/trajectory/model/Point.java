package com.urbancomputing.trajectory.model;

/**
 * Point Class
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class Point {
    /**
     * longitude or x
     */
    private double lng;
    /**
     * latitude or y
     */
    private double lat;

    public Point() {
        this.lng = 0.0;
        this.lat = 0.0;
    }

    public Point(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }

    public double getCoord(int i) {
        if (i == 0) {
            return lng;
        } else {
            return lat;
        }
    }

    public void setCoord(int index, double value) {
        if (index == 0) {
            lng = value;
        } else {
            lat = value;
        }
    }

    @Override
    public String toString() {
        return "POINT (" + lng + " " + lat + ")";
    }
}
