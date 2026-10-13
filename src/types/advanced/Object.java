package types.advanced;

import types.datatypes.Vector3;
import exec.file.MaterialParser;
import exec.file.ObjectParser;
import exec.renderer.World;
import types.visuals.Texture;

import java.awt.Color;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Object {

    private final World world;
    private final ObjectParser.Data data;

    int[] worldIndices;
    private double[] center;
    private boolean generated = false;

    public double x = 0;
    public double y = 0;
    public double z = 0;

    public double scaleX = 1;
    public double scaleY = 1;
    public double scaleZ = 1;

    public double rotationX = 0;
    public double rotationY = 0;
    public double rotationZ = 0;

    public boolean centerAtOrigin = true;
    public boolean generateEdges = true;
    public boolean opaque = true;

    public Color vertexColor = Color.WHITE;
    public Color edgeColor = Color.BLACK;

    public int vertexRadius = 1;
    public float edgeThickness = 1;

    // Default texture if a face has no usable material texture.
    public Texture texture;

    // All materials loaded from the MTL exec.file.
    public Map<String, MaterialParser.Material> materials =
            new LinkedHashMap<>();

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

    public void generate() throws IOException {

        int triangleCount = 0;

        int vertexCount = data.vertices.size();

        if (generated) {
            updateTransform();
            return;
        }

        worldIndices = new int[vertexCount];
        center = calculateCenter();

        System.out.println("OBJ vertices: " + vertexCount);
        System.out.println("OBJ faces: " + data.faces.size());
        System.out.println("OBJ explicit edges: " + data.edges.size());
        System.out.println("UV coordinates: " + data.uvs.size());

        System.out.println("Materials loaded: " + materials.keySet());

        for (ObjectParser.Face face : data.faces) {
            System.out.println(
                    "Face vertices: " + face.vertices.size()
                            + ", material: " + face.materialName
            );
        }

        double[] center = calculateCenter();

        // Generate transformed vertices.
        for (int i = 0; i < vertexCount; i++) {
            double[] v = data.vertices.get(i);

            double[] p = transform(
                    v[0] - center[0],
                    v[1] - center[1],
                    v[2] - center[2]
            );

            worldIndices[i] = world.nextVertexIndex();

            world.storeVertex(
                    new Vector3(
                            p[0] + x,
                            p[1] + y,
                            p[2] + z
                    ),
                    vertexColor,
                    vertexRadius,
                    opaque
            );
        }

        Set<Long> edges = new HashSet<>();

        // Generate triangles and collect face edges.
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

                // Use the material's diffuse color as a solid texture.
                faceTexture = new Texture(material.diffuseColor);

                // An image texture takes priority when one is available.
                if (material.diffuseMap != null
                        && !material.diffuseMap.isBlank()) {

                    faceTexture = world.rendererPanel.textureManager
                            .loadMaterialTexture(material.diffuseMap);
                }
            }

            ObjectParser.FaceVertex first = face.vertices.get(0);

            for (int i = 1; i < face.vertices.size() - 1; i++) {

                ObjectParser.FaceVertex b = face.vertices.get(i);
                ObjectParser.FaceVertex c = face.vertices.get(i + 1);

                double[] uv0 = getUV(first.uvIndex);
                double[] uv1 = getUV(b.uvIndex);
                double[] uv2 = getUV(c.uvIndex);

                world.storeTriangles(
                        worldIndices[first.vertexIndex],
                        worldIndices[b.vertexIndex],
                        worldIndices[c.vertexIndex],
                        faceTexture,
                        faceOpaque,
                        uv0[0], uv0[1],
                        uv1[0], uv1[1],
                        uv2[0], uv2[1]
                );

                triangleCount++;
            }
            System.out.println("Triangles submitted to World: " + triangleCount);

            if (generateEdges) {
                for (int i = 0; i < face.vertices.size(); i++) {
                    int a = face.vertices.get(i).vertexIndex;

                    int b = face.vertices.get(
                            (i + 1) % face.vertices.size()
                    ).vertexIndex;

                    edges.add(edgeKey(a, b));
                }
            }
        }

        // Include explicit line geometry from the OBJ.
        if (generateEdges) {
            for (ObjectParser.Edge edge : data.edges) {
                edges.add(edgeKey(edge.vertex1, edge.vertex2));
            }
        }

        // Store unique edges.
        for (long edge : edges) {
            int a = (int) (edge >> 32);
            int b = (int) edge;

            world.storeLine(
                    worldIndices[a],
                    worldIndices[b],
                    edgeColor,
                    edgeThickness,
                    opaque
            );
        }

        System.out.println("Generated model vertices: " + vertexCount);
        System.out.println("Generated model triangles: " + triangleCount);
        System.out.println("Generated unique edges: " + edges.size());

        generated = true;
    }


    public void updateTransform() {
        if (!generated) {
            return;
        }

        for (int i = 0; i < data.vertices.size(); i++) {
            double[] v = data.vertices.get(i);

            double[] p = transform(
                    v[0] - center[0],
                    v[1] - center[1],
                    v[2] - center[2]
            );

            world.updateVertex(
                    worldIndices[i],
                    new Vector3(
                            p[0] + x,
                            p[1] + y,
                            p[2] + z
                    ),
                    vertexColor,
                    vertexRadius,
                    opaque
            );
        }
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

        return new double[]{
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0
        };
    }

    private double[] transform(double x, double y, double z) {

        x *= scaleX;
        y *= scaleY;
        z *= scaleZ;

        // Rotate around X.
        double a = Math.toRadians(rotationX);

        double ny = y * Math.cos(a) - z * Math.sin(a);
        double nz = y * Math.sin(a) + z * Math.cos(a);

        y = ny;
        z = nz;

        // Rotate around Y.
        a = Math.toRadians(rotationY);

        double nx = x * Math.cos(a) + z * Math.sin(a);
        nz = -x * Math.sin(a) + z * Math.cos(a);

        x = nx;
        z = nz;

        // Rotate around Z.
        a = Math.toRadians(rotationZ);

        nx = x * Math.cos(a) - y * Math.sin(a);
        ny = x * Math.sin(a) + y * Math.cos(a);

        return new double[]{nx, ny, z};
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
}