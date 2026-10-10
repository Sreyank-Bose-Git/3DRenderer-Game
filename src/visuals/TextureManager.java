package visuals;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class TextureManager {

    public Texture[] textures;

    public TextureManager(int tSize) {

        textures = new Texture[tSize];

        createTextures();
    }

    void createTextures() {

        try {
            textures[0] = new Texture(ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/textures/blocks/grass.png"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
