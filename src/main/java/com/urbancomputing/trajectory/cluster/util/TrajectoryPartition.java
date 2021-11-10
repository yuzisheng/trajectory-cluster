package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;
import com.urbancomputing.trajectory.cluster.model.Trajectory;

import java.util.ArrayList;

import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.*;

/**
 * trajectory partition
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class TrajectoryPartition {
    /**
     * raw trajectory
     */
    private static Trajectory traj;
    /**
     * wantage to make up no partition mdl cost
     */
    private static int mldCostWantage;
    /**
     * minimum length threshold to filter short segments
     */
    private static double minSegmentLength;

    public static ArrayList<Segment> partition(Trajectory traj, int mldCostWantage, double minSegmentLength) throws Exception {
        TrajectoryPartition.traj = traj;
        TrajectoryPartition.mldCostWantage = mldCostWantage;
        TrajectoryPartition.minSegmentLength = minSegmentLength;

        int pointNumber = traj.getPointNumber();
        if (pointNumber < 2) {
            throw new Exception("trajectory to be partitioned shall contain at least two points");
        }

        ArrayList<Point> characteristicPoints = new ArrayList<>();
        // first: add the start point
        characteristicPoints.add(traj.getPoint(0));

        // second: check each point
        int startIndex = 0, length = 1, currIndex;
        int parMDLCost, noParMDLCost;
        do {
            currIndex = startIndex + length;
            // MDLCost = L(H) + L(D|H)
            parMDLCost = computeParModelCost(startIndex, currIndex) + computeEncodingCost(startIndex, currIndex);
            // L(D|H)=0 when there is no characteristic point between pi and pj
            noParMDLCost = computeNoParModelCost(startIndex, currIndex);
            if (parMDLCost > noParMDLCost + TrajectoryPartition.mldCostWantage) {
                characteristicPoints.add(traj.getPoint(currIndex - 1));
                startIndex = currIndex - 1;
                length = 1;
            } else {
                length += 1;
            }
        } while (startIndex + length < pointNumber);

        // third: add the end point
        characteristicPoints.add(traj.getPoint(pointNumber - 1));

        ArrayList<Segment> segments = new ArrayList<>();
        for (int i = 0; i < characteristicPoints.size() - 1; i++) {
            Segment s = new Segment(characteristicPoints.get(i), characteristicPoints.get(i + 1), traj.getTid());
            if (s.length() >= TrajectoryPartition.minSegmentLength) {
                segments.add(s);
            }
        }
        return segments;
    }

    /**
     * compute L(H) assuming pi and pj are only two characteristic points
     */
    private static int computeParModelCost(int i, int j) {
        double distance = computePointToPointDistance(traj.getPoint(i), traj.getPoint(j));
        if (distance < 1.0) distance = 1.0;
        return (int) Math.ceil(log2(distance));
    }

    /**
     * compute L(H) assuming no characteristic point between pi and pj
     */
    private static int computeNoParModelCost(int i, int j) {
        int modelCost = 0;
        double distance;
        for (int k = i; k < j; k++) {
            distance = computePointToPointDistance(traj.getPoint(k), traj.getPoint(k + 1));
            if (distance < 1.0) distance = 1.0;
            modelCost += (int) Math.ceil(log2(distance));
        }
        return modelCost;
    }

    /**
     * compute L(D|H) assuming pi and pj are only two characteristic points
     */
    private static int computeEncodingCost(int i, int j) {
        int encodingCost = 0;
        Segment s1 = new Segment(traj.getPoint(i), traj.getPoint(j));
        Segment s2;
        double perDistance, angleDistance;
        for (int k = i; k < j; k++) {
            s2 = new Segment(traj.getPoint(k), traj.getPoint(k + 1));
            perDistance = computePerpendicularDistance(s1, s2);
            angleDistance = computeAngleDistance(s1, s2);

            if (perDistance < 1.0) perDistance = 1.0;
            if (angleDistance < 1.0) angleDistance = 1.0;
            encodingCost += ((int) Math.ceil(log2(perDistance)) + (int) Math.ceil(log2(angleDistance)));
        }
        return encodingCost;
    }
}
