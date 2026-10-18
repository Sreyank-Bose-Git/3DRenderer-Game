package types.meshes;

import types.datatypes.Vector3;
import exec.file.MaterialParser;
import exec.file.ObjectParser;
import exec.renderer.World;
import types.visuals.Texture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Object extends Mesh {

    private final World world;
    private final ObjectParser.Data data;

    private double[] center = {0, 0, 0};

    public boolean centerAtOrigin = true;

    // Default texture for faces without a usable material texture.
    public Texture texture;

    // Materials loaded from the OBJ's MTL file.
    public Map<String, MaterialParser.Material> materials =
            new LinkedHashMap<>();

    // Cache textures so the same material image is not loaded repeatedly.
    private final Map<String, Texture> textureCache = new HashMap<>();

    public Object(World world, String objPath) throws IOException {
        this.world = world;
        this.data = new ObjectParser().parse(objPath);

        if (data.materialLibrary != null
                && !data.materialLibrary.isBlank()) {

            String resolvedMtlPath =
                    resolveMtlPath(objPath, data.materialLibrary);

            this.materials =
                    new MaterialParser().parse(resolvedMtlPath);
        }

        System.out.println("OBJ loaded: " + objPath);
        System.out.println("Materials found: " + materials.keySet());
    }

    private String resolveMtlPath(String objPath, String mtlPath) {
        if (mtlPath.startsWith("/")) {
            return mtlPath;
        }

        int lastSlash = objPath.lastIndexOf('/');

        String directory = lastSlash >= 0
                ? objPath.substring(0, lastSlash + 1)
                : "/";

        return directory + mtlPath;
    }

    /**
     * Builds this mesh's local geometry.
     * Mesh.generate() clears the geometry lists before calling this method.
     * The vertices, edges and triangles are stored in this Mesh, not World.
     */
    @Override
    protected void generateGeometry() {

        int vertexCount = data.vertices.size();
        int triangleCount = 0;

        center = calculateCenter();

        System.out.println("OBJ vertices: " + vertexCount);
        System.out.println("OBJ faces: " + data.faces.size());
        System.out.println("OBJ explicit edges: " + data.edges.size());
        System.out.println("UV coordinates: " + data.uvs.size());
        System.out.println("Materials loaded: " + materials.keySet());

        // 1. Store vertices in this mesh's local coordinate system.
        for (double[] v : data.vertices) {
            Vector3 position = new Vector3(
                    v[0] - center[0],
                    v[1] - center[1],
                    v[2] - center[2]
            );

            addVertex(position, vertexColor, vertexRadius, opaque);
        }

        // Use a set to avoid storing duplicate edges.
        Set<Long> uniqueEdges = new HashSet<>();

        // 2. Triangulate each OBJ face and preserve its material and UVs.
        for (ObjectParser.Face face : data.faces) {

            if (face.vertices.size() < 3) {
                continue;
            }

            MaterialParser.Material material =
                    materials.get(face.materialName);

            Texture faceTexture = texture;
            boolean faceOpaque = opaque;

            if (material != null) {
                faceOpaque = opaque && material.opacity >= 1.0;
                faceTexture = getMaterialTexture(material);
            }

            ObjectParser.FaceVertex first = face.vertices.getFirst();

            // Triangle fan: (first, i, i + 1).
            for (int i = 1; i < face.vertices.size() - 1; i++) {

                ObjectParser.FaceVertex b = face.vertices.get(i);
                ObjectParser.FaceVertex c = face.vertices.get(i + 1);

                double[] uv0 = getUV(first.uvIndex);
                double[] uv1 = getUV(b.uvIndex);
                double[] uv2 = getUV(c.uvIndex);

                addTriangle(
                        first.vertexIndex,
                        b.vertexIndex,
                        c.vertexIndex,
                        faceTexture,
                        faceOpaque,
                        uv0[0], uv0[1],
                        uv1[0], uv1[1],
                        uv2[0], uv2[1]
                );

                triangleCount++;
            }

            // 3. Collect the polygon's boundary edges.
            if (generateEdges) {
                for (int i = 0; i < face.vertices.size(); i++) {

                    int a = face.vertices.get(i).vertexIndex;

                    int b = face.vertices.get(
                            (i + 1) % face.vertices.size()
                    ).vertexIndex;

                    uniqueEdges.add(edgeKey(a, b));
                }
            }
        }

        // 4. Include explicitly declared OBJ line geometry.
        if (generateEdges) {
            for (ObjectParser.Edge edge : data.edges) {
                uniqueEdges.add(edgeKey(edge.vertex1, edge.vertex2));
            }
        }

        // 5. Store unique edges using this mesh's local vertex indices.
        for (long key : uniqueEdges) {
            int a = (int) (key >>> 32);
            int b = (int) key;

            addEdge(a, b, edgeColor, edgeThickness, opaque);
        }

        System.out.println("Generated mesh vertices: " + getVertexCount());
        System.out.println("Generated mesh triangles: " + triangleCount);
        System.out.println("Generated unique edges: " + getEdgeCount());
    }

    private Texture getMaterialTexture(
            MaterialParser.Material material) {

        // Use the diffuse color as a solid-color texture by default.
        Texture result = new Texture(material.diffuseColor);

        if (material.diffuseMap == null
                || material.diffuseMap.isBlank()) {
            return result;
        }

        String path = material.diffuseMap;

        Texture cached = textureCache.get(path);
        if (cached != null) {
            return cached;
        }

        try {
            result = world.rendererPanel.textureManager
                    .loadMaterialTexture(path);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not load material texture: " + path, e
            );
        }

        textureCache.put(path, result);
        return result;
    }

    private double[] calculateCenter() {

        if (!centerAtOrigin || data.vertices.isEmpty()) {
            return new double[]{0, 0, 0};
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double[] v : data.vertices) {
            minX = Math.min(minX, v[0]);
            minY = Math.min(minY, v[1]);
            minZ = Math.min(minZ, v[2]);

            maxX = Math.max(maxX, v[0]);
            maxY = Math.max(maxY, v[1]);
            maxZ = Math.max(maxZ, v[2]);
        }

        // Center the model around its bounding-box center.
        return new double[]{
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0
        };
    }

    private double[] getUV(int index) {
        if (index < 0 || index >= data.uvs.size()) {
            return new double[]{0, 0};
        }

        return data.uvs.get(index);
    }

    private long edgeKey(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);

        return ((long) min << 32) | (max & 0xffffffffL);
    }

    /**
     * Geometry remains in local space. The renderer should apply this
     * mesh's position, scale and rotation when drawing it.
     */
    @Override
    public void updateTransform() {
        // No vertex rewriting is needed in the local-space design.
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }

        super.dispose();
        textureCache.clear();
    }
}