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
    protected final List<VertexData> vertices = new ArrayList<>();
    protected final List<EdgeData> edges = new ArrayList<>();
    protected final List<TriangleData> triangles = new ArrayList<>();

    // Lifecycle
    protected boolean generated = false;
    protected boolean disposed = false;

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

    // Geometry creation
    protected int addVertex(Vector3 position) {
        vertices.add(new VertexData(position, vertexColor, vertexRadius, opaque));
        return vertices.size() - 1;
    }

    protected int addVertex(Vector3 position, Color color, int radius, boolean opaque) {
        vertices.add(new VertexData(position, color, radius, opaque));
        return vertices.size() - 1;
    }

    protected void addEdge(int vertex1, int vertex2) {
        addEdge(vertex1, vertex2, edgeColor, edgeThickness, opaque);
    }

    protected void addEdge(int vertex1, int vertex2, Color color,
                           float thickness, boolean opaque) {
        checkVertexIndex(vertex1);
        checkVertexIndex(vertex2);
        edges.add(new EdgeData(vertex1, vertex2, color, thickness, opaque));
    }

    protected void addTriangle(int vertex1, int vertex2, int vertex3,
                               Texture texture, boolean opaque,
                               double u0, double v0,
                               double u1, double v1,
                               double u2, double v2) {
        checkVertexIndex(vertex1);
        checkVertexIndex(vertex2);
        checkVertexIndex(vertex3);
        triangles.add(new TriangleData(vertex1, vertex2, vertex3, texture, opaque,
                u0, v0, u1, v1, u2, v2));
    }

    private void checkVertexIndex(int index) {
        if (index < 0 || index >= vertices.size()) {
            throw new IndexOutOfBoundsException("Invalid mesh vertex index: " + index);
        }
    }

    // Geometry access
    /** Indexed access avoids copying the whole vertex list during rendering. */
    public VertexData getVertexData(int index) {
        return vertices.get(index);
    }

    public List<VertexData> getVertices() {
        return List.copyOf(vertices);
    }

    public List<EdgeData> getEdges() {
        return List.copyOf(edges);
    }

    public List<TriangleData> getTriangles() {
        return List.copyOf(triangles);
    }


    public TriangleData getTriangleData(int index) {
        return triangles.get(index);
    }


    public int getVertexCount() {
        return vertices.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

    public int getTriangleCount() {
        return triangles.size();
    }

    // State
    public boolean isGenerated() {
        return generated;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public boolean isVisible() {
        return visible && !disposed;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
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
