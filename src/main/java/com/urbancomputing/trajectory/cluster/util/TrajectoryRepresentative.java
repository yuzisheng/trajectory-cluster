package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;
import com.urbancomputing.trajectory.cluster.model.Trajectory;

import java.util.*;

import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.computeInnerProduct;
import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.computeVectorLength;

/**
 * compute representative trajectories
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class TrajectoryRepresentative {
    /**
     * dimension of point
     */
    static final int POINT_DIM = 2;
    /**
     * minimum trajectory number for a cluster to construct representative trajectory
     */
    static int minTrajNumForCluster;
    /**
     * minimum segment number for sweep line to construct representative points (minLns in the paper)
     */
    static int minSegmentNumForSweep;
    /**
     * minimum smoothing length to filter close representative points (gamma in the paper)
     */
    static double minSmoothingLength;

    static List<Segment> segments;
    static List<Integer> clusterIds;
    static SegmentCluster[] segmentClusters;

    public static ArrayList<Trajectory> construct(List<Segment> segments,
                                                  List<Integer> clusterIds,
                                                  double minSmoothingLength,
                                                  int minTrajNumForCluster,
                                                  int minSegmentNumForSweep) {
        TrajectoryRepresentative.segments = segments;
        TrajectoryRepresentative.clusterIds = clusterIds;
        TrajectoryRepresentative.minSmoothingLength = minSmoothingLength;
        TrajectoryRepresentative.minTrajNumForCluster = minTrajNumForCluster;
        TrajectoryRepresentative.minSegmentNumForSweep = minSegmentNumForSweep;

        // noise is exclusive
        int clusterNumber = (new HashSet<>(clusterIds)).size() - 1;
        TrajectoryRepresentative.segmentClusters = new SegmentCluster[clusterNumber];

        // initialize
        for (int i = 0; i < clusterNumber; i++) {
            segmentClusters[i] = new SegmentCluster();
            segmentClusters[i].clusterId = i;
            segmentClusters[i].segmentNumber = 0;
            segmentClusters[i].enabled = false;
        }

        // first: compute average direction vector for each cluster
        for (int i = 0; i < segments.size(); i++) {
            int clusterId = clusterIds.get(i);
            if (clusterId >= 0) {
                for (int j = 0; j < POINT_DIM; j++) {
                    double vectorValue = segments.get(i).getCoord(j + POINT_DIM) - segments.get(i).getCoord(j);
                    double currSum = segmentClusters[clusterId].avgDirectionVector[j] + vectorValue;
                    segmentClusters[clusterId].avgDirectionVector[j] = currSum;
                }
                segmentClusters[clusterId].segmentNumber++;
            }
        }
        for (int i = 0; i < clusterNumber; i++) {
            SegmentCluster clusterEntry = segmentClusters[i];
            for (int j = 0; j < POINT_DIM; j++) {
                clusterEntry.avgDirectionVector[j] /= clusterEntry.segmentNumber;
            }
        }

        // second: compute angle between average direction vector and x-axis for each cluster
        double[] unitVectorX = new double[]{1.0, 0.0};
        double unitVectorXLength = 1.0;
        double cosTheta, sinTheta;
        for (int i = 0; i < clusterNumber; i++) {
            SegmentCluster clusterEntry = segmentClusters[i];
            cosTheta = computeInnerProduct(clusterEntry.avgDirectionVector, unitVectorX) /
                    (computeVectorLength(clusterEntry.avgDirectionVector) * unitVectorXLength);
            if (cosTheta > 1.0) cosTheta = 1.0;
            if (cosTheta < -1.0) cosTheta = -1.0;
            sinTheta = Math.sqrt(1 - Math.pow(cosTheta, 2));
            if (clusterEntry.avgDirectionVector[1] < 0) {
                sinTheta = -sinTheta;
            }
            clusterEntry.cosTheta = cosTheta;
            clusterEntry.sinTheta = sinTheta;
        }

        // third: rotate axis and update for each cluster
        for (int i = 0; i < segments.size(); i++) {
            if (clusterIds.get(i) >= 0) {
                SegmentCluster clusterEntry = segmentClusters[clusterIds.get(i)];
                Segment segment = segments.get(i);
                double rotatedX1 = GET_X_ROTATION(segment.getCoord(0), segment.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
                double rotatedX2 = GET_X_ROTATION(segment.getCoord(2), segment.getCoord(3), clusterEntry.cosTheta, clusterEntry.sinTheta);
                clusterEntry.rotatedPoints.add(new RotatedPoint(rotatedX1, i));
                clusterEntry.rotatedPoints.add(new RotatedPoint(rotatedX2, i));

                clusterEntry.clusterRotatedSegments.add(new RotatedSegment(rotatedX1, rotatedX2, i));
                clusterEntry.trajIds.add(segment.getTid());
            }
        }
        // sort by x value of rotated point
        Comparator<RotatedPoint> comparator = Comparator.comparingDouble(o -> o.rotatedX);
        for (SegmentCluster clusterEntry : segmentClusters) {
            clusterEntry.rotatedPoints.sort(comparator);
        }

        // fourth: compute representative trajectory for each cluster
        for (int i = 0; i < clusterNumber; i++) {
            SegmentCluster clusterEntry = segmentClusters[i];
            // a cluster must contain a certain number of different trajectories
            if (clusterEntry.trajIds.size() >= minTrajNumForCluster) {
                clusterEntry.enabled = true;
                computeRepresentativeTrajectory(clusterEntry);
            }
        }

        // fifth: convert cluster to trajectory
        int newCurrClusterId = 0;
        ArrayList<Trajectory> representativeTrajs = new ArrayList<>();
        for (int i = 0; i < clusterNumber; i++) {
            if (segmentClusters[i].enabled) {
                representativeTrajs.add(new Trajectory(Integer.toString(newCurrClusterId), segmentClusters[i].representativePoints));
                newCurrClusterId++;
            }
        }
        return representativeTrajs;
    }

    /**
     * compute representative trajectory
     */
    private static void computeRepresentativeTrajectory(SegmentCluster clusterEntry) {
        int sweepSegmentNum;
        double prevRotatedX = 0.0;
        HashSet<Integer> sweepSegmentIds = new HashSet<>();
        // sweep for each rotated point (sort by x value)
        for (RotatedPoint rp : clusterEntry.rotatedPoints) {
            sweepSegmentNum = 0;
            sweepSegmentIds.clear();
            // count the number of segments that contain x value of rotated point
            for (RotatedSegment rs : clusterEntry.clusterRotatedSegments) {
                if (rs.rotatedStartX <= rp.rotatedX && rp.rotatedX <= rs.rotatedEndX ||
                        rs.rotatedEndX <= rp.rotatedX && rp.rotatedX <= rs.rotatedStartX) {
                    sweepSegmentNum++;
                    sweepSegmentIds.add(rs.segmentId);
                }
            }
            // if sweep enough segments
            if (sweepSegmentNum >= minSegmentNumForSweep) {
                // filter close representative points
                if (Math.abs(rp.rotatedX - prevRotatedX) >= minSmoothingLength) {
                    Point repPoint = getRepresentativePoint(clusterEntry, rp.rotatedX, sweepSegmentIds);
                    clusterEntry.representativePoints.add(repPoint);
                    prevRotatedX = rp.rotatedX;
                }
            }
        }
        // a trajectory must have at least two points
        if (clusterEntry.representativePoints.size() < 2) clusterEntry.enabled = false;
    }

    /**
     * get representative point
     */
    private static Point getRepresentativePoint(SegmentCluster clusterEntry, double currX, Set<Integer> segmentIds) {
        int sweepSegmentNumber = segmentIds.size();
        Point representativePoint = new Point();
        Point sweepPoint;

        // compute the average of all the sweep points: x=(x1+x2+...+xn)/n
        for (int segmentId : segmentIds) {
            sweepPoint = getSweepPointOfSegment(clusterEntry, currX, segmentId);
            for (int i = 0; i < POINT_DIM; i++) {
                representativePoint.setCoord(i, representativePoint.getCoord(i) + sweepPoint.getCoord(i) / sweepSegmentNumber);
            }
        }

        // recover original coordinate
        double origX = GET_X_REV_ROTATION(representativePoint.getCoord(0), representativePoint.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double origY = GET_Y_REV_ROTATION(representativePoint.getCoord(0), representativePoint.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        representativePoint.setCoord(0, origX);
        representativePoint.setCoord(1, origY);

        return representativePoint;
    }

    /**
     * get point on the segment according to its x value
     */
    private static Point getSweepPointOfSegment(SegmentCluster clusterEntry, double currXValue, int segmentId) {
        Segment segment = segments.get(segmentId);
        double rotatedX1 = GET_X_ROTATION(segment.getCoord(0), segment.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedX2 = GET_X_ROTATION(segment.getCoord(POINT_DIM), segment.getCoord(1 + POINT_DIM), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedY1 = GET_Y_ROTATION(segment.getCoord(0), segment.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedY2 = GET_Y_ROTATION(segment.getCoord(POINT_DIM), segment.getCoord(1 + POINT_DIM), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double coefficient = (currXValue - rotatedX1) / (rotatedX2 - rotatedX1);
        return new Point(currXValue, rotatedY1 + coefficient * (rotatedY2 - rotatedY1));
    }

    private static double GET_X_ROTATION(double _x, double _y, double _cos, double _sin) {
        return ((_x) * (_cos) + (_y) * (_sin));
    }

    private static double GET_Y_ROTATION(double _x, double _y, double _cos, double _sin) {
        return (-(_x) * (_sin) + (_y) * (_cos));
    }

    private static double GET_X_REV_ROTATION(double _x, double _y, double _cos, double _sin) {
        return ((_x) * (_cos) - (_y) * (_sin));
    }

    private static double GET_Y_REV_ROTATION(double _x, double _y, double _cos, double _sin) {
        return ((_x) * (_sin) + (_y) * (_cos));
    }

    static class SegmentCluster {
        /**
         * cluster id
         */
        int clusterId;
        /**
         * the number of segments
         */
        int segmentNumber;
        /**
         * ids of containing trajectories
         */
        HashSet<String> trajIds = new HashSet<>();
        /**
         * representative points
         */
        ArrayList<Point> representativePoints = new ArrayList<>();
        /**
         * candidate representative point
         */
        ArrayList<RotatedPoint> rotatedPoints = new ArrayList<>();
        /**
         * rotated segments of cluster
         */
        ArrayList<RotatedSegment> clusterRotatedSegments = new ArrayList<>();
        /**
         * can form a representative trajectory or not
         */
        boolean enabled;
        /**
         * average direction vector
         */
        double[] avgDirectionVector = new double[2];
        /**
         * other temp variable
         */
        double cosTheta, sinTheta;
    }

    static class RotatedSegment {
        /**
         * rotated x value of start point
         */
        double rotatedStartX;
        /**
         * rotated x value of end point
         */
        double rotatedEndX;
        /**
         * segment id
         */
        int segmentId;

        public RotatedSegment(double rotatedStartX, double rotatedEndX, int segmentId) {
            this.rotatedStartX = rotatedStartX;
            this.rotatedEndX = rotatedEndX;
            this.segmentId = segmentId;
        }
    }

    static class RotatedPoint {
        /**
         * rotated x value
         */
        double rotatedX;
        /**
         * segment id
         */
        int segmentId;

        public RotatedPoint(double rotatedX, int segmentId) {
            this.rotatedX = rotatedX;
            this.segmentId = segmentId;
        }
    }
}
