package com.urbancomputing.trajectory.cluster;

import com.urbancomputing.trajectory.model.Point;
import com.urbancomputing.trajectory.model.Trajectory;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * trajectory cluster test
 *
 * @author yuzisheng
 * @date 2021/11/17
 */
public class TrajectoryClusterTest {
    // trajectory partition parameters
    static double PARTITION_MIN_SEGMENT_LENGTH_IN_M = 50.0;

    // segments dbscan cluster parameters
    static double DBSCAN_EPS_IN_M = 25.0;
    static int DBSCAN_MIN_NUM = 5;

    // compute representative trajectory parameters
    static double REP_MIN_SMOOTHING_LENGTH_IN_M = 30.0;
    static int REP_MIN_TRAJ_NUM_FOR_CLUSTER = 10;
    static int REP_MIN_SEGMENT_NUM_FOR_SWEEP = 10;

    @Test
    public void doCluster() throws Exception {
        // initialize test data
        ArrayList<Trajectory> trajs = getTestData();

        // trajectory cluster
        TrajectoryCluster trajectoryCluster = new TrajectoryCluster(trajs,
                PARTITION_MIN_SEGMENT_LENGTH_IN_M, DBSCAN_EPS_IN_M, DBSCAN_MIN_NUM,
                REP_MIN_SMOOTHING_LENGTH_IN_M, REP_MIN_TRAJ_NUM_FOR_CLUSTER, REP_MIN_SEGMENT_NUM_FOR_SWEEP);
        List<Trajectory> representativeTrajs = trajectoryCluster.doCluster();
        assertEquals(representativeTrajs.size(), 6);

        // trajectory visualization
        TrajectoryVisualizeFrame frame = new TrajectoryVisualizeFrame(trajs, null, representativeTrajs);
        frame.draw();

        // sleep for a while to avoid exit automatic in java unit test
        Thread.sleep(60 * 1000);
    }

    private ArrayList<Trajectory> getTestData() throws IOException {
        String filePath = Objects.requireNonNull(TrajectoryClusterTest.class.getResource("/elk_1993.txt")).getPath();
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
        return trajs;
    }
}

