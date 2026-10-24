package exec.renderer;

import main.RendererPanel;
import types.datatypes.Vector3;
import types.things.meshes.Mesh;
import types.parts.Line;
import types.parts.Triangle;
import types.parts.Vertex;
import types.things.Thing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/** Renders mesh-owned geometry without storing global geometry arrays in World. */
public class Renderer {

    public final RendererPanel rendererPanel;

    public double focalLength = 1.0;
    public double nearPlane = 0.01;
    public double farPlane = 1000.0;

    public final double[] zBuffer;
    public final Color[] cBuffer;
    public final BufferedImage screenBuffer;
    private final int[] imagePixels;

    /* Current mesh context consumed by Scripts. These are reusable cached buffers,
       not geometry arrays owned by World. */
    public Vertex[] currentVertices;
    public Line[] currentLines;
    public Triangle[] currentTriangles;

    private final Map<Mesh, MeshRenderData> meshCache = new IdentityHashMap<>();

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
        if ("play".equals(rendererPanel.gameState)) {
            // Update game objects here.
        }
    }

    public void draw(Graphics2D g2D) {
        if (!"play".equals(rendererPanel.gameState) || g2D == null
                || rendererPanel.world == null) {
            return;
        }

        World world = rendererPanel.world;
        Camera camera = (Camera) world.things[world.activeCameraIndex];
        if (camera == null || camera.actualCoordinates == null || camera.actualRotation == null
                || rendererPanel.scripts == null) {
            drawBlackScreen(g2D);
            return;
        }

        Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
        Arrays.fill(cBuffer, Color.BLACK);

        // Remove cache entries for meshes no longer in this world.
        meshCache.keySet().removeIf(cached -> {
            for (Thing thing : world.things) if (thing == cached) return false;
            return true;
        });

        for (Thing thing : world.things) {
            if (thing == null) continue;

            if (!(thing instanceof Mesh mesh)) continue;
            if (!mesh.generated || !mesh.visible) continue;

            MeshRenderData data = getRenderData(mesh);
            updateTransformedVertices(mesh, data);

            currentVertices = data.vertices;
            currentLines = data.lines;
            currentTriangles = data.triangles;

            // Smooth normals are calculated for this mesh using transformed geometry.
            rendererPanel.scripts.beginFrame();

            for (Triangle triangle : currentTriangles) {
                if (triangle == null) continue;
                rendererPanel.scripts.drawTriangle(triangle, camera);
            }
            for (Line line : currentLines) {
                if (line == null) continue;
                rendererPanel.scripts.drawLine(line, camera);
            }
            for (Vertex vertex : currentVertices) {
                if (vertex == null) continue;
                rendererPanel.scripts.drawVertex(vertex, camera);
            }
        }

        currentVertices = null;
        currentLines = null;
        currentTriangles = null;

        for (int i = 0; i < cBuffer.length; i++) {
            Color color = cBuffer[i];
            imagePixels[i] = color == null ? 0xFF000000 : color.getRGB();
        }
        g2D.drawImage(screenBuffer, 0, 0, null);
    }

    private MeshRenderData getRenderData(Mesh mesh) {
        MeshRenderData data = meshCache.get(mesh);
        int vertexCount = mesh.vertices.size();
        int edgeCount = mesh.edges.size();
        int triangleCount = mesh.triangles.size();

        if (data != null && data.revision == mesh.geometryRevision
                && data.vertices.length == vertexCount
                && data.lines.length == edgeCount && data.triangles.length == triangleCount) {
            return data;
        }

        var meshVertices = mesh.vertices;
        var meshEdges = mesh.edges;
        var meshTriangles = mesh.triangles;

        Vertex[] vertices = new Vertex[meshVertices.size()];
        for (int i = 0; i < vertices.length; i++) {
            Mesh.VertexData source = meshVertices.get(i);
            vertices[i] = new Vertex(
                    new Vector3(source.position.x, source.position.y, source.position.z),
                    source.color, source.radius, source.opaque);
        }

        Line[] lines = new Line[meshEdges.size()];
        for (int i = 0; i < lines.length; i++) {
            Mesh.EdgeData source = meshEdges.get(i);
            lines[i] = new Line(source.vertex1, source.vertex2,
                    source.color, source.thickness, source.opaque);
        }

        Triangle[] triangles = new Triangle[meshTriangles.size()];
        for (int i = 0; i < triangles.length; i++) {
            Mesh.TriangleData source = meshTriangles.get(i);
            triangles[i] = new Triangle(source.vertex1, source.vertex2, source.vertex3,
                    source.texture, source.opaque,
                    source.u0, source.v0, source.u1, source.v1, source.u2, source.v2);
        }

        data = new MeshRenderData(vertices, lines, triangles, mesh.collectionName, mesh.geometryRevision);
        meshCache.put(mesh, data);
        return data;
    }

    private void updateTransformedVertices(Mesh mesh, MeshRenderData data) {
        for (int i = 0; i < data.vertices.length; i++) {
            Vector3 local = mesh.vertices.get(i).position;
            Vector3 out = data.vertices[i].coordinates;
            transformInto(local, mesh, out);
        }
    }

    /** Scale, rotate X then Y then Z, then translate, without per-frame allocations. */
    private void transformInto(Vector3 vertex, Mesh mesh, Vector3 out) {

        double x = (vertex.x * mesh.actualScale.x);
        double y = vertex.y * mesh.actualScale.y;
        double z = vertex.z * mesh.actualScale.z;

        double angle = Math.toRadians(mesh.actualRotation.x);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double ny = y * cos - z * sin;
        double nz = y * sin + z * cos;
        y = ny;
        z = nz;

        angle = Math.toRadians(mesh.actualRotation.y);
        cos = Math.cos(angle);
        sin = Math.sin(angle);
        double nx = x * cos + z * sin;
        nz = -x * sin + z * cos;
        x = nx;
        z = nz;

        angle = Math.toRadians(mesh.actualRotation.z);
        cos = Math.cos(angle);
        sin = Math.sin(angle);
        nx = x * cos - y * sin;
        ny = x * sin + y * cos;
        x = nx;
        y = ny;

        out.x = x + mesh.actualCoordinates.x;
        out.y = y + mesh.actualCoordinates.y;
        out.z = z + mesh.actualCoordinates.z;
    }

    private void drawBlackScreen(Graphics2D g2D) {
        Arrays.fill(imagePixels, Color.BLACK.getRGB());
        g2D.drawImage(screenBuffer, 0, 0, null);
    }

    private static final class MeshRenderData {
        final Vertex[] vertices;
        final Line[] lines;
        final Triangle[] triangles;
        final String collectionName;
        final long revision;

        MeshRenderData(Vertex[] vertices, Line[] lines, Triangle[] triangles, String collectionName, long revision) {
            this.vertices = vertices;
            this.lines = lines;
            this.triangles = triangles;
            this.collectionName = collectionName;
            this.revision = revision;
        }
    }
}
