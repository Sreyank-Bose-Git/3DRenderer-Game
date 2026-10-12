package main;

import extras.AudioManager;
import handler.KeyHandler;
import handler.MouseHandler;
import renderer.Camera;
import handler.CollisionHandler;
import renderer.Renderer;
import renderer.World;
import visuals.TextureManager;

import javax.swing.*;
import java.awt.*;

public class RendererPanel extends JPanel implements Runnable{

    public final int resolution = 4;

    public final int singlePixelSize = 16 / resolution;

    public final int totalScreenCols = 80 * resolution;
    public final int totalScreenRows = 45 * resolution;

    public final int screenWidth = totalScreenCols * singlePixelSize;
    public final int screenHeight = totalScreenRows * singlePixelSize;

    public final int framesPerSecond = 200;
    public final long frameInterval = 1000000000 / framesPerSecond;
    public long workTime;

    public long startTime;
    public int frames;

    public Thread rendererThread;
    public boolean rendererRunning = true;

    public Renderer renderer;
    public World world;
    public KeyHandler keyHandler;
    public Camera camera;
    public MouseHandler mouseHandler;
    public Scripts scripts;
    public CollisionHandler collisionHandler;
    public TextureManager textureManager;
    public AudioManager audioManager;

    RendererPanel() {

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        keyHandler = new KeyHandler();
        this.addKeyListener(keyHandler);

        mouseHandler = new MouseHandler(this);
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        collisionHandler = new CollisionHandler();
        textureManager = new TextureManager(10);

        scripts = new Scripts(this);

        renderer = new Renderer(this);
        world = new World(200000, 10000, 10000, this);
        camera = new Camera(this);

        audioManager = new AudioManager(10);
        audioManager.playAndLoopAudio(0);

        rendererThread = new Thread(this);
        rendererThread.start();
    }

    @Override
    public void run() {

        workTime = System.nanoTime() + frameInterval;
        startTime = System.nanoTime();

        while(rendererRunning) {

            if(System.nanoTime() - startTime >= 1000000000) {
                System.out.println("FPS: " + frames);
                frames = 0;
                startTime = System.nanoTime();
            }

            if(workTime - System.nanoTime() <= 0) {

                update();

                repaint();

                frames++;
                workTime = System.nanoTime() + frameInterval;
            }
        }
    }

    public void update() {

        if(keyHandler.ePressed) {
            mouseHandler.isLocked = false;
        }

        renderer.update();
        camera.update();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2D = (Graphics2D) g;

        renderer.draw(g2D);

        g2D.dispose();
    }
}
