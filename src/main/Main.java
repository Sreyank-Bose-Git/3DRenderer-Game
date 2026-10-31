package main;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        JFrame rendererWindow = new JFrame();

        rendererWindow.setTitle("OpenStreets");
        rendererWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        RendererPanel rendererPanel = new RendererPanel();
        rendererWindow.add(rendererPanel);

        rendererWindow.pack();
        rendererWindow.setResizable(false);
        rendererWindow.setLocationRelativeTo(null);
        rendererWindow.setVisible(true);
    }
}
