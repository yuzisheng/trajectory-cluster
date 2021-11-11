package com.urbancomputing.trajectory.cluster.model;

/**
 * Segment Class
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class Segment {
    /**
     * start point
     */
    private final Point startPoint;
    /**
     * end point
     */
    private final Point endPoint;
    /**
     * id of the trajectory containing this segment
     */
    private final String tid;

    public Segment(Point startPoint, Point endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.tid = "default";
    }

    public Segment(Point startPoint, Point endPoint, String tid) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.tid = tid;
    }

    /**
     * length of this segment
     */
    public double length() {
        return Math.sqrt(Math.pow(startPoint.getLng() - endPoint.getLng(), 2) + Math.pow(startPoint.getLat() - endPoint.getLat(), 2));
    }

    public double getCoord(int i) {
        if (i == 0) {
            return startPoint.getLng();
        } else if (i == 1) {
            return startPoint.getLat();
        } else if (i == 2) {
            return endPoint.getLng();
        } else {
            return endPoint.getLat();
        }
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public String getTid() {
        return tid;
    }

    @Override
    public String toString() {
        return "LINESTRING (" + startPoint.getLng() + " " + startPoint.getLat() + ", " + endPoint.getLng() + " " + endPoint.getLat() + ")";
    }
}
