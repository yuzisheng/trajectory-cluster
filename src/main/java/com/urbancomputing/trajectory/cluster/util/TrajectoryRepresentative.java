package com.urbancomputing.trajectory.cluster.util;

import com.urbancomputing.trajectory.cluster.model.Segment;

import java.util.ArrayList;
import java.util.List;

/**
 * 代表轨迹计算
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class TrajectoryRepresentative {
    public static void construct(List<Segment> segments, List<Integer> clusterIds) {
        int clusterNumber = new ArrayList<>(clusterIds).size() - 1; // 噪音点除外
    }
}
