package visuals;

import visuals.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TextureManager {

    public Texture[] textures;

    // Textures loaded from MTL files, indexed by resource path.
    public Map<String, Texture> materialTextures = new HashMap<>();

    public TextureManager(int tSize) {
        textures = new Texture[tSize];
        createTextures();
    }

    void createTextures() {
        try {
            textures[0] = new Texture(
                    ImageIO.read(
                            Objects.requireNonNull(
                                    getClass().getResourceAsStream(
                                            "/textures/blocks/grass.png"
                                    )
                            )
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Texture loadMaterialTexture(String path)
            throws IOException {

        if (path == null || path.isBlank()) {
            return null;
        }

        // Reuse a texture if it has already been loaded.
        if (materialTextures.containsKey(path)) {
            return materialTextures.get(path);
        }

        InputStream stream = getClass().getResourceAsStream(path);

        if (stream == null) {
            throw new IOException(
                    "Texture resource not found: " + path
            );
        }

        try (stream) {
            BufferedImage image = ImageIO.read(stream);

            if (image == null) {
                throw new IOException(
                        "Unsupported or invalid image: " + path
                );
            }

            Texture loadedTexture = new Texture(image);

            materialTextures.put(path, loadedTexture);

            return loadedTexture;
        }
    }
}