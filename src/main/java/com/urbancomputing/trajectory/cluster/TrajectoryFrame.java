package com.urbancomputing.trajectory.cluster;

import com.sun.deploy.panel.JavaPanel;
import com.urbancomputing.trajectory.cluster.model.Point;
import com.urbancomputing.trajectory.cluster.model.Segment;
import com.urbancomputing.trajectory.cluster.model.Trajectory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * 轨迹可视化
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class TrajectoryFrame extends JFrame {
    /**
     * 原始轨迹
     */
    List<Trajectory> rawTrajs;

    /**
     * 分段后的轨迹线段
     */
    List<Segment> partitionedSegments;

    public TrajectoryFrame(List<Trajectory> rawTrajs, List<Segment> partitionedSegments) {
        this.rawTrajs = rawTrajs;
        this.partitionedSegments = partitionedSegments;
    }

    public void drawRawAndPartitionedTrajs() {
        // 初始化窗口
        JPanel p = new JavaPanel();
        this.setBounds(200, 200, 1200, 900);
        this.setContentPane(p);
        this.setTitle("Raw and Partitioned Trajectories");
        this.setVisible(true);
        this.setResizable(true);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // 画原始轨迹
        Graphics g = p.getGraphics();
        p.paint(g);
        for (Trajectory traj : rawTrajs) {
            for (int i = 0; i < traj.getPointNumber() - 1; i++) {
                Point startPoint = traj.getPoint(i);
                Point endPoint = traj.getPoint(i + 1);
                g.setColor(Color.GREEN);
                g.drawLine((int) startPoint.getLng(), (int) startPoint.getLat(), (int) endPoint.getLng(), (int) endPoint.getLat());
            }
        }
        // 画分段后轨迹
        for (int i = 0; i < partitionedSegments.size() - 1; i++) {
            Point startPoint = partitionedSegments.get(i).getStartPoint();
            Point endPoint = partitionedSegments.get(i + 1).getEndPoint();
            g.setColor(Color.BLUE);
            g.drawLine((int) startPoint.getLng(), (int) startPoint.getLat(), (int) endPoint.getLng(), (int) endPoint.getLat());
        }
    }
}
