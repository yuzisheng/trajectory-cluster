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

    /**
     * 待分段轨迹
     */
    Trajectory traj;

    public TrajectoryPartition(Trajectory traj) {
        this.traj = traj;
    }

    public ArrayList<Segment> partition() throws Exception {
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
        } while (startIndex + length <= pointNumber);

        // 将终点加入特征点集
        characteristicPoints.add(traj.getPoint(pointNumber - 1));

        ArrayList<Segment> segments = new ArrayList<>();  // 分段后的线段集
        for (int i = 1; i < characteristicPoints.size(); i++) {
            segments.add(new Segment(characteristicPoints.get(i - 1), characteristicPoints.get(i)));
        }
        return segments;
    }

    private int computeModelCost(int startIndex, int endIndex) {
        Point startPoint = traj.getPoint(startIndex);
        Point endPoint = traj.getPoint(endIndex);
        double distance = computeEuclideanDistance(startPoint, endPoint);
        // 防止取对数后为负数
        if (distance < 1.0) {
            distance = 1.0;
        }
        return (int) Math.ceil(log2(distance));
    }

    private int computeEncodingCost(int startIndex, int endIndex) {
        Point characteristicStartPoint, characteristicEndPoint;
        Point trajStartPoint, trajEndPoint;
        double perpendicularDistance, angleDistance;
        int encodingCost = 0;

        characteristicStartPoint = traj.getPoint(startIndex);
        characteristicEndPoint = traj.getPoint(endIndex);
        for (int i = startIndex; i < endIndex; i++) {
            trajStartPoint = traj.getPoint(i);
            trajEndPoint = traj.getPoint(i + 1);

            Segment s1 = new Segment(characteristicStartPoint, characteristicEndPoint);
            Segment s2 = new Segment(trajStartPoint, trajEndPoint);
            perpendicularDistance = computePerpendicularDistance(s1, s2);
            angleDistance = computeAngleDistance(s1, s2);

            if (perpendicularDistance < 1.0) perpendicularDistance = 1.0;
            if (angleDistance < 1.0) angleDistance = 1.0;
            encodingCost += ((int) Math.ceil(log2(perpendicularDistance)) + (int) Math.ceil(log2(angleDistance)));
        }
        return encodingCost;
    }
}
