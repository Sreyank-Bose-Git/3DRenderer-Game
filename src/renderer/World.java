package renderer;

import main.RendererPanel;
import visuals.Texture;
import objects.Line;
import objects.Triangle;
import datatypes.Vector3;
import objects.Vertex;

import java.awt.*;
import advanced.Object;

public class World {

    public Vertex[] vertices;
    public Line[] lines;
    public Triangle[] triangles;

    public RendererPanel rendererPanel;

    public World(int vSize, int lSize, int tSize,
                 RendererPanel rendererPanel) {

        vertices = new Vertex[Math.max(1, vSize)];
        lines = new Line[Math.max(1, lSize)];
        triangles = new Triangle[Math.max(1, tSize)];

        this.rendererPanel = rendererPanel;

        createBasicWorld();
    }

    void createBasicWorld() {
        try {
            long start = System.nanoTime();

            Object cube = new Object(
                    this,
                    "/objects/cube.obj"
            );

            System.out.println("OBJ/MTL loading: "
                    + (System.nanoTime() - start) / 1_000_000 + " ms");

            cube.scaleX = 10;
            cube.scaleY = 10;
            cube.scaleZ = 10;

            start = System.nanoTime();
            cube.generate();

            System.out.println("Geometry generation: "
                    + (System.nanoTime() - start) / 1_000_000 + " ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // AUTOMATIC ARRAY GROWTH
    // =========================================================

    private int findNextVertexIndex() {
        for (int i = 0; i < vertices.length; i++) {
            if (vertices[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextLineIndex() {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextTriangleIndex() {
        for (int i = 0; i < triangles.length; i++) {
            if (triangles[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private void growVertices() {
        vertices = java.util.Arrays.copyOf(
                vertices, vertices.length * 2
        );
    }

    private void growLines() {
        lines = java.util.Arrays.copyOf(
                lines, lines.length * 2
        );
    }

    private void growTriangles() {
        triangles = java.util.Arrays.copyOf(
                triangles, triangles.length * 2
        );
    }

    // =========================================================
    // NEXT VERTEX INDEX
    // =========================================================

    public int nextVertexIndex() {
        int index = findNextVertexIndex();

        if (index == -1) {
            growVertices();
            index = findNextVertexIndex();
        }

        return index;
    }

    // =========================================================
    // ADD TERRAIN VERTEX
    // =========================================================

    int addTerrainVertex(double x, double y, double z) {
        int index = nextVertexIndex();

        storeVertex(
                new Vector3(x, y, z),
                Color.WHITE,
                1,
                true
        );

        return index;
    }

    // =========================================================
    // STORE VERTEX
    // =========================================================

    public void storeVertex(
            Vector3 coordinates,
            Color color,
            int radius,
            boolean opacity) {

        int index = findNextVertexIndex();

        if (index == -1) {
            growVertices();
            index = findNextVertexIndex();
        }

        vertices[index] = new Vertex(
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

        int index = findNextLineIndex();

        if (index == -1) {
            growLines();
            index = findNextLineIndex();
        }

        lines[index] = new Line(
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

        int index = findNextTriangleIndex();

        if (index == -1) {
            growTriangles();
            index = findNextTriangleIndex();
        }

        triangles[index] = new Triangle(
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