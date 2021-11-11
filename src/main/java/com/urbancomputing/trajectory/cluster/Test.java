package com.urbancomputing.trajectory.cluster;

import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;
import com.urbancomputing.trajectory.cluster.model.Trajectory;
import com.urbancomputing.trajectory.cluster.util.TrajectoryDBScan;
import com.urbancomputing.trajectory.cluster.util.TrajectoryPartition;
import com.urbancomputing.trajectory.cluster.util.TrajectoryRepresentative;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Test
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class Test {
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
        ArrayList<Segment> segments = new ArrayList<>();
        for (Trajectory traj : trajs) {
            segments.addAll(TrajectoryPartition.partition(traj, PARTITION_MIN_SEGMENT_LENGTH));
        }
        System.out.println(segments.size());
        if (segments.size() != 3226) {
            throw new Exception("error encounter in the step of trajectory partition");
        }
        // second: trajectory cluster including noise
        ArrayList<Integer> clusterIds = TrajectoryDBScan.dbscan(segments, DBSCAN_EPS, DBSCAN_MIN_NUM);
        // third: compute representative trajectory
        ArrayList<Trajectory> representativeTrajs = TrajectoryRepresentative.construct(segments, clusterIds, REP_MIN_SMOOTHING_LENGTH, REP_MIN_TRAJ_NUM_FOR_CLUSTER, REP_MIN_SEGMENT_NUM_FOR_SWEEP);

        // trajectory visualization
        TrajectoryFrame frame = new TrajectoryFrame(trajs, null, representativeTrajs);
        frame.draw();
    }
}
