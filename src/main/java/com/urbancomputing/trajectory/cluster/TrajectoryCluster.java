package com.urbancomputing.trajectory.cluster;


import com.urbancomputing.trajectory.model.Segment;
import com.urbancomputing.trajectory.model.Trajectory;

import java.util.ArrayList;

/**
 * trajectory cluster
 *
 * @author yuzisheng
 * @date 2021/11/17
 */
public class TrajectoryCluster {
    ArrayList<Trajectory> trajs;

    // trajectory partition parameters
    double partitionMinSegmentLength;

    // segments dbscan cluster parameters
    double dbscanEps;
    int dbscanMinNum;

    // compute representative trajectory parameters
    double repMinSmoothingLength;
    int repMinTrajNumForCluster;
    int repMinSegmentNumForSweep;

    /**
     * trajectory cluster
     *
     * @param trajs                     trajectories to cluster
     * @param partitionMinSegmentLength min segment length for partition step
     * @param dbscanEps                 eps for dbscan step
     * @param dbscanMinNum              min number for dbscan step
     * @param repMinSmoothingLength     min smoothing length for representative computing step
     * @param repMinTrajNumForCluster   min trajectory number during cluster for representative computing step
     * @param repMinSegmentNumForSweep  min segment number during sweep representative computing step
     */
    public TrajectoryCluster(ArrayList<Trajectory> trajs,
                             double partitionMinSegmentLength,
                             double dbscanEps,
                             int dbscanMinNum,
                             double repMinSmoothingLength,
                             int repMinTrajNumForCluster,
                             int repMinSegmentNumForSweep) {
        this.trajs = trajs;
        this.partitionMinSegmentLength = partitionMinSegmentLength;
        this.dbscanEps = dbscanEps;
        this.dbscanMinNum = dbscanMinNum;
        this.repMinSmoothingLength = repMinSmoothingLength;
        this.repMinTrajNumForCluster = repMinTrajNumForCluster;
        this.repMinSegmentNumForSweep = repMinSegmentNumForSweep;
    }

    /**
     * do trajectory cluster
     *
     * @return list of representative spatial line
     */
    public ArrayList<Trajectory> doCluster() throws Exception {
        // first step: trajectory partition
        TrajectoryPartition trajectoryPartition = new TrajectoryPartition(trajs, partitionMinSegmentLength);
        ArrayList<Segment> segments = trajectoryPartition.partition();

        // second step: trajectory cluster including noise
        TrajectoryDBScan trajectoryDBScan = new TrajectoryDBScan(segments, dbscanEps, dbscanMinNum);
        ArrayList<Integer> clusterIds = trajectoryDBScan.cluster();

        // third step: compute representative trajectory
        int clusterNum = trajectoryDBScan.getClusterNum();
        TrajectoryRepresentative trajectoryRepresentative = new TrajectoryRepresentative(segments, clusterIds, clusterNum,
                repMinSmoothingLength, repMinTrajNumForCluster, repMinSegmentNumForSweep);

        return trajectoryRepresentative.compute();
    }
}

