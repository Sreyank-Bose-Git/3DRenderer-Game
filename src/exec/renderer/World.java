package exec.renderer;

import main.RendererPanel;
import types.datatypes.Vector2;
import types.visuals.Light;
import types.visuals.Texture;
import types.objects.Line;
import types.objects.Triangle;
import types.datatypes.Vector3;
import types.objects.Vertex;

import java.awt.*;
import types.advanced.Object;

public class World {

    public Vertex[] vertices;
    public Line[] lines;
    public Triangle[] triangles;
    public Object[] objects;
    public Light[] lights;

    public int activeCameraIndex;
    public Camera[] cameras;

    public RendererPanel rendererPanel;

    public World(int verticesSize, int linesSize, int trianglesSize, int objectsSize, int lightsSize, int camerasSize,
                 RendererPanel rendererPanel) {

        vertices = new Vertex[Math.max(1, verticesSize)];
        lines = new Line[Math.max(1, linesSize)];
        triangles = new Triangle[Math.max(1, trianglesSize)];
        objects = new Object[objectsSize];
        lights = new Light[lightsSize];
        cameras = new Camera[camerasSize];

        this.rendererPanel = rendererPanel;

        createBasicWorld();
    }

    void createBasicWorld() {

        storeCamera(new Vector3(0, 50, -200), new Vector2(0, 0));
        activeCameraIndex = 0;

        storeLight(
                "point",
                5.0,
                new Vector3(0, 50, -200),
                new Vector3(0, 0, 180)
        );

        storeObject("/types/objects/cube.obj");
        objects[0].scaleZ *= 1000;
        objects[0].scaleY *= 10;
        objects[0].scaleX *= 1000;
        objects[0].updateTransform();
    }

    // =========================================================
    // AUTOMATIC ARRAY GROWTH
    // =========================================================

    private int findNextCameraIndex() {
        for (int i = 0; i < cameras.length; i++) {
            if (cameras[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextLightIndex() {
        for (int i = 0; i < lights.length; i++) {
            if (lights[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextObjectIndex() {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                return i;
            }
        }

        return -1;
    }

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

    private void growCameras() {
        cameras = java.util.Arrays.copyOf(
                cameras, cameras.length * 2
        );
    }

    private void growLights() {
        lights = java.util.Arrays.copyOf(
                lights, lights.length * 2
        );
    }

    private void growObjects() {
        objects = java.util.Arrays.copyOf(
                objects, objects.length * 2
        );
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


    public void updateVertex(
            int index,
            Vector3 coordinates,
            Color color,
            int radius,
            boolean opacity) {

        vertices[index] = new Vertex(
                coordinates,
                color,
                radius,
                opacity
        );
    }


    // =========================================================
    // STORE VERTEX
    // =========================================================

    public void storeCamera(Vector3 coordinates, Vector2 rotation) {
        int index = findNextCameraIndex();

        if (index == -1) {
            growCameras();
            index = findNextCameraIndex();
        }

        cameras[index] = new Camera(rendererPanel, coordinates, rotation);
    }

    public void storeLight(String lightType, double lightStrength, Vector3 coordinates, Vector3 rotation) {
        int index = findNextLightIndex();

        if (index == -1) {
            growLights();
            index = findNextLightIndex();
        }

        lights[index] = new Light(lightType, lightStrength, coordinates, rotation);
    }

    public void storeObject(String objPath) {
        try {
            int index = findNextObjectIndex();

            if (index == -1) {
                growObjects();
                index = findNextObjectIndex();
            }

            objects[index] = new Object(this, objPath);
            objects[index].generate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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