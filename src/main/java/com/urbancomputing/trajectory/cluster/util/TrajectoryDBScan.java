package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Segment;

import java.util.*;

import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.computeSegmentToSegmentDistance;

/**
 * 线段聚类
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class TrajectoryDBScan {
    static List<Segment> segments;
    static Double eps;
    static int minLns;
    static ArrayList<Integer> clusterIds;
    static int segmentNumber;
    static private final int UNCLASSIFIED_ID = -2;
    static private final int NOISE_ID = -1;
    static int currentId = 0;

    public static ArrayList<Integer> dbscan(List<Segment> segments, Double eps, int minLns) {
        TrajectoryDBScan.segments = segments;
        TrajectoryDBScan.eps = eps;
        TrajectoryDBScan.minLns = minLns;
        TrajectoryDBScan.segmentNumber = segments.size();
        TrajectoryDBScan.clusterIds = new ArrayList<>(Collections.nCopies(segmentNumber, UNCLASSIFIED_ID));

        for (int i = 0; i < segmentNumber; i++) {
            if (clusterIds.get(i) == UNCLASSIFIED_ID && expandDense(i)) {
                currentId++;
            }
        }
        return clusterIds;
    }

    private static boolean expandDense(int segmentIndex) {
        Set<Integer> neighborhoods1 = new HashSet<>();
        Set<Integer> neighborhoods2 = new HashSet<>();

        computeEpsNeighborhood(segmentIndex, neighborhoods1);
        if (neighborhoods1.size() < minLns) {
            clusterIds.set(segmentIndex, NOISE_ID);
            return false;
        }
        for (int seed : neighborhoods1) {
            clusterIds.set(seed, currentId);
        }
        neighborhoods1.remove(segmentIndex);
        int currIndex;
        while (!neighborhoods1.isEmpty()) {
            currIndex = (int) neighborhoods1.toArray()[0];
            computeEpsNeighborhood(currIndex, neighborhoods2);
            if (neighborhoods2.size() >= minLns) {
                for (int seed : neighborhoods2) {
                    int tempId = clusterIds.get(seed);
                    if (tempId == UNCLASSIFIED_ID || tempId == NOISE_ID) {
                        if (tempId == UNCLASSIFIED_ID) {
                            neighborhoods1.add(seed);
                        }
                        clusterIds.set(seed, currentId);
                    }
                }
            }
            neighborhoods1.remove(currIndex);
        }
        return true;
    }

    private static void computeEpsNeighborhood(int i, Set<Integer> seeds) {
        seeds.clear();
        for (int j = 0; j < segmentNumber; j++) {
            double distance = computeSegmentToSegmentDistance(segments.get(i), segments.get(j));
            if (distance <= eps) seeds.add(j);
        }
    }
}
