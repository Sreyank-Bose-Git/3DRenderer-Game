package extras;

import javax.sound.sampled.*;
import java.net.URL;

public class AudioManager {

    public URL[] audioURLs;
    Clip clip;
    AudioInputStream audioInputStream;

    public AudioManager(int aSize) {

        audioURLs = new URL[aSize];

        setupAudio();
    }

    void setupAudio() {

        audioURLs[0] = getClass().getResource("/audio/music/Background.wav");
    }

    public void playAudio(int index) {

        if(audioURLs[index] == null) {
            System.err.println("Audio file not found: " + index);
            return;
        }

        try {
            audioInputStream = AudioSystem.getAudioInputStream(audioURLs[index]);

            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAndLoopAudio(int index) {

        if(audioURLs[index] == null) {
            System.err.println("Audio file not found: " + index);
            return;
        }

        try {
            audioInputStream = AudioSystem.getAudioInputStream(audioURLs[index]);

            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAudio() {
        clip.stop();
    }
}
