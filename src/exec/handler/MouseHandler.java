package exec.handler;

import main.RendererPanel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class MouseHandler extends MouseAdapter implements MouseMotionListener {

    public double deltaYaw = 0;
    public double deltaPitch = 0;

    public boolean isLocked = false;
    public boolean mousePressed = false;

    private Robot robot;
    private RendererPanel rendererPanel;
    private double sensitivity = 0.15;

    public MouseHandler(RendererPanel rendererPanel) {

        this.rendererPanel = rendererPanel;
        try { this.robot = new Robot(); } catch (Exception ignored) {}
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
        if(rendererPanel.gameState.equals("play") && isLocked) {
            recenter();
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!isLocked) return;

        int dx = e.getX() - (rendererPanel.getWidth() / 2);
        int dy = e.getY() - (rendererPanel.getHeight() / 2);

        if (dx == 0 && dy == 0) return;

        deltaYaw = dx * sensitivity;
        deltaPitch = -dy * sensitivity;

        recenter();
    }

    private void recenter() {
        if (!rendererPanel.isShowing()) return;
        Point p = rendererPanel.getLocationOnScreen();
        robot.mouseMove(p.x + rendererPanel.getWidth() / 2, p.y + rendererPanel.getHeight() / 2);
    }
}
