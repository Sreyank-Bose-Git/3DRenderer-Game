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

        Texture grass = rendererPanel.textureManager.textures[0];

        // Terrain settings
        int blocksX = 20;
        int blocksZ = 20;
        double blockSize = 20;

        double startX = -(blocksX * blockSize) / 2.0;
        double startZ = -250;

        // Create terrain blocks using nested loops
        for (int x = 0; x < blocksX; x++) {
            for (int z = 0; z < blocksZ; z++) {

                double worldX = startX + x * blockSize;
                double worldZ = startZ + z * blockSize;

                // Distance from the mountain center
                double centerX = 0;
                double centerZ = startZ + (blocksZ * blockSize) / 2.0;

                double dx = worldX + blockSize / 2.0 - centerX;
                double dz = worldZ + blockSize / 2.0 - centerZ;

                double distance = Math.sqrt(dx * dx + dz * dz);

                // Mountain height decreases with distance
                double mountainRadius = 100;
                double maxHeight = 100;

                double height = 0;

                if (distance < mountainRadius) {
                    double factor = 1.0 - distance / mountainRadius;
                    height = maxHeight * factor * factor;
                }

                // Add some smaller hills
                height += Math.max(0,
                        8 * Math.sin(worldX * 0.04)
                                * Math.cos(worldZ * 0.04));

                // Four corners of each terrain block
                int v0 = addTerrainVertex(
                        worldX, height, worldZ);

                int v1 = addTerrainVertex(
                        worldX + blockSize, height, worldZ);

                int v2 = addTerrainVertex(
                        worldX + blockSize, height,
                        worldZ + blockSize);

                int v3 = addTerrainVertex(
                        worldX, height,
                        worldZ + blockSize);

                // First textured triangle
                storeTriangles(
                        v0, v1, v2,
                        grass, true,
                        0, 0,
                        1, 0,
                        1, 1
                );

                // Second textured triangle
                storeTriangles(
                        v0, v2, v3,
                        grass, true,
                        0, 0,
                        1, 1,
                        0, 1
                );
            }
        }
    }

    // Helper: add a vertex and return its actual index
    int addTerrainVertex(double x, double y, double z) {

        int index = 0;

        while (index < vertices.length && vertices[index] != null) {
            index++;
        }

        if (index >= vertices.length) {
            throw new IllegalStateException(
                    "Not enough vertex capacity! Increase vSize."
            );
        }

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