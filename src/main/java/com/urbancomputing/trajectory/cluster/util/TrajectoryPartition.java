package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;
import com.urbancomputing.trajectory.cluster.model.Trajectory;

import java.util.ArrayList;

import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.computeEuclideanDistance;

/**
 * 轨迹分段
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class TrajectoryPartition {
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

        int startIndex = 0;
        int length = 1;
        do {
            int curIndex = startIndex + length;
            double parCost = 0;
            double noParCost = 0;
            if (parCost > noParCost) {
                characteristicPoints.add(traj.getPoint(curIndex - 1));
                startIndex = curIndex - 1;
                length = 1;
            } else {
                length += 1;
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

    private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private double computeParMDL(Point p1, Point p2) {
        return 0.0;
    }

    private double computeNoParMDL(Point p1, Point p2) {
        return 0.0;
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
}
