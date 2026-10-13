package exec.handler;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class MouseHandler extends MouseAdapter implements MouseMotionListener {

    public double deltaYaw = 0;
    public double deltaPitch = 0;

    public boolean isLocked = false;

    private Robot robot;
    private Component panel;
    private double sensitivity = 0.15;

    public MouseHandler(Component panel) {

        this.panel = panel;
        try { this.robot = new Robot(); } catch (Exception ignored) {}
    }

    @Override
    public void mousePressed(MouseEvent e) {
        isLocked = true;
        recenter();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!isLocked) return;

        int dx = e.getX() - (panel.getWidth() / 2);
        int dy = e.getY() - (panel.getHeight() / 2);

        if (dx == 0 && dy == 0) return;

        deltaYaw = dx * sensitivity;
        deltaPitch = -dy * sensitivity;

        recenter();
    }

    private void recenter() {
        if (!panel.isShowing()) return;
        Point p = panel.getLocationOnScreen();
        robot.mouseMove(p.x + panel.getWidth() / 2, p.y + panel.getHeight() / 2);
    }
}
