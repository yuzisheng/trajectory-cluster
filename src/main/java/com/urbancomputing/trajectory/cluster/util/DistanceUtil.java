package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;

/**
 * 距离计算工具类
 *
 * @author yuzisheng3
 * @date 2021/11/5
 */
public class DistanceUtil {
    /**
     * 计算两点内积
     */
    private static double computeInnerProduct(Point p1, Point p2) {
        return p1.getLng() * p2.getLng() + p1.getLat() * p2.getLat();
    }

    /**
     * 计算两点之间的欧式距离
     */
    public static double computeEuclideanDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.getLng() - p2.getLng(), 2) + Math.pow(p1.getLat() - p2.getLat(), 2));
    }

    /**
     * 计算点到直线的距离
     */
    public static double computePointToSegmentDistance(Point p, Segment s) {
        Point p1 = s.getStartPoint();
        Point p2 = s.getEndPoint();
        double a = computeEuclideanDistance(p, p1);
        double b = computeEuclideanDistance(p, p2);
        double c = computeEuclideanDistance(p1, p2);
        double t = (a + b + c) / 2.0;
        double area = Math.sqrt(Math.abs(t * (t - a) * (t - b) * (t - c)));
        return 2.0 * area / c;
    }

    /**
     * 计算两线段之间的垂直距离
     */
    public static double computePerpendicularDistance(Segment s1, Segment s2) {
        // 假设线段一更长
        if (s1.length() < s2.length()) {
            Segment temp = s1;
            s1 = s2;
            s2 = temp;
        }
        double distance1 = computePointToSegmentDistance(s2.getStartPoint(), s1);
        double distance2 = computePointToSegmentDistance(s2.getEndPoint(), s1);
        if (distance1 == 0.0 && distance2 == 0.0) {
            return 0.0;
        }
        return (Math.pow(distance1, 2) + Math.pow(distance2, 2)) / (distance1 + distance2);
    }
}
