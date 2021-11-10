package com.urbancomputing.trajectory.cluster.model;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Trajectory Class
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class Trajectory {
    /**
     * trajectory id
     */
    private final String tid;
    /**
     * trajectory points
     */
    private final ArrayList<Point> points;

    public Trajectory(String tid, ArrayList<Point> points) {
        this.tid = tid;
        this.points = points;
    }

    public int getPointNumber() {
        return points.size();
    }

    public Point getPoint(int i) {
        return points.get(i);
    }

    public String getTid() {
        return tid;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "LINESTRING (" +
                points.stream()
                        .map(pt -> pt.getLng() + " " + pt.getLat())
                        .collect(Collectors.joining(", ")) + ")";
    }
}
