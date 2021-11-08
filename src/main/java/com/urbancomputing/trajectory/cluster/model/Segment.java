package com.urbancomputing.trajectory.cluster.model;

/**
 * 线段类
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class Segment {
    /**
     * 起点
     */
    private final Point startPoint;

    /**
     * 终点
     */
    private final Point endPoint;

    /**
     * 轨迹标识
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
     * 计算线段长度
     */
    public double length() {
        return Math.sqrt(Math.pow(startPoint.getLng() - endPoint.getLng(), 2) + Math.pow(startPoint.getLat() - endPoint.getLat(), 2));
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
