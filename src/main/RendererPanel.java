package main;

import exec.extras.AudioManager;
import exec.handler.KeyHandler;
import exec.handler.MouseHandler;
import exec.renderer.Camera;
import exec.handler.CollisionHandler;
import exec.renderer.Renderer;
import exec.renderer.World;
import exec.visuals.LightManager;
import exec.visuals.TextureManager;

import javax.swing.*;
import java.awt.*;

public class RendererPanel extends JPanel implements Runnable{

    public final int resolution = 2; // less than or equal to 16

    public final int singlePixelSize = 16 / resolution;

    public final int totalScreenCols = 80 * resolution;
    public final int totalScreenRows = 45 * resolution;

    public final int screenWidth = totalScreenCols * singlePixelSize;
    public final int screenHeight = totalScreenRows * singlePixelSize;

    public final int framesPerSecond = 160;
    public final long frameInterval = 1000000000 / framesPerSecond;
    public long workTime;

    public long startTime;
    public int frames;

    public String gameState;

    public Thread rendererThread;
    public boolean rendererRunning = true;

    PointerInfo pointerInfo;
    Point mouseLocation;

    public Renderer renderer;
    public World world;
    public KeyHandler keyHandler;
    public MouseHandler mouseHandler;
    public Scripts scripts;
    public CollisionHandler collisionHandler;
    public TextureManager textureManager;
    public AudioManager audioManager;
    public LightManager lightManager;
    public Interface myInterface;

    RendererPanel() {

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setDoubleBuffered(true);
        this.setFocusable(true);
        this.setBackground(Color.BLACK);

        keyHandler = new KeyHandler();
        this.addKeyListener(keyHandler);

        mouseHandler = new MouseHandler(this);
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        collisionHandler = new CollisionHandler();
        textureManager = new TextureManager(10);

        lightManager = new LightManager();

        scripts = new Scripts(this);

        renderer = new Renderer(this);
        world = new World(100, 100, 100, 100, 100, 10,this);

        audioManager = new AudioManager(10);

        gameState = "home";
        myInterface = new Interface(this);

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

        pointerInfo = MouseInfo.getPointerInfo();
        mouseLocation = pointerInfo.getLocation();

        mouseHandler.isLocked = !keyHandler.ePressed;

        renderer.update();
        world.cameras[world.activeCameraIndex].update();
        myInterface.update();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2D = (Graphics2D) g;

        renderer.draw(g2D);
        myInterface.draw(g2D);

        g2D.dispose();
    }
}
