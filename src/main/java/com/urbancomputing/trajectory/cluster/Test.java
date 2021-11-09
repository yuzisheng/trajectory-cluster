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
 * 测试类
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class Test {
    public static void main(String[] args) throws Exception {
        // 初始化轨迹数据
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

        // 第一步：轨迹分段
        ArrayList<Segment> segments = new ArrayList<>();
        for (Trajectory traj : trajs) {
            segments.addAll(TrajectoryPartition.partition(traj));
        }
        assert segments.size() == 3373;
        // 第二步：轨迹聚类
        ArrayList<Integer> clusterIds = TrajectoryDBScan.dbscan(segments, 25.0, 5);
        // 第三步：特征轨迹
        ArrayList<Trajectory> representativeTrajs = TrajectoryRepresentative.construct(segments, clusterIds, 5);

        // 轨迹可视化
        TrajectoryFrame frame = new TrajectoryFrame(trajs, null, representativeTrajs);
        frame.drawRawAndPartitionedTrajs();
    }
}
