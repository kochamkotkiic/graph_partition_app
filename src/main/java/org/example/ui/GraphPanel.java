package org.example.ui;

import org.example.model.Graph;

import javax.swing.*;
import java.awt.*;

public class GraphPanel extends JPanel {
    private Graph graph;

    public void setGraph(Graph graph) {
        this.graph = graph;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graph == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // TODO: Rysuj wierzchołki i krawędzie jak wcześniej pokazałam
    }
}
