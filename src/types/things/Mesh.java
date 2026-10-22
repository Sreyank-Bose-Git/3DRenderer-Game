package types.things;

import types.datatypes.Vector3;
import types.visuals.Texture;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Mesh extends Thing {

    // Transform
    public Vector3 scale = new Vector3(1, 1, 1);

    // Rendering properties
    public boolean visible = true;
    public boolean opaque = true;

    public Color vertexColor = Color.WHITE;
    public int vertexRadius = 1;

    public boolean generateEdges = true;
    public Color edgeColor = Color.BLACK;
    public float edgeThickness = 1.0f;

    // Geometry owned by this mesh
    public final List<VertexData> vertices = new ArrayList<>();
    public final List<EdgeData> edges = new ArrayList<>();
    public final List<TriangleData> triangles = new ArrayList<>();

    // Lifecycle
    public boolean generated = false;
    public boolean disposed = false;

    /** Incremented whenever mesh geometry is regenerated or disposed. */
    public long geometryRevision = 0;

    protected Mesh() {
    }

    // Geometry generation
    public void generate() {
        if (disposed) {
            throw new IllegalStateException("Cannot generate a disposed mesh.");
        }

        vertices.clear();
        edges.clear();
        triangles.clear();

        generateGeometry();
        generated = true;
        geometryRevision++;
    }

    protected void generateGeometry() {

    }

    public void updateTransform() {
        // Transform is applied by Renderer; override if needed.
    }

    // Cleanup
    public void dispose() {
        vertices.clear();
        edges.clear();
        triangles.clear();
        generated = false;
        disposed = true;
        geometryRevision++;
    }

    // Mesh geometry data types
    public static class VertexData {
        public Vector3 position;
        public Color color;
        public int radius;
        public boolean opaque;

        public VertexData(Vector3 position, Color color, int radius, boolean opaque) {
            this.position = position;
            this.color = color;
            this.radius = radius;
            this.opaque = opaque;
        }
    }

    public static class EdgeData {
        public int vertex1;
        public int vertex2;
        public Color color;
        public float thickness;
        public boolean opaque;

        public EdgeData(int vertex1, int vertex2, Color color,
                        float thickness, boolean opaque) {
            this.vertex1 = vertex1;
            this.vertex2 = vertex2;
            this.color = color;
            this.thickness = thickness;
            this.opaque = opaque;
        }
    }

    public static class TriangleData {
        public int vertex1;
        public int vertex2;
        public int vertex3;
        public Texture texture;
        public boolean opaque;
        public double u0, v0;
        public double u1, v1;
        public double u2, v2;

        public TriangleData(int vertex1, int vertex2, int vertex3,
                            Texture texture, boolean opaque,
                            double u0, double v0,
                            double u1, double v1,
                            double u2, double v2) {
            this.vertex1 = vertex1;
            this.vertex2 = vertex2;
            this.vertex3 = vertex3;
            this.texture = texture;
            this.opaque = opaque;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }
    }
}
