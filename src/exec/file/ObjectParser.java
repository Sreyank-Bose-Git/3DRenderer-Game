package exec.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ObjectParser {

    public static class FaceVertex {
        public int vertexIndex;
        public int uvIndex;

        public FaceVertex(int vertexIndex, int uvIndex) {
            this.vertexIndex = vertexIndex;
            this.uvIndex = uvIndex;
        }
    }

    public static class Face {
        public List<FaceVertex> vertices = new ArrayList<>();
        public String materialName;
    }

    public static class Edge {
        public int vertex1;
        public int vertex2;

        public Edge(int vertex1, int vertex2) {
            this.vertex1 = vertex1;
            this.vertex2 = vertex2;
        }
    }

    public static class Data {
        public String materialLibrary;

        public List<double[]> vertices = new ArrayList<>();
        public List<double[]> uvs = new ArrayList<>();
        public List<Face> faces = new ArrayList<>();
        public List<Edge> edges = new ArrayList<>();
    }

    public Data parse(String resourcePath) throws IOException {

        Data data = new Data();
        String currentMaterial = null;
        int lineNumber = 0;

        try (InputStream stream =
                     ObjectParser.class.getResourceAsStream(resourcePath)) {

            if (stream == null) {
                throw new IOException(
                        "OBJ resource not found: " + resourcePath
                );
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                String raw;

                while ((raw = reader.readLine()) != null) {
                    lineNumber++;

                    String line = raw.trim();

                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");

                    try {
                        switch (parts[0]) {

                            case "mtllib":
                                data.materialLibrary =
                                        line.substring("mtllib".length()).trim();
                                break;

                            case "usemtl":
                                currentMaterial =
                                        line.substring("usemtl".length()).trim();
                                break;

                            case "v":
                                if (parts.length < 4) {
                                    throw new IOException("Invalid vertex");
                                }

                                data.vertices.add(new double[]{
                                        Double.parseDouble(parts[1]),
                                        Double.parseDouble(parts[2]),
                                        Double.parseDouble(parts[3])
                                });
                                break;

                            case "vt":
                                if (parts.length < 3) {
                                    throw new IOException(
                                            "Invalid texture coordinate"
                                    );
                                }

                                data.uvs.add(new double[]{
                                        Double.parseDouble(parts[1]),
                                        Double.parseDouble(parts[2])
                                });
                                break;

                            case "f": {
                                Face face = new Face();
                                face.materialName = currentMaterial;

                                for (int i = 1; i < parts.length; i++) {
                                    String[] indices =
                                            parts[i].split("/", -1);

                                    int vertexIndex = resolveIndex(
                                            indices[0],
                                            data.vertices.size()
                                    );

                                    int uvIndex = -1;

                                    if (indices.length > 1
                                            && !indices[1].isEmpty()) {
                                        uvIndex = resolveIndex(
                                                indices[1],
                                                data.uvs.size()
                                        );
                                    }

                                    face.vertices.add(
                                            new FaceVertex(vertexIndex, uvIndex)
                                    );
                                }

                                if (face.vertices.size() < 3) {
                                    throw new IOException(
                                            "Face needs at least 3 vertices"
                                    );
                                }

                                data.faces.add(face);
                                break;
                            }

                            case "l":
                                for (int i = 1; i < parts.length - 1; i++) {
                                    String[] a = parts[i].split("/");
                                    String[] b = parts[i + 1].split("/");

                                    data.edges.add(new Edge(
                                            resolveIndex(
                                                    a[0],
                                                    data.vertices.size()
                                            ),
                                            resolveIndex(
                                                    b[0],
                                                    data.vertices.size()
                                            )
                                    ));
                                }
                                break;

                            default:
                                // Ignore normals, groups, smoothing, etc.
                                break;
                        }

                    } catch (RuntimeException | IOException e) {
                        throw new IOException(
                                "OBJ parsing error at line " + lineNumber
                                        + ": " + line,
                                e
                        );
                    }
                }
            }
        }

        if (data.vertices.isEmpty()) {
            throw new IOException("OBJ resource contains no vertices.");
        }

        return data;
    }

    private int resolveIndex(String value, int size)
            throws IOException {

        int index = Integer.parseInt(value);

        index = index > 0 ? index - 1 : size + index;

        if (index < 0 || index >= size) {
            throw new IOException("Invalid OBJ index: " + value);
        }

        return index;
    }
}