package renderer;

import main.RendererPanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class Renderer {

    RendererPanel rendererPanel;

    public double focalLength = 1;

    public double nearPlane = 0.01;
    public double farPlane = 1000;

    // =========================================================
    // BUFFERS
    // =========================================================

    public double[] zBuffer;

    public Color[] cBuffer;

    public BufferedImage screenBuffer;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Renderer(RendererPanel rendererPanel) {

        this.rendererPanel = rendererPanel;

        int size =
                rendererPanel.screenWidth *
                        rendererPanel.screenHeight;


        // Z-buffer

        zBuffer =
                new double[size];


        // Color buffer

        cBuffer =
                new Color[size];


        // Buffered image

        screenBuffer =
                new BufferedImage(
                        rendererPanel.screenWidth,
                        rendererPanel.screenHeight,
                        BufferedImage.TYPE_INT_ARGB
                );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void update() {

    }


    // =========================================================
    // DRAW
    // =========================================================

    public void draw(Graphics2D g2D) {

        // =====================================================
        // CLEAR Z-BUFFER
        // =====================================================

        Arrays.fill(
                zBuffer,
                Double.POSITIVE_INFINITY
        );


        // =====================================================
        // CLEAR COLOR BUFFER
        // =====================================================

        Arrays.fill(
                cBuffer,
                Color.BLACK
        );


        // =====================================================
        // DRAW TRIANGLES
        // =====================================================

        for (int i = 0;
             i < rendererPanel.world.triangles.length;
             i++) {

            if (rendererPanel.world.triangles[i] == null) {
                break;
            }

            rendererPanel.scripts.drawTriangle(
                    rendererPanel.world.triangles[i],
                    rendererPanel.camera
            );
        }


        // =====================================================
        // DRAW LINES
        // =====================================================

        for (int i = 0;
             i < rendererPanel.world.lines.length;
             i++) {

            if (rendererPanel.world.lines[i] == null) {
                break;
            }

            rendererPanel.scripts.drawLine(
                    rendererPanel.world.lines[i],
                    rendererPanel.camera
            );
        }


        // =====================================================
        // DRAW VERTICES
        // =====================================================

        for (int i = 0;
             i < rendererPanel.world.vertices.length;
             i++) {

            if (rendererPanel.world.vertices[i] == null) {
                break;
            }

            rendererPanel.scripts.drawVertex(
                    rendererPanel.world.vertices[i],
                    rendererPanel.camera
            );
        }


        // =====================================================
        // COPY COLOR BUFFER → BUFFERED IMAGE
        // =====================================================

        for (int y = 0;
             y < rendererPanel.screenHeight;
             y++) {

            for (int x = 0;
                 x < rendererPanel.screenWidth;
                 x++) {

                int index =
                        y * rendererPanel.screenWidth + x;


                Color color =
                        cBuffer[index];


                if (color == null) {
                    color = Color.BLACK;
                }


                screenBuffer.setRGB(
                        x,
                        y,
                        color.getRGB()
                );
            }
        }


        // =====================================================
        // DRAW BUFFERED IMAGE TO SCREEN
        // =====================================================

        g2D.drawImage(
                screenBuffer,
                0,
                0,
                null
        );
    }
}