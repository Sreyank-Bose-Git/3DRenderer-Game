package main.interfaces;

import main.RendererPanel;
import main.interfaces.ingame.SoundInterface;
import main.interfaces.ingame.UserInterface;

import java.awt.*;

public class Interface {

    RendererPanel rendererPanel;
    public String prevGameState;

    public UserInterface userInterface;
    public SoundInterface soundInterface;

    public Interface(RendererPanel rendererPanel) {
        this.rendererPanel = rendererPanel;
        prevGameState = "";

        userInterface = new UserInterface(rendererPanel);
        soundInterface = new SoundInterface(rendererPanel);
    }

    public void update(){

        userInterface.update();
        soundInterface.update();
    }

    public void draw(Graphics2D g2D) {

        userInterface.draw(g2D);
    }
}
