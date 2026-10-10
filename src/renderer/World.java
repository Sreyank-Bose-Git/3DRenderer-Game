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

        int blocksX = 20;
        int blocksZ = 20;
        double blockSize = 20;

        double startX = -(blocksX * blockSize) / 2.0;
        double startZ = -250;

        // Shared grid vertices: prevents gaps between blocks
        int[][] vertexIndices = new int[blocksX + 1][blocksZ + 1];

        // Generate all vertices first
        for (int x = 0; x <= blocksX; x++) {
            for (int z = 0; z <= blocksZ; z++) {

                double worldX = startX + x * blockSize;
                double worldZ = startZ + z * blockSize;

                double centerX = 0;
                double centerZ = startZ + blocksZ * blockSize / 2.0;

                double dx = worldX - centerX;
                double dz = worldZ - centerZ;

                double distance = Math.sqrt(dx * dx + dz * dz);

                // Mountain shape
                double mountainRadius = 120;
                double maxHeight = 120;

                double height = 0;

                if (distance < mountainRadius) {
                    double factor = 1.0 - distance / mountainRadius;
                    height = maxHeight * factor * factor;
                }

                // Rolling hills around the mountain
                height += 5.0
                        * Math.sin(worldX * 0.035)
                        * Math.cos(worldZ * 0.035);

                // Add vertex and remember its index
                vertexIndices[x][z] = addTerrainVertex(
                        worldX, height, worldZ
                );
            }
        }

        // Generate two angled triangles for every block
        for (int x = 0; x < blocksX; x++) {
            for (int z = 0; z < blocksZ; z++) {

                int v0 = vertexIndices[x][z];
                int v1 = vertexIndices[x + 1][z];
                int v2 = vertexIndices[x + 1][z + 1];
                int v3 = vertexIndices[x][z + 1];

                // Triangle 1
                storeTriangles(
                        v0, v1, v2,
                        grass, true,
                        0, 0,
                        1, 0,
                        1, 1
                );

                // Triangle 2
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