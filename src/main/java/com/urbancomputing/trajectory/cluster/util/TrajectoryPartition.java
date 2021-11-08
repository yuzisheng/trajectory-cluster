package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;
import com.urbancomputing.trajectory.cluster.model.Trajectory;

import java.util.ArrayList;

import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.*;

/**
 * 轨迹分段
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class TrajectoryPartition {
    private static final int MDL_COST_ADWANTAGE = 25;
    private static final double MIN_SEGMENT_LENGTH = 50.0;

    /**
     * 待分段轨迹
     */
    private static Trajectory traj;

    public static ArrayList<Segment> partition(Trajectory t) throws Exception {
        traj = t;
        int pointNumber = traj.getPointNumber();
        if (pointNumber < 2) {
            throw new Exception("待分段的轨迹至少应包含两个点");
        }

        ArrayList<Point> characteristicPoints = new ArrayList<>();  // 轨迹特征点集
        // 将起点加入特征点集
        characteristicPoints.add(traj.getPoint(0));

        int startIndex = 0, length;
        int fullPartitionMDLCost, partialPartitionMDLCost;
        do {
            fullPartitionMDLCost = 0;
            for (length = 1; startIndex + length < pointNumber; length++) {
                fullPartitionMDLCost += computeModelCost(startIndex + length - 1, startIndex + length);
                partialPartitionMDLCost = computeModelCost(startIndex, startIndex + length) + computeEncodingCost(startIndex, startIndex + length);

                if (fullPartitionMDLCost + MDL_COST_ADWANTAGE < partialPartitionMDLCost) {
                    characteristicPoints.add(traj.getPoint(startIndex + length - 1));
                    startIndex = startIndex + length - 1;
                    length = 0;
                    break;
                }
            }
        } while (startIndex + length < pointNumber);

        // 将终点加入特征点集
        characteristicPoints.add(traj.getPoint(pointNumber - 1));

        ArrayList<Segment> segments = new ArrayList<>();  // 分段后的线段集
        for (int i = 0; i < characteristicPoints.size() - 1; i++) {
            Segment s = new Segment(characteristicPoints.get(i), characteristicPoints.get(i + 1), traj.getTid());
            if (s.length() >= MIN_SEGMENT_LENGTH) {
                segments.add(s);
            }
        }
        return segments;
    }

    private static int computeModelCost(int startIndex, int endIndex) {
        Point startPoint = traj.getPoint(startIndex);
        Point endPoint = traj.getPoint(endIndex);
        double distance = computePointToPointDistance(startPoint, endPoint);
        if (distance < 1.0) distance = 1.0;
        return (int) Math.ceil(log2(distance));
    }

    private static int computeEncodingCost(int startIndex, int endIndex) {
        Point characteristicStartPoint, characteristicEndPoint;
        Point trajStartPoint, trajEndPoint;
        double perDistance, angleDistance;
        int encodingCost = 0;

        characteristicStartPoint = traj.getPoint(startIndex);
        characteristicEndPoint = traj.getPoint(endIndex);
        for (int i = startIndex; i < endIndex; i++) {
            trajStartPoint = traj.getPoint(i);
            trajEndPoint = traj.getPoint(i + 1);

            Segment s1 = new Segment(characteristicStartPoint, characteristicEndPoint);
            Segment s2 = new Segment(trajStartPoint, trajEndPoint);
            perDistance = computePerpendicularDistance(s1, s2);
            angleDistance = computeAngleDistance(s1, s2);

            if (perDistance < 1.0) perDistance = 1.0;
            if (angleDistance < 1.0) angleDistance = 1.0;
            encodingCost += ((int) Math.ceil(log2(perDistance)) + (int) Math.ceil(log2(angleDistance)));
        }
        return encodingCost;
    }
}
