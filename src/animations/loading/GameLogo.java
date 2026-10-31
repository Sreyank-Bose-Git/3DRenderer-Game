
package animations.loading;

import main.RendererPanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class GameLogo {

    private BufferedImage logoImage;

    private float alpha = 0;
    private boolean finished = false;

    private final float duration = 2.0f;
    private long startTime;

    public GameLogo() {
        try (InputStream stream = GameLogo.class.getResourceAsStream(
                "/images/brand/OpenStreets.png")) {

            if (stream == null) {
                throw new IllegalStateException("Logo image not found.");
            }

            logoImage = ImageIO.read(stream);

        } catch (Exception e) {
            e.printStackTrace();
            finished = true;
        }
    }

    public void update() {

        if (finished) return;

        if (startTime == 0) {
            startTime = System.nanoTime();
        }

        float elapsed = (System.nanoTime() - startTime) / 1_000_000_000f;

        alpha = Math.min(elapsed / duration, 1.0f);

        if (alpha >= 1.0f) {
            finished = true;
        }
    }

    public void draw(Graphics2D g2D, RendererPanel rendererPanel) {

        if (logoImage == null) return;

        Graphics2D g = (Graphics2D) g2D.create();

        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha));

        int size = 400;

        int x = (rendererPanel.getWidth() - size) / 2;
        int y = (rendererPanel.getHeight() - size) / 2;

        g.drawImage(logoImage, x, y, size, size, null);

        g.dispose();
    }

    public boolean isFinished() {
        return finished;
    }
}
