package exec.renderer;

import main.RendererPanel;
import types.objects.Line;
import types.objects.Triangle;
import types.objects.Vertex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Objects;

public class Renderer {

    public final RendererPanel rendererPanel;

    public double focalLength = 1.0;
    public double nearPlane = 0.01;
    public double farPlane = 1000.0;

    public final double[] zBuffer;
    public final Color[] cBuffer;
    public final BufferedImage screenBuffer;

    // Direct access avoids calling BufferedImage.setRGB once per screen pixel.
    private final int[] imagePixels;

    public Renderer(RendererPanel rendererPanel) {
        if (rendererPanel == null) {
            throw new IllegalArgumentException("rendererPanel cannot be null");
        }
        this.rendererPanel = rendererPanel;

        int width = rendererPanel.screenWidth;
        int height = rendererPanel.screenHeight;

        int size = Math.multiplyExact(width, height);
        zBuffer = new double[size];
        cBuffer = new Color[size];
        screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        imagePixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
    }

    public void update() {
        if(rendererPanel.gameState.equals("play")) {
            // For playing.


        }
    }

    public void draw(Graphics2D g2D) {
        if(rendererPanel.gameState.equals("play")) {
            if (g2D == null || rendererPanel.world == null) return;

            Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
            Arrays.fill(cBuffer, Color.BLACK);

            if (rendererPanel.world.cameras == null ||
                    rendererPanel.world.activeCameraIndex < 0 ||
                    rendererPanel.world.activeCameraIndex >= rendererPanel.world.cameras.length) {
                Arrays.fill(imagePixels, Color.BLACK.getRGB());
                g2D.drawImage(screenBuffer, 0, 0, null);
                return;
            }

            Camera camera = rendererPanel.world.cameras[rendererPanel.world.activeCameraIndex];
            if (camera == null || camera.coordinates == null || camera.rotation == null) {
                Arrays.fill(imagePixels, Color.BLACK.getRGB());
                g2D.drawImage(screenBuffer, 0, 0, null);
                return;
            }

            // Precompute smooth vertex normals once per frame, rather than rescanning
            // all triangles for each triangle vertex.
            if (rendererPanel.scripts != null) {
                rendererPanel.scripts.beginFrame();
            }

            if (rendererPanel.world.triangles != null && rendererPanel.scripts != null) {
                for (Triangle triangle : rendererPanel.world.triangles) {
                    if (triangle == null) break; // World arrays are expected to be densely packed.
                    rendererPanel.scripts.drawTriangle(triangle, camera);
                }
            }

            if (rendererPanel.world.lines != null && rendererPanel.scripts != null) {
                for (Line line : rendererPanel.world.lines) {
                    if (line == null) break;
                    rendererPanel.scripts.drawLine(line, camera);
                }
            }

            if (rendererPanel.world.vertices != null && rendererPanel.scripts != null) {
                for (Vertex vertex : rendererPanel.world.vertices) {
                    if (vertex == null) break;
                    rendererPanel.scripts.drawVertex(vertex, camera);
                }
            }

            // Copy the Color[] framebuffer to the image in one linear pass.
            for (int i = 0; i < cBuffer.length; i++) {
                Color color = cBuffer[i];
                imagePixels[i] = color == null ? 0xFF000000 : color.getRGB();
            }

            g2D.drawImage(screenBuffer, 0, 0, null);
        }
    }
}
