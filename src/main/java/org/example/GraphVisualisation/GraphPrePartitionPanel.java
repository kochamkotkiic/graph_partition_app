package org.example.GraphVisualisation;
import org.example.model.Graph;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GraphPrePartitionPanel extends JPanel {
    private Graph graph;
    private Map<Integer, Point2D> vertexPositions = new HashMap<>();
    private boolean layoutGenerated = false;
    private double zoomLevel = 1.0;
    private Point2D panOffset = new Point2D.Double(0, 0);
    private Point lastMousePos;

    // Kolory i rozmiary
    private static final Color VERTEX_COLOR = new Color(70, 130, 180); // SteelBlue
    private static final Color EDGE_COLOR = new Color(200, 200, 200);
    private static final int BASE_VERTEX_SIZE = 5;

    public GraphPrePartitionPanel() {
        setBackground(Color.WHITE);
        setupMouseListeners();
    }

    public void setGraph(Graph graph) {
        if (this.graph != null && graph != null && 
            this.graph.getNumVertices() == graph.getNumVertices()) {
            // Zachowaj stare pozycje
        } else {
            // Wyczyść pozycje tylko jeśli zmienił się graf
            vertexPositions.clear();
            layoutGenerated = false;
        }
        
        this.graph = graph;
        
        // Generuj layout od razu przy ustawianiu grafu
        if (!layoutGenerated) {
            generateLayout();
            layoutGenerated = true;
        }
        
        resetView();
        revalidate();
        repaint();
    }

    private void generateLayout() {
        if (graph == null) return;

        int width = getWidth() > 0 ? getWidth() : 800;
        int height = getHeight() > 0 ? getHeight() : 600;

        // Inicjalizacja losowych pozycji
        Random random = new Random();
        int vertexCount = graph.getNumVertices();
        for (int i = 0; i < vertexCount; i++) {
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * height;
            vertexPositions.put(i, new Point2D.Double(x, y));
        }

        // Zoptymalizowane parametry
        double k = Math.sqrt((width * height) / vertexCount);
        // Zmniejszamy liczbę iteracji dla większych grafów
        int iterations = Math.min(50, Math.max(20, 200 / (int)Math.sqrt(vertexCount)));
        double temperature = width / 8.0;
        double coolingFactor = 0.9;

        // Force-directed layout
        for (int iter = 0; iter < iterations; iter++) {
            Map<Integer, Point2D.Double> forces = new HashMap<>();
            for (int i = 0; i < vertexCount; i++) {
                forces.put(i, new Point2D.Double(0, 0));
            }

            // Optymalizacja sił odpychania - używamy mniejszej liczby par dla większych grafów
            int skipFactor = Math.max(1, vertexCount / 500); // Proporcjonalne pomijanie
            for (int i = 0; i < vertexCount; i += skipFactor) {
                Point2D v1 = vertexPositions.get(i);
                for (int j = i + skipFactor; j < vertexCount; j += skipFactor) {
                    Point2D v2 = vertexPositions.get(j);
                    double dx = v1.getX() - v2.getX();
                    double dy = v1.getY() - v2.getY();
                    double distance = Math.max(0.1, Math.sqrt(dx * dx + dy * dy));
                    double force = k * k / distance;
                
                    dx = (dx / distance) * force;
                    dy = (dy / distance) * force;

                    forces.get(i).x += dx;
                    forces.get(i).y += dy;
                    forces.get(j).x -= dx;
                    forces.get(j).y -= dy;
                }
            }

            // Siły przyciągania - tylko dla rzeczywistych krawędzi
            List<Integer>[] neighbors = graph.getNeighbors();
            for (int i = 0; i < vertexCount; i++) {
                Point2D v1 = vertexPositions.get(i);
                for (int neighbor : neighbors[i]) {
                    if (neighbor > i) {
                        Point2D v2 = vertexPositions.get(neighbor);
                        double dx = v1.getX() - v2.getX();
                        double dy = v1.getY() - v2.getY();
                        double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    dx = (dx / distance) * distance * distance / (k * 3); // Zmniejszone siły przyciągania
                    dy = (dy / distance) * distance * distance / (k * 3);

                    forces.get(i).x -= dx;
                    forces.get(i).y -= dy;
                    forces.get(neighbor).x += dx;
                    forces.get(neighbor).y += dy;
                }
            }
        }

        // Zastosuj siły z ograniczeniem
        for (int i = 0; i < vertexCount; i++) {
            Point2D pos = vertexPositions.get(i);
            Point2D.Double force = forces.get(i);
            double dx = Math.min(Math.max(-temperature, force.x), temperature);
            double dy = Math.min(Math.max(-temperature, force.y), temperature);
            
            double newX = Math.min(Math.max(0, pos.getX() + dx), width);
            double newY = Math.min(Math.max(0, pos.getY() + dy), height);
            pos.setLocation(newX, newY);
        }

        temperature *= coolingFactor;
    }
}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graph == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform transform = new AffineTransform();
        transform.scale(zoomLevel, zoomLevel);
        transform.translate(panOffset.getX(), panOffset.getY());
        g2d.transform(transform);

        drawEdges(g2d);
        drawVertices(g2d);
    }

    public Map<Integer, Point2D> getVertexPositions() {
        return new HashMap<>(vertexPositions);
    }

    public void setVertexPositions(Map<Integer, Point2D> positions) {
        if (positions != null) {
            this.vertexPositions = new HashMap<>(positions);
            layoutGenerated = true;
            repaint();
        }
    }

    public void clearVisualization() {
        this.graph = null;
        this.vertexPositions.clear();
        this.layoutGenerated = false;
        resetView();
        revalidate();
        repaint();
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    lastMousePos = e.getPoint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && lastMousePos != null) {
                    Point current = e.getPoint();
                    double dx = (current.x - lastMousePos.x) / zoomLevel;
                    double dy = (current.y - lastMousePos.y) / zoomLevel;

                    panOffset.setLocation(
                            panOffset.getX() + dx,
                            panOffset.getY() + dy
                    );

                    lastMousePos = current;
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            double zoomFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
            zoomLevel *= zoomFactor;
            zoomLevel = Math.max(0.05, Math.min(zoomLevel, 10.0)); // Ogranicz zoom
            repaint();
        });
    }

    public void resetView() {
        zoomLevel = 1.0;
        panOffset.setLocation(0, 0);
        repaint();
    }

    private void drawEdges(Graphics2D g2d) {
        g2d.setColor(EDGE_COLOR);
        List<Integer>[] neighbors = graph.getNeighbors();

        for (int i = 0; i < neighbors.length; i++) {
            Point2D p1 = vertexPositions.get(i);
            if (p1 == null) continue;

            for (int neighbor : neighbors[i]) {
                Point2D p2 = vertexPositions.get(neighbor);
                if (p2 != null) {
                    g2d.drawLine(
                            (int) p1.getX(), (int) p1.getY(),
                            (int) p2.getX(), (int) p2.getY()
                    );
                }
            }
        }
    }

    private void drawVertices(Graphics2D g2d) {
        int vertexSize = (int) (BASE_VERTEX_SIZE / Math.sqrt(zoomLevel));
        vertexSize = Math.max(2, Math.min(vertexSize, 15)); // Ogranicz rozmiar

        g2d.setColor(VERTEX_COLOR);
        for (Point2D pos : vertexPositions.values()) {
            if (pos != null) {
                g2d.fillOval(
                        (int) (pos.getX() - vertexSize/2),
                        (int) (pos.getY() - vertexSize/2),
                        vertexSize, vertexSize
                );
            }
        }
    }
}