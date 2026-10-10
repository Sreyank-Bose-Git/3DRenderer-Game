package renderer;

import main.RendererPanel;
import visuals.Texture;
import objects.Line;
import objects.Triangle;
import datatypes.Vector3;
import objects.Vertex;

import java.awt.*;

public class World {

    public Vertex[] vertices;
    public Line[] lines;
    public Triangle[] triangles;

    RendererPanel rendererPanel;

    public World(int vSize, int lSize, int tSize, RendererPanel rendererPanel) {

        vertices = new Vertex[vSize];
        lines = new Line[lSize];
        triangles = new Triangle[tSize];
        this.rendererPanel = rendererPanel;

        createBasicWorld();
    }


    void createBasicWorld() {

        // Vertices
        storeVertex(new Vector3(-50, 0, -250), Color.WHITE, 1, true); // 0
        storeVertex(new Vector3( 50, 0, -250), Color.WHITE, 1, true); // 1
        storeVertex(new Vector3( 50, 0, -150), Color.WHITE, 1, true); // 2
        storeVertex(new Vector3(-50, 0, -150), Color.WHITE, 1, true); // 3

// Textured triangles: u1, v1, u2, v2, u3, v3
        storeTriangles(0, 1, 2, rendererPanel.textureManager.textures[0], true, 0, 0, 1, 0, 1, 1);
        storeTriangles(0, 2, 3, rendererPanel.textureManager.textures[0], true, 0, 0, 1, 1, 0, 1);
    }


    // =========================================================
    // STORE VERTEX
    // =========================================================

    public void storeVertex(
            Vector3 coordinates,
            Color color,
            int radius,
            boolean opacity) {

        int i = 0;

        while (i < vertices.length && vertices[i] != null) {
            i++;
        }

        if (i >= vertices.length) {
            return;
        }

        vertices[i] =
                new Vertex(
                        coordinates,
                        color,
                        radius,
                        opacity
                );
    }


    // =========================================================
    // STORE LINE
    // =========================================================

    public void storeLine(
            int index1,
            int index2,
            Color color,
            float thickness,
            boolean opacity) {

        int i = 0;

        while (i < lines.length && lines[i] != null) {
            i++;
        }

        if (i >= lines.length) {
            return;
        }

        lines[i] =
                new Line(
                        index1,
                        index2,
                        color,
                        thickness,
                        opacity
                );
    }


    // =========================================================
    // STORE TRIANGLE
    // =========================================================

    public void storeTriangles(
            int index1,
            int index2,
            int index3,
            Texture texture,
            boolean opacity,
            double u1, double v1,
            double u2, double v2,
            double u3, double v3) {

        int i = 0;

        while (i < triangles.length && triangles[i] != null) {
            i++;
        }

        if (i >= triangles.length) {
            return;
        }

        triangles[i] =
                new Triangle(
                        index1,
                        index2,
                        index3,
                        texture,
                        opacity,
                        u1, v1,
                        u2, v2,
                        u3, v3
                );
    }
}