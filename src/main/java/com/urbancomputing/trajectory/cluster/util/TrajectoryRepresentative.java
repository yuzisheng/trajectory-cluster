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

        for (int i = 0; i < segments.size(); i++) {
            int clusterId = clusterIds.get(i);
            if (clusterId >= 0) {
                for (int j = 0; j < POINT_DIM; j++) {
                    double difference = segments.get(i).getEndPoint().getCoord(j) - segments.get(i).getStartPoint().getCoord(j);
                    double currSum = segmentClusters[clusterId].avgDirectionVector[j] + difference;
                    segmentClusters[clusterId].avgDirectionVector[j] = currSum;
                }
                segmentClusters[clusterId].segmentNumber++;
            }
        }

        // 计算平均方向向量
        double[] vector2 = new double[]{1.0, 0.0};
        double vectorLength1, vectorLength2 = 1.0, innerProduct;
        double cosTheta, sinTheta;
        for (int i = 0; i < clusterNumber; i++) {
            SegmentCluster clusterEntry = segmentClusters[i];
            for (int j = 0; j < POINT_DIM; j++) {
                clusterEntry.avgDirectionVector[j] /= clusterEntry.segmentNumber;
            }
            vectorLength1 = computeVectorLength(clusterEntry.avgDirectionVector);
            innerProduct = computeInnerProduct(clusterEntry.avgDirectionVector, vector2);
            cosTheta = innerProduct / (vectorLength1 * vectorLength2);
            if (cosTheta > 1.0) cosTheta = 1.0;
            if (cosTheta < -1.0) cosTheta = -1.0;
            sinTheta = Math.sqrt(1 - Math.pow(cosTheta, 2));
            if (clusterEntry.avgDirectionVector[1] < 0) {
                sinTheta = -sinTheta;
            }
            clusterEntry.cosTheta = cosTheta;
            clusterEntry.sinTheta = sinTheta;
        }

        // 统计线段簇信息
        for (int i = 0; i < segments.size(); i++) {
            if (clusterIds.get(i) >= 0) {
                RegisterAndUpdateSegmentCluster(i);
            }
        }

        for (int i = 0; i < clusterNumber; i++) {
            SegmentCluster clusterEntry = segmentClusters[i];
            // a cluster must contain a certain number of different trajectories
            if (clusterEntry.trajNumber >= minTrajNum) {
                clusterEntry.enabled = true;
                computeRepresentativeTrajectory(clusterEntry);
            } else {
                clusterEntry.candidatePointList.clear();
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

    private static void computeRepresentativeTrajectory(SegmentCluster clusterEntry) {
        Set<Integer> segmentIds = new HashSet<>();
        Set<Integer> insertionList = new HashSet<>();
        Set<Integer> deletionList = new HashSet<>();

        int iter = 0;
        CandidateClusterPoint candidatePoint, nextCandidatePoint;
        double prevOrderingValue = 0.0;
        int clusterPointNumber = 0;

        // 扫描线
        while (iter != (clusterEntry.candidatePointList.size() - 1) && clusterEntry.candidatePointList.size() > 0) {
            insertionList.clear();
            deletionList.clear();
            do {
                candidatePoint = clusterEntry.candidatePointList.get(iter);
                iter++;
                //  check whether this line segment has begun or not
                if (!segmentIds.contains(candidatePoint.segmentId)) {
                    insertionList.add(candidatePoint.segmentId);        //  this line segment begins at this point
                    segmentIds.add(candidatePoint.segmentId);
                } else {                        //  if there is a matched element,
                    deletionList.add(candidatePoint.segmentId);        //  this line segment ends at this point
                }
                //  check whether the next line segment begins or ends at the same point
                if (iter != (clusterEntry.candidatePointList.size() - 1)) {
                    nextCandidatePoint = clusterEntry.candidatePointList.get(iter);
                } else {
                    break;
                }
            } while (candidatePoint.orderingValue == nextCandidatePoint.orderingValue);

            // 检查线段是否与另一线段在同一轨迹中连接，若是则删除一个以避免重复
            for (int iter2 = 0; iter2 < insertionList.size(); iter2++) {
                for (int iter3 = 0; iter3 < deletionList.size(); iter3++) {
                    int a = (Integer) (insertionList.toArray()[iter2]);
                    int b = (Integer) (deletionList.toArray()[iter3]);
                    if (a == b) {
                        segmentIds.remove((Integer) (deletionList.toArray()[iter3]));
                        deletionList.remove((Integer) (deletionList.toArray()[iter3]));
                        break;
                    }
                }

                for (int iter3 = 0; iter3 < deletionList.size(); iter3++) {
                    if (Objects.equals(segments.get((Integer) (insertionList.toArray()[iter2])).getTid(),
                            segments.get((Integer) (deletionList.toArray()[iter3])).getTid())) {
                        segmentIds.remove((Integer) (deletionList.toArray()[iter3]));
                        deletionList.remove((Integer) (deletionList.toArray()[iter3]));
                        break;
                    }
                }
            }

            // 若扫描到轨迹数量达到阈值
            if (segmentIds.size() >= minTrajNum) {
                if (Math.abs(candidatePoint.orderingValue - prevOrderingValue) > (minSegmentLength / 1.414)) {
                    computeAndRegisterClusterPoint(clusterEntry, candidatePoint.orderingValue, segmentIds);
                    prevOrderingValue = candidatePoint.orderingValue;
                    clusterPointNumber++;
                }
            }

            // 删除不与其它线段相连的线段
            for (int iter3 = 0; iter3 < deletionList.size(); iter3++) {
                segmentIds.remove((Integer) (deletionList.toArray()[iter3]));
            }
        }

        // 至少包含两点才可形成特征轨迹
        if (clusterPointNumber >= 2) {
            clusterEntry.repPointNumber = clusterPointNumber;
        } else {
            clusterEntry.enabled = false;
            clusterEntry.candidatePointList.clear();
            clusterEntry.repPoints.clear();
            clusterEntry.trajIds.clear();
        }
    }

    private static void computeAndRegisterClusterPoint(SegmentCluster clusterEntry, double currValue, Set<Integer> segmentIds) {
        int segmentSetSize = segmentIds.size();
        Point clusterPoint = new Point();
        Point sweepPoint = new Point();

        for (int iter = 0; iter < segmentIds.size(); iter++) {
            // get the sweep point of each line segment
            // this point is parallel to the current value of the sweeping direction
            getSweepPointOfSegment(clusterEntry, currValue, (Integer) (segmentIds.toArray()[iter]), sweepPoint);
            for (int i = 0; i < POINT_DIM; i++) {
                clusterPoint.setCoord(i, clusterPoint.getCoord(i) + sweepPoint.getCoord(i) / (double) segmentSetSize);
            }
        }

        // NOTE: this program code works only for the 2-dimensional data
        double origX, origY;
        origX = GET_X_REV_ROTATION(clusterPoint.getCoord(0), clusterPoint.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        origY = GET_Y_REV_ROTATION(clusterPoint.getCoord(0), clusterPoint.getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        clusterPoint.setCoord(0, origX);
        clusterPoint.setCoord(1, origY);

        // register the obtained cluster point (i.e., the average of all the sweep points)
        clusterEntry.repPoints.add(clusterPoint);
    }

    private static void getSweepPointOfSegment(SegmentCluster clusterEntry, double currValue, int lineSegmentId, Point sweepPoint) {
        Segment segment = segments.get(lineSegmentId);
        double coefficient;
        double newStartX, newEndX, newStartY, newEndY;
        newStartX = GET_X_ROTATION(segment.getStartPoint().getCoord(0), segment.getStartPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        newEndX = GET_X_ROTATION(segment.getEndPoint().getCoord(0), segment.getEndPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        newStartY = GET_Y_ROTATION(segment.getStartPoint().getCoord(0), segment.getStartPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        newEndY = GET_Y_ROTATION(segment.getEndPoint().getCoord(0), segment.getEndPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);

        coefficient = (currValue - newStartX) / (newEndX - newStartX);
        sweepPoint.setCoord(0, currValue);
        sweepPoint.setCoord(1, newStartY + coefficient * (newEndY - newStartY));
    }

    /**
     * 统计线段簇相关信息
     */
    private static void RegisterAndUpdateSegmentCluster(int segmentIndex) {
        int clusterId = clusterIds.get(segmentIndex);
        SegmentCluster clusterEntry = segmentClusters[clusterId];
        Segment segment = segments.get(segmentIndex);
        double orderingValue1 = GET_X_ROTATION(segment.getStartPoint().getCoord(0),
                segment.getStartPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);
        double orderingValue2 = GET_X_ROTATION(segment.getEndPoint().getCoord(0),
                segment.getEndPoint().getCoord(1), clusterEntry.cosTheta, clusterEntry.sinTheta);

        // 将线段按起点横坐标排序
        CandidateClusterPoint existingCandidatePoint, newCandidatePoint1, newCandidatePoint2;
        int i, j;
        int iter1 = 0;
        for (i = 0; i < clusterEntry.candidatePointList.size(); i++) {
            existingCandidatePoint = clusterEntry.candidatePointList.get(iter1);
            if (existingCandidatePoint.orderingValue >= orderingValue1) {
                break;
            }
            iter1++;
        }
        newCandidatePoint1 = new CandidateClusterPoint();
        newCandidatePoint1.orderingValue = orderingValue1;
        newCandidatePoint1.segmentId = segmentIndex;
        newCandidatePoint1.startPointFlag = true;
        if (i == 0) {
            clusterEntry.candidatePointList.add(0, newCandidatePoint1);
        } else if (i >= clusterEntry.candidatePointList.size()) {
            clusterEntry.candidatePointList.add(newCandidatePoint1);
        } else {
            clusterEntry.candidatePointList.add(iter1, newCandidatePoint1);
        }
        int iter2 = 0;
        for (j = 0; j < clusterEntry.candidatePointList.size(); j++) {
            existingCandidatePoint = clusterEntry.candidatePointList.get(iter2);
            if (existingCandidatePoint.orderingValue >= orderingValue2) {
                break;
            }
            iter2++;
        }

        newCandidatePoint2 = new CandidateClusterPoint();
        newCandidatePoint2.orderingValue = orderingValue2;
        newCandidatePoint2.segmentId = segmentIndex;
        newCandidatePoint2.startPointFlag = false;

        if (j == 0) {
            clusterEntry.candidatePointList.add(0, newCandidatePoint2);
        } else if (j >= clusterEntry.candidatePointList.size()) {
            clusterEntry.candidatePointList.add(newCandidatePoint2);
        } else {
            clusterEntry.candidatePointList.add(iter2, newCandidatePoint2);
        }

        String tid = segments.get(segmentIndex).getTid();
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
     * Segment Cluster Class
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
         * candidate cluster point
         */
        ArrayList<CandidateClusterPoint> candidatePointList = new ArrayList<>();
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

    static class CandidateClusterPoint {
        double orderingValue;
        /**
         * the id of segment
         */
        int segmentId;
        /**
         * is a start point
         */
        boolean startPointFlag;
    }
}
