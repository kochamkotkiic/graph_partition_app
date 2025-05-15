package org.example;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Create frame
        JFrame frame = new JFrame("Graph Partitioning");

        // Create MainUI instance
        MainUI form = new MainUI();
        JPanel panel = form.getPanel();
        
        // Verify panel is not null before setting
        if (panel == null) {
            // Fallback to a default panel if MainUI.getPanel() returns null
            panel = new JPanel();
            panel.add(new JLabel("Error: Could not load main panel"));
        }
        
        frame.setContentPane(panel);

        // Frame settings
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
    }
}