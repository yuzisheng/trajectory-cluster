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
     * minimum length threshold to filter short segments
     */
    static double minSegmentLength;
    /**
     * minimum trajectory number for sweep line
     */
    static int minTrajNum;

    static List<Segment> segments;
    static List<Integer> clusterIds;
    static SegmentCluster[] segmentClusters;

    public static ArrayList<Trajectory> construct(List<Segment> segments, List<Integer> clusterIds, double minSegmentLength, int minTrajNum) {
        TrajectoryRepresentative.segments = segments;
        TrajectoryRepresentative.clusterIds = clusterIds;
        TrajectoryRepresentative.minSegmentLength = minSegmentLength;
        TrajectoryRepresentative.minTrajNum = minTrajNum;

        // noise is exclusive
        int clusterNumber = (new HashSet<>(clusterIds)).size() - 1;
        TrajectoryRepresentative.segmentClusters = new SegmentCluster[clusterNumber];

        // initialize
        for (int i = 0; i < clusterNumber; i++) {
            segmentClusters[i] = new SegmentCluster();
            segmentClusters[i].clusterId = i;
            segmentClusters[i].segmentNumber = 0;
            segmentClusters[i].trajNumber = 0;
            segmentClusters[i].repPointNumber = 0;
            segmentClusters[i].enabled = false;
        }

        // first: compute average direction vector for each cluster
        for (int i = 0; i < segments.size(); i++) {
            int clusterId = clusterIds.get(i);
            if (clusterId >= 0) {
                for (int j = 0; j < POINT_DIM; j++) {
                    double vectorValue = segments.get(i).getEndPoint().getCoord(j) - segments.get(i).getStartPoint().getCoord(j);
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

        // third: update candidate point info for each cluster
        for (int i = 0; i < segments.size(); i++) {
            if (clusterIds.get(i) >= 0) {
                updateClusterCandPoints(i);
            }
        }

        // fourth: compute representative trajectory for each cluster
        for (int i = 0; i < clusterNumber; i++) {
            SegmentCluster clusterEntry = segmentClusters[i];
            // a cluster must contain a certain number of different trajectories
            if (clusterEntry.trajNumber >= minTrajNum) {
                clusterEntry.enabled = true;
                computeRepresentativeTrajectory(clusterEntry);
            } else {
                clusterEntry.candidatePoints.clear();
                clusterEntry.repPoints.clear();
                clusterEntry.trajIds.clear();
            }
        }

        // convert cluster to trajectory
        int newCurrClusterId = 0;
        ArrayList<Trajectory> representativeTrajs = new ArrayList<>();
        for (int i = 0; i < clusterNumber; i++) {
            if (segmentClusters[i].enabled) {
                representativeTrajs.add(new Trajectory(Integer.toString(newCurrClusterId), segmentClusters[i].repPoints));
                newCurrClusterId++;
            }
        }
        return representativeTrajs;
    }

    /**
     * compute representative trajectory
     */
    private static void computeRepresentativeTrajectory(SegmentCluster clusterEntry) {
        Set<Integer> segmentIds = new HashSet<>();
        Set<Integer> insertionSegmentIds = new HashSet<>();
        Set<Integer> deletionSegmentIds = new HashSet<>();

        int iter = 0;
        CandidatePoint candidatePoint, nextCandidatePoint;
        double prevOrderingValue = 0.0;
        int clusterPointNumber = 0;

        // 扫描线
        while (iter != (clusterEntry.candidatePoints.size() - 1) && clusterEntry.candidatePoints.size() > 0) {
            insertionSegmentIds.clear();
            deletionSegmentIds.clear();
            do {
                candidatePoint = clusterEntry.candidatePoints.get(iter);
                iter++;
                //  check whether this segment has begun or not
                if (!segmentIds.contains(candidatePoint.segmentId)) {
                    insertionSegmentIds.add(candidatePoint.segmentId);        //  this line segment begins at this point
                    segmentIds.add(candidatePoint.segmentId);
                } else {
                    deletionSegmentIds.add(candidatePoint.segmentId);        //  this line segment ends at this point
                }
                //  check whether the next segment begins or ends at the same point
                if (iter != (clusterEntry.candidatePoints.size() - 1)) {
                    nextCandidatePoint = clusterEntry.candidatePoints.get(iter);
                } else {
                    break;
                }
            } while (candidatePoint.rotatedX == nextCandidatePoint.rotatedX);

            // 检查线段是否与另一线段在同一轨迹中连接，若是则删除一个以避免重复
            for (int iter2 = 0; iter2 < insertionSegmentIds.size(); iter2++) {
                for (int iter3 = 0; iter3 < deletionSegmentIds.size(); iter3++) {
                    int a = (Integer) (insertionSegmentIds.toArray()[iter2]);
                    int b = (Integer) (deletionSegmentIds.toArray()[iter3]);
                    if (a == b) {
                        segmentIds.remove((Integer) (deletionSegmentIds.toArray()[iter3]));
                        deletionSegmentIds.remove((Integer) (deletionSegmentIds.toArray()[iter3]));
                        break;
                    }
                }

                for (int iter3 = 0; iter3 < deletionSegmentIds.size(); iter3++) {
                    if (Objects.equals(segments.get((Integer) (insertionSegmentIds.toArray()[iter2])).getTid(),
                            segments.get((Integer) (deletionSegmentIds.toArray()[iter3])).getTid())) {
                        segmentIds.remove((Integer) (deletionSegmentIds.toArray()[iter3]));
                        deletionSegmentIds.remove((Integer) (deletionSegmentIds.toArray()[iter3]));
                        break;
                    }
                }
            }

            // 若扫描到轨迹数量达到阈值
            if (segmentIds.size() >= minTrajNum) {
                if (Math.abs(candidatePoint.rotatedX - prevOrderingValue) > (minSegmentLength / 1.414)) {
                    computeAndAddRepPoint(clusterEntry, candidatePoint.rotatedX, segmentIds);
                    prevOrderingValue = candidatePoint.rotatedX;
                    clusterPointNumber++;
                }
            }

            // 删除不与其它线段相连的线段
            for (int iter3 = 0; iter3 < deletionSegmentIds.size(); iter3++) {
                segmentIds.remove((Integer) (deletionSegmentIds.toArray()[iter3]));
            }
        }

        // a trajectory must have at least two points
        if (clusterPointNumber >= 2) {
            clusterEntry.repPointNumber = clusterPointNumber;
        } else {
            clusterEntry.enabled = false;
            clusterEntry.candidatePoints.clear();
            clusterEntry.repPoints.clear();
            clusterEntry.trajIds.clear();
        }
    }

    /**
     * compute representative point and add it to cluster result
     */
    private static void computeAndAddRepPoint(SegmentCluster clusterEntry, double currXValue, Set<Integer> segmentIds) {
        int sweepSegmentNumber = segmentIds.size();
        Point representativePoint = new Point();
        Point sweepPoint;

        // compute the average of all the sweep points: x=(x1+x2+...+xn)/n
        for (int segmentId: segmentIds) {
            sweepPoint = getSweepPointOfSegment(clusterEntry, currXValue, segmentId);
            for (int i = 0; i < POINT_DIM; i++) {
                representativePoint.setCoord(i, representativePoint.getCoord(i) + sweepPoint.getCoord(i) / sweepSegmentNumber);
            }
        }

        // recover original coordinate
        double origX = GET_X_REV_ROTATION(representativePoint.getCoord(0), representativePoint.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double origY = GET_Y_REV_ROTATION(representativePoint.getCoord(0), representativePoint.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        representativePoint.setCoord(0, origX);
        representativePoint.setCoord(1, origY);

        clusterEntry.repPoints.add(representativePoint);
    }

    /**
     * get point on the segment according to its x value
     */
    private static Point getSweepPointOfSegment(SegmentCluster clusterEntry, double currXValue, int segmentId) {
        Segment segment = segments.get(segmentId);
        double rotatedX1 = GET_X_ROTATION(segment.getStartPoint().getCoord(0), segment.getStartPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedX2 = GET_X_ROTATION(segment.getEndPoint().getCoord(0), segment.getEndPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedY1 = GET_Y_ROTATION(segment.getStartPoint().getCoord(0), segment.getStartPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedY2 = GET_Y_ROTATION(segment.getEndPoint().getCoord(0), segment.getEndPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double coefficient = (currXValue - rotatedX1) / (rotatedX2 - rotatedX1);
        return new Point(currXValue, rotatedY1 + coefficient * (rotatedY2 - rotatedY1));
    }

    /**
     * insert start and end point of this segment to candidate points and keep it in order by rotated x-axis
     */
    private static void updateClusterCandPoints(int segmentId) {
        int clusterId = clusterIds.get(segmentId);
        SegmentCluster clusterEntry = segmentClusters[clusterId];
        Segment segment = segments.get(segmentId);

        // 1. rotate x-axis
        double rotatedX1 = GET_X_ROTATION(segment.getStartPoint().getCoord(0),
                segment.getStartPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double rotatedX2 = GET_X_ROTATION(segment.getEndPoint().getCoord(0),
                segment.getEndPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);

        // 2. sort points by rotated x value: here use insert sort algorithm to insert new points (start and end points of this segment)
        // 2.1 find the first point whose x value is greater than rotated x1
        int iter1 = 0, i;
        for (i = 0; i < clusterEntry.candidatePoints.size(); i++) {
            if (clusterEntry.candidatePoints.get(iter1).rotatedX >= rotatedX1) {
                break;
            }
            iter1++;
        }
        // 2.2 create new candidate point
        CandidatePoint newCandidatePoint1 = new CandidatePoint();
        newCandidatePoint1.rotatedX = rotatedX1;
        newCandidatePoint1.segmentId = segmentId;
        newCandidatePoint1.startPointFlag = true;
        // 2.3 insert start point of this segment
        if (i == 0) {
            clusterEntry.candidatePoints.add(0, newCandidatePoint1);
        } else if (i >= clusterEntry.candidatePoints.size()) {
            clusterEntry.candidatePoints.add(newCandidatePoint1);
        } else {
            clusterEntry.candidatePoints.add(iter1, newCandidatePoint1);
        }

        // the insertion of end point is the same as above
        int iter2 = 0, j;
        for (j = 0; j < clusterEntry.candidatePoints.size(); j++) {
            if (clusterEntry.candidatePoints.get(iter2).rotatedX >= rotatedX2) {
                break;
            }
            iter2++;
        }
        CandidatePoint newCandidatePoint2 = new CandidatePoint();
        newCandidatePoint2.rotatedX = rotatedX2;
        newCandidatePoint2.segmentId = segmentId;
        newCandidatePoint2.startPointFlag = false;
        if (j == 0) {
            clusterEntry.candidatePoints.add(0, newCandidatePoint2);
        } else if (j >= clusterEntry.candidatePoints.size()) {
            clusterEntry.candidatePoints.add(newCandidatePoint2);
        } else {
            clusterEntry.candidatePoints.add(iter2, newCandidatePoint2);
        }

        // 3. update trajectory info of cluster
        String tid = segments.get(segmentId).getTid();
        if (!clusterEntry.trajIds.contains(tid)) {
            clusterEntry.trajIds.add(tid);
            clusterEntry.trajNumber++;
        }
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

    /**
     * segment cluster info
     */
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
         * the number of trajectories
         */
        int trajNumber;
        /**
         * ids of containing trajectories
         */
        ArrayList<String> trajIds = new ArrayList<>();
        /**
         * ths number of representative points
         */
        int repPointNumber;
        /**
         * representative points
         */
        ArrayList<Point> repPoints = new ArrayList<>();
        /**
         * candidate representative point
         */
        ArrayList<CandidatePoint> candidatePoints = new ArrayList<>();
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

    /**
     * start or end point of segment
     */
    static class CandidatePoint {
        /**
         * rotated x value
         */
        double rotatedX;
        /**
         * segment id
         */
        int segmentId;
        /**
         * is a start point
         */
        boolean startPointFlag;
    }
}
