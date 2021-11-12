package com.urbancomputing.trajectory.cluster;

import com.urbancomputing.trajectory.model.Point;
import com.urbancomputing.trajectory.model.Segment;
import com.urbancomputing.trajectory.model.Trajectory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Main
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class Main {
    // trajectory partition parameters
    static double PARTITION_MIN_SEGMENT_LENGTH = 50.0;

    // segments dbscan cluster parameters
    static double DBSCAN_EPS = 25.0;
    static int DBSCAN_MIN_NUM = 5;

    // compute representative trajectory parameters
    static double REP_MIN_SMOOTHING_LENGTH = 30.0;
    static int REP_MIN_TRAJ_NUM_FOR_CLUSTER = 10;
    static int REP_MIN_SEGMENT_NUM_FOR_SWEEP = 10;

    public static void main(String[] args) throws Exception {
        // initialize data
        String filePath = "src/main/resources/elk_1993.txt";
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        ArrayList<Trajectory> trajs = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(" ");
            String oid = items[0];
            int pointNumber = Integer.parseInt(items[1]);
            ArrayList<Point> points = new ArrayList<>(pointNumber);
            for (int i = 2; i < items.length; i += 2) {
                Point p = new Point(Double.parseDouble(items[i]), Double.parseDouble(items[i + 1]));
                points.add(p);
            }
            Trajectory traj = new Trajectory(oid, points);
            trajs.add(traj);
        }

        // first: trajectory partition
        TrajectoryPartition trajectoryPartition = new TrajectoryPartition(trajs, PARTITION_MIN_SEGMENT_LENGTH);
        ArrayList<Segment> segments = trajectoryPartition.partition();
        // second: trajectory cluster including noise
        TrajectoryDBScan trajectoryDBScan = new TrajectoryDBScan(segments, DBSCAN_EPS, DBSCAN_MIN_NUM);
        ArrayList<Integer> clusterIds = trajectoryDBScan.cluster();
        int clusterNum = trajectoryDBScan.getClusterNum();
        // third: compute representative trajectory
        TrajectoryRepresentative trajectoryRepresentative = new TrajectoryRepresentative(segments, clusterIds, clusterNum,
                REP_MIN_SMOOTHING_LENGTH, REP_MIN_TRAJ_NUM_FOR_CLUSTER, REP_MIN_SEGMENT_NUM_FOR_SWEEP);
        ArrayList<Trajectory> representativeTrajs = trajectoryRepresentative.compute();

        // trajectory visualization
        TrajectoryVisualizeFrame frame = new TrajectoryVisualizeFrame(trajs, null, representativeTrajs);
        frame.draw();
    }
}
