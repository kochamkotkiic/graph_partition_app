package org.example;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            // Tutaj dodaj kod zmieniający ikonę
            ImageIcon icon = new ImageIcon("src/images/kitty.jpg");
            frame.setIconImage(icon.getImage());
            // Koniec kodu ikony
            frame.setVisible(true);
        });
    }
}