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
 * trajectory visualization
 *
 * @author yuzisheng
 * @date 2021/11/8
 */
public class TrajectoryFrame extends JFrame {
    /**
     * raw trajectories
     */
    List<Trajectory> rawTrajs;
    /**
     * partitioned segments
     */
    List<Segment> segments;
    /**
     * representative trajectories
     */
    List<Trajectory> representativeTrajs;

    public TrajectoryFrame(List<Trajectory> rawTrajs, List<Segment> segments, List<Trajectory> representativeTrajs) {
        this.rawTrajs = rawTrajs;
        this.segments = segments;
        this.representativeTrajs = representativeTrajs;
    }

    public void draw() {
        // initialize
        JPanel p = new JavaPanel();
        this.setBounds(200, 200, 1200, 900);
        this.setContentPane(p);
        this.setTitle("Trajectory Visualization: Green is raw trajs, Blue are partitioned segments, Red are representative trajs -- Zisheng Yu");
        this.setVisible(true);
        this.setResizable(true);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        Graphics g = p.getGraphics();
        p.paint(g);

        // draw raw trajs
        if (rawTrajs != null) {
            for (Trajectory traj : rawTrajs) {
                for (int i = 0; i < traj.getPointNumber() - 1; i++) {
                    Point startPoint = traj.getPoint(i);
                    Point endPoint = traj.getPoint(i + 1);
                    g.setColor(Color.GREEN);
                    g.drawLine((int) startPoint.getLng(), (int) startPoint.getLat(), (int) endPoint.getLng(), (int) endPoint.getLat());
                }
            }
        }
        // draw partitioned segments
        if (segments != null) {
            for (int i = 0; i < segments.size() - 1; i++) {
                Point startPoint = segments.get(i).getStartPoint();
                Point endPoint = segments.get(i + 1).getEndPoint();
                g.setColor(Color.BLUE);
                g.drawLine((int) startPoint.getLng(), (int) startPoint.getLat(), (int) endPoint.getLng(), (int) endPoint.getLat());
            }
        }
        // draw representative trajs
        if (representativeTrajs != null) {
            for (Trajectory traj : representativeTrajs) {
                for (int i = 0; i < traj.getPointNumber() - 1; i++) {
                    Point startPoint = traj.getPoint(i);
                    Point endPoint = traj.getPoint(i + 1);
                    g.setColor(Color.RED);
                    g.drawLine((int) startPoint.getLng(), (int) startPoint.getLat(), (int) endPoint.getLng(), (int) endPoint.getLat());
                }
            }
        }
    }
}
