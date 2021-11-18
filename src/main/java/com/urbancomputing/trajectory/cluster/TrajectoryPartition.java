package com.urbancomputing.trajectory.cluster;

import com.urbancomputing.trajectory.model.Point;
import com.urbancomputing.trajectory.model.Segment;
import com.urbancomputing.trajectory.model.Trajectory;

import java.util.ArrayList;

/**
 * trajectory partition
 *
 * @author yuzisheng
 * @date 2021/11/5
 */
public class TrajectoryPartition {
    /**
     * raw trajectories
     */
    private final ArrayList<Trajectory> trajs;
    /**
     * minimum length threshold to filter short segments
     */
    private final double minSegmentLength;

    public TrajectoryPartition(ArrayList<Trajectory> trajs, double minSegmentLength) {
        this.trajs = trajs;
        this.minSegmentLength = minSegmentLength;
    }

    public ArrayList<Segment> partition() throws Exception {
        ArrayList<Segment> partitionedSegments = new ArrayList<>();
        for (Trajectory traj : trajs) {
            partitionedSegments.addAll(partitionOneTraj(traj));
        }
        return partitionedSegments;
    }

    private ArrayList<Segment> partitionOneTraj(Trajectory traj) throws Exception {
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
            parMDLCost = computeParModelCost(traj, startIndex, currIndex) + computeEncodingCost(traj, startIndex, currIndex);
            // L(D|H)=0 when there is no characteristic point between pi and pj
            noParMDLCost = computeNoParModelCost(traj, startIndex, currIndex);
            if (parMDLCost > noParMDLCost) {
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
            if (s.length() >= minSegmentLength) {
                segments.add(s);
            }
        }
        return segments;
    }

    /**
     * compute L(H) assuming pi and pj are only two characteristic points
     */
    private static int computeParModelCost(Trajectory traj, int i, int j) {
        double distance = TrajectoryDistance.computePointToPointDistance(traj.getPoint(i), traj.getPoint(j));
        if (distance < 1.0) distance = 1.0;
        return (int) Math.ceil(TrajectoryDistance.log2(distance));
    }

    /**
     * compute L(H) assuming no characteristic point between pi and pj
     */
    private static int computeNoParModelCost(Trajectory traj, int i, int j) {
        int modelCost = 0;
        double distance;
        for (int k = i; k < j; k++) {
            distance = TrajectoryDistance.computePointToPointDistance(traj.getPoint(k), traj.getPoint(k + 1));
            if (distance < 1.0) distance = 1.0;
            modelCost += (int) Math.ceil(TrajectoryDistance.log2(distance));
        }
        return modelCost;
    }

    /**
     * compute L(D|H) assuming pi and pj are only two characteristic points
     */
    private static int computeEncodingCost(Trajectory traj, int i, int j) {
        int encodingCost = 0;
        Segment s1 = new Segment(traj.getPoint(i), traj.getPoint(j));
        Segment s2;
        double perDistance, angleDistance;
        for (int k = i; k < j; k++) {
            s2 = new Segment(traj.getPoint(k), traj.getPoint(k + 1));
            perDistance = TrajectoryDistance.computePerpendicularDistance(s1, s2);
            angleDistance = TrajectoryDistance.computeAngleDistance(s1, s2);

            if (perDistance < 1.0) perDistance = 1.0;
            if (angleDistance < 1.0) angleDistance = 1.0;
            encodingCost += ((int) Math.ceil(TrajectoryDistance.log2(perDistance)) + (int) Math.ceil(TrajectoryDistance.log2(angleDistance)));
        }
        return encodingCost;
    }
}
