package com.urbancomputing.trajectory.cluster.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 轨迹类
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class Trajectory {
    /**
     * 轨迹标识
     */
    private final String tid;

    /**
     * 轨迹点
     */
    private final Point[] points;

    public Trajectory(String tid, Point[] points) {
        this.tid = tid;
        this.points = points;
    }

    public int getPointNumber() {
        return points.length;
    }

    public Point getPoint(int i) {
        return points[i];
    }

    public String getTid() {
        return tid;
    }

    public Point[] getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "LINESTRING (" +
                Arrays.stream(points)
                        .map(pt -> pt.getLng() + " " + pt.getLat())
                        .collect(Collectors.joining(", ")) + ")";
    }
}
