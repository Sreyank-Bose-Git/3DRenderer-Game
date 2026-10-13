package exec.handler;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean forward, backward, leftward, rightward, jumpPressed, ePressed;

    @Override
    public void keyTyped(KeyEvent e) {
        if(e.getKeyChar() == 'e') {
            ePressed = !ePressed;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch(e.getKeyCode()) {
            case KeyEvent.VK_W:
                forward = true;
                break;
            case KeyEvent.VK_S:
                backward = true;
                break;
            case KeyEvent.VK_A:
                leftward = true;
                break;
            case KeyEvent.VK_D:
                rightward = true;
                break;
            case KeyEvent.VK_SPACE:
                jumpPressed = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch(e.getKeyCode()) {
            case KeyEvent.VK_W:
                forward = false;
                break;
            case KeyEvent.VK_S:
                backward = false;
                break;
            case KeyEvent.VK_A:
                leftward = false;
                break;
            case KeyEvent.VK_D:
                rightward = false;
                break;
            case KeyEvent.VK_SPACE:
                jumpPressed = false;
                break;
        }
    }
}
