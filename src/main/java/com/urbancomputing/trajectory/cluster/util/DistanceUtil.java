package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;

/**
 * 距离计算工具类
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class DistanceUtil {
    // 距离计算相关变量
    static int pointDim = 2;
    static double[] vector1 = new double[2];
    static double[] vector2 = new double[2];
    static double coefficient;
    static Point projectionPoint = new Point();

    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * 计算向量长度
     */
    public static double computeVectorLength(double[] vector) {
        double squareSum = 0.0;
        for (double value : vector) {
            squareSum += Math.pow(value, 2);
        }
        return Math.sqrt(squareSum);
    }

    /**
     * 计算两向量内积
     */
    public static double computeInnerProduct(double[] v1, double[] v2) {
        int vectorDim = v1.length;
        double innerProduct = 0.0;
        for (int i = 0; i < vectorDim; i++) {
            innerProduct += (v1[i] * v2[i]);
        }
        return innerProduct;
    }

    /**
     * 计算两点之间的欧式距离
     */
    public static double computePointToPointDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.getLng() - p2.getLng(), 2) + Math.pow(p1.getLat() - p2.getLat(), 2));
    }

    /**
     * 计算两线段之间的垂直距离
     */
    public static double computePerpendicularDistance(Segment s1, Segment s2) {
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

    /**
     * 计算两线段之间的角度距离
     */
    public static double computeAngleDistance(Segment s1, Segment s2) {
        for (int i = 0; i < pointDim; i++) {
            vector1[i] = s1.getEndPoint().getCoord(i) - s1.getStartPoint().getCoord(i);
            vector2[i] = s2.getEndPoint().getCoord(i) - s2.getStartPoint().getCoord(i);
        }
        double vectorLength1 = computeVectorLength(vector1);
        double vectorLength2 = computeVectorLength(vector2);
        if (vectorLength1 == 0.0 || vectorLength2 == 0.0) return 0.0;

        double innerProduct = computeInnerProduct(vector1, vector2);
        double cosTheta = innerProduct / (vectorLength1 * vectorLength2);
        if (cosTheta > 1.0) cosTheta = 1.0;
        if (cosTheta < -1.0) cosTheta = -1.0;
        double sinTheta = Math.sqrt(1 - Math.pow(cosTheta, 2));
        return (vectorLength2 * sinTheta);
    }

    /**
     * 计算点到线段的距离
     */
    private static double computePointToSegmentDistance(Point p, Segment s) {
        Point p1 = s.getStartPoint();
        Point p2 = s.getEndPoint();
        for (int i = 0; i < pointDim; i++) {
            vector1[i] = p.getCoord(i) - p1.getCoord(i);
            vector2[i] = p2.getCoord(i) - p1.getCoord(i);
        }
        coefficient = computeInnerProduct(vector1, vector2) / computeInnerProduct(vector2, vector2);
        for (int i = 0; i < pointDim; i++) {
            projectionPoint.setCoord(i, p1.getCoord(i) + coefficient * vector2[i]);
        }
        return computePointToPointDistance(p, projectionPoint);
    }

    /**
     * 计算两线段之间的距离
     */
    public static double computeSegmentToSegmentDistance(Segment s1, Segment s2) {
        double perDistance;
        double parDistance;
        double angleDistance;

        // 令s1为较长的线段
        double segmentLength1 = s1.length();
        double segmentLength2 = s2.length();
        if (segmentLength1 < segmentLength2) {
            Segment temp = s1;
            s1 = s2;
            s2 = temp;
        }

        double perDistance1, perDistance2;
        double parDistance1, parDistance2;
        perDistance1 = computePointToSegmentDistance(s2.getStartPoint(), s1);
        if (coefficient < 0.5) {
            parDistance1 = computePointToPointDistance(s1.getStartPoint(), projectionPoint);
        } else {
            parDistance1 = computePointToPointDistance(s1.getEndPoint(), projectionPoint);
        }
        perDistance2 = computePointToSegmentDistance(s2.getEndPoint(), s1);
        if (coefficient < 0.5) {
            parDistance2 = computePointToPointDistance(s1.getStartPoint(), projectionPoint);
        } else {
            parDistance2 = computePointToPointDistance(s1.getEndPoint(), projectionPoint);
        }

        // 计算垂直距离Perpendicular Distance: (d1^2 + d2^2) / (d1 + d2)
        if (!(perDistance1 == 0.0 && perDistance2 == 0.0)) {
            perDistance = ((Math.pow(perDistance1, 2) + Math.pow(perDistance2, 2)) / (perDistance1 + perDistance2));
        } else {
            perDistance = 0.0;
        }

        // 计算平行距离Parallel Distance: min(d1, d2)
        parDistance = Math.min(parDistance1, parDistance2);

        // 计算角度距离Angle Distance
        angleDistance = computeAngleDistance(s1, s2);
        return (perDistance + parDistance + angleDistance);
    }
}
