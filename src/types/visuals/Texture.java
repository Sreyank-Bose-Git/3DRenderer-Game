
package types.visuals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class Texture {

    public final int width;
    public final int height;

    public final Color[] pixels;

    public Texture(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException(
                    "Texture image cannot be null"
            );
        }

        width = image.getWidth();
        height = image.getHeight();

        pixels = new Color[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] =
                        new Color(image.getRGB(x, y), true);
            }
        }
    }

    public Texture(Color color) {
        this.width = 1;
        this.height = 1;
        this.pixels = new Color[]{color};
    }

    public static Texture load(String path)
            throws IOException {

        try (InputStream input =
                     Texture.class.getResourceAsStream(path)) {

            if (input == null) {
                throw new IOException(
                        "Texture not found: " + path
                );
            }

            BufferedImage image = ImageIO.read(input);

            if (image == null) {
                throw new IOException(
                        "Unsupported image format: " + path
                );
            }

            return new Texture(image);
        }
    }

    // UV actualCoordinates range from 0.0 to 1.0.
    public Color sample(double u, double v) {

        u = Math.clamp(u, 0.0, 1.0);
        v = Math.clamp(v, 0.0, 1.0);

        int x = Math.min((int) (u * width), width - 1);
        int y = Math.min((int) (v * height), height - 1);

        return pixels[y * width + x];
    }

    // Repeat the texture outside the 0.0–1.0 range.
    public Color sampleRepeat(double u, double v) {

        u -= Math.floor(u);
        v -= Math.floor(v);

        int x = Math.min((int) (u * width), width - 1);
        int y = Math.min((int) (v * height), height - 1);

        return pixels[y * width + x];
    }
}