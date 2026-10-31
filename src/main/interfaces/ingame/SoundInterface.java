package main.interfaces.ingame;

import main.RendererPanel;

public class SoundInterface {

    RendererPanel rendererPanel;

    public SoundInterface(RendererPanel rendererPanel) {

        this.rendererPanel = rendererPanel;
    }

    public void update() {
        if(!rendererPanel.gameState.equals(rendererPanel.myInterface.prevGameState)) {
            if(rendererPanel.audioManager.clip != null) {
                rendererPanel.audioManager.stopAudio();
            }
            if(rendererPanel.gameState.equals("home")) {
                rendererPanel.keyHandler.ePressed = true;
                rendererPanel.audioManager.playAndLoopAudio(0);
            }
            else if(rendererPanel.gameState.equals("play")) {
                rendererPanel.keyHandler.ePressed = false;
                rendererPanel.audioManager.playAndLoopAudio(0);
            }
            rendererPanel.myInterface.prevGameState = rendererPanel.gameState;
        }
    }
}
