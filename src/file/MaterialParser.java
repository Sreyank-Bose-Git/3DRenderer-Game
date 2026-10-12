package file;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MaterialParser {

    public static class Material {
        public String name;

        public Color diffuseColor = Color.WHITE;
        public Color ambientColor = Color.BLACK;
        public Color specularColor = Color.BLACK;

        public double shininess = 0;
        public double opacity = 1.0;

        public String diffuseMap;

        public Material(String name) {
            this.name = name;
        }
    }

    public Map<String, Material> parse(String resourcePath)
            throws IOException {

        Map<String, Material> materials = new LinkedHashMap<>();

        try (InputStream stream =
                     MaterialParser.class.getResourceAsStream(resourcePath)) {

            if (stream == null) {
                throw new IOException(
                        "MTL resource not found: " + resourcePath
                );
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                Material current = null;
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    String[] parts = line.split("\\s+", 2);
                    String command = parts[0];

                    String value = parts.length > 1
                            ? parts[1].trim()
                            : "";

                    switch (command) {

                        case "newmtl":
                            current = new Material(value);
                            materials.put(value, current);
                            break;

                        case "Kd":
                            if (current != null) {
                                current.diffuseColor = parseColor(value);
                            }
                            break;

                        case "Ka":
                            if (current != null) {
                                current.ambientColor = parseColor(value);
                            }
                            break;

                        case "Ks":
                            if (current != null) {
                                current.specularColor = parseColor(value);
                            }
                            break;

                        case "Ns":
                            if (current != null) {
                                current.shininess =
                                        Double.parseDouble(value);
                            }
                            break;

                        case "d":
                            if (current != null) {
                                current.opacity = clamp(
                                        Double.parseDouble(value)
                                );
                            }
                            break;

                        case "Tr":
                            if (current != null) {
                                current.opacity = 1.0 - clamp(
                                        Double.parseDouble(value)
                                );
                            }
                            break;

                        case "map_Kd":
                            if (current != null) {
                                current.diffuseMap =
                                        resolveResourcePath(
                                                resourcePath, value
                                        );
                            }
                            break;
                    }
                }
            }
        }

        return materials;
    }

    private Color parseColor(String value) {
        String[] parts = value.split("\\s+");

        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Invalid MTL color: " + value
            );
        }

        return new Color(
                toColorChannel(Double.parseDouble(parts[0])),
                toColorChannel(Double.parseDouble(parts[1])),
                toColorChannel(Double.parseDouble(parts[2]))
        );
    }

    private int toColorChannel(double value) {
        return (int) Math.round(clamp(value) * 255.0);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String resolveResourcePath(
            String mtlPath, String texturePath) {

        if (texturePath.startsWith("/")) {
            return texturePath;
        }

        int lastSlash = mtlPath.lastIndexOf('/');

        String directory = lastSlash >= 0
                ? mtlPath.substring(0, lastSlash + 1)
                : "/";

        return directory + texturePath;
    }
}