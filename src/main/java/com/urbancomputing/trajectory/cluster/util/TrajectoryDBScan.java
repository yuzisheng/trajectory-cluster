package com.urbancomputing.trajectory.cluster.util;

import java.util.*;
import com.urbancomputing.trajectory.cluster.model.Segment;
import static com.urbancomputing.trajectory.cluster.util.DistanceUtil.computeSegmentToSegmentDistance;

/**
 * segment cluster
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class TrajectoryDBScan {
    /**
     * segments to be clustered
     */
    static List<Segment> segments;
    /**
     * eps
     */
    static double eps;
    /**
     * minimum number
     */
    static int minNum;
    /**
     * cluster ids
     */
    static ArrayList<Integer> clusterIds;
    /**
     * unclassified point id
     */
    static private final int UNCLASSIFIED_ID = -2;
    /**
     * noise point id
     */
    static private final int NOISE_ID = -1;

    public static ArrayList<Integer> dbscan(List<Segment> segments, Double eps, int minNum) {
        TrajectoryDBScan.segments = segments;
        TrajectoryDBScan.eps = eps;
        TrajectoryDBScan.minNum = minNum;
        TrajectoryDBScan.clusterIds = new ArrayList<>(Collections.nCopies(segments.size(), UNCLASSIFIED_ID));

        int currentId = 0;
        for (int i = 0; i < segments.size(); i++) {
            if (clusterIds.get(i) == UNCLASSIFIED_ID && expandDense(i, currentId)) {
                currentId++;
            }
        }
        return clusterIds;
    }

    private static boolean expandDense(int segmentIndex, int currentId) {
        Set<Integer> neighborhoods1 = new HashSet<>();
        Set<Integer> neighborhoods2 = new HashSet<>();

        computeEpsNeighborhood(segmentIndex, neighborhoods1);
        if (neighborhoods1.size() < minNum) {
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
            if (neighborhoods2.size() >= minNum) {
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

    private static void computeEpsNeighborhood(int i, Set<Integer> neighborhoods) {
        neighborhoods.clear();
        for (int j = 0; j < segments.size(); j++) {
            double distance = computeSegmentToSegmentDistance(segments.get(i), segments.get(j));
            if (distance <= eps) neighborhoods.add(j);
        }
    }
}
