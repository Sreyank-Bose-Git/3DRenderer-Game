package main;

import java.awt.*;

public class Interface {

    RendererPanel rendererPanel;
    String prevGameState;

    Font mv_bola_25;
    Font mv_bola_50;
    BasicStroke stroke_5;

    int x, y;

    // home
    int width;
    Color playColor = Color.white;
    Color exitColor = Color.red;

    public Interface(RendererPanel rendererPanel) {
        mv_bola_25 = new Font("MV Bola", Font.BOLD, 25);
        mv_bola_50 = new Font("MV Bola", Font.BOLD, 50);
        stroke_5 = new BasicStroke(5);

        this.rendererPanel = rendererPanel;
        prevGameState = "";
    }

    public void update(){
        if(rendererPanel.gameState.equals("home")) {
            try {
                x = rendererPanel.mouseLocation.x - rendererPanel.getLocationOnScreen().x;
                y = rendererPanel.mouseLocation.y - rendererPanel.getLocationOnScreen().y;

                if(x > (rendererPanel.screenWidth / 2) - 75 && x < (rendererPanel.screenWidth / 2) + 75 && y > 400 && y < 450) {
                    playColor = Color.red;
                    if(rendererPanel.mouseHandler.mousePressed) {
                        rendererPanel.gameState = "play";
                    }
                }
                else {
                    playColor = Color.white;
                }
            } catch(Exception e) {
                System.out.println("MouseLocation: Waiting for JFrame to load...");
            }
        }
        else if(rendererPanel.gameState.equals("play")) {
            x = rendererPanel.mouseLocation.x - rendererPanel.getLocationOnScreen().x;
            y = rendererPanel.mouseLocation.y - rendererPanel.getLocationOnScreen().y;

            if(x > 25 && x < 125 && y > 25 && y < 70) {
                exitColor = Color.white;
                if(rendererPanel.mouseHandler.mousePressed) {
                    rendererPanel.gameState = "home";
                }
            }
            else {
                exitColor = Color.red;
            }
        }

        if(!rendererPanel.gameState.equals(prevGameState)) {
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
            prevGameState = rendererPanel.gameState;
        }
    }

    public void draw(Graphics2D g2D) {
        if(rendererPanel.gameState.equals("home")) {
            // Logo
            g2D.setFont(mv_bola_50);
            g2D.setColor(Color.white);
            width = g2D.getFontMetrics().stringWidth("TriPlex: The 3D Renderer Game");
            g2D.drawString("TriPlex: The 3D Renderer Game", (rendererPanel.screenWidth / 2) - (width / 2), 100);

            // Play Button
            g2D.setColor(Color.green);
            g2D.fillRect((rendererPanel.screenWidth / 2) - 75, 400, 150, 50);
            g2D.setColor(playColor);
            g2D.setStroke(stroke_5);
            g2D.drawRect((rendererPanel.screenWidth / 2) - 75, 400, 150, 50);
            g2D.setFont(mv_bola_25);
            g2D.setColor(Color.white);
            width = g2D.getFontMetrics().stringWidth("Play");
            g2D.drawString("Play", (rendererPanel.screenWidth / 2) - (width / 2), 435);
        }
        else if(rendererPanel.gameState.equals("play")) {

            // Play Button
            g2D.setColor(Color.green);
            g2D.fillRect(25, 25, 100, 45);
            g2D.setColor(exitColor);
            g2D.setStroke(stroke_5);
            g2D.drawRect(25, 25, 100, 45);
            g2D.setFont(mv_bola_25);
            g2D.setColor(Color.white);
            width = g2D.getFontMetrics().stringWidth("Exit");
            g2D.drawString("Exit", (width / 2) + 26, 57);
        }
    }
}
