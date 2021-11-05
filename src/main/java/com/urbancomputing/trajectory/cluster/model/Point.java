package com.urbancomputing.trajectory.cluster.model;

/**
 * 点类
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class Point {
    /**
     * 经度
     */
    private final double lng;

    /**
     * 纬度
     */
    private final double lat;

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

    @Override
    public String toString() {
        return "POINT (" + lng + " " + lat + ")";
    }
}
