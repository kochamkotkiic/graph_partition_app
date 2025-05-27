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
import java.util.ArrayList;

public class GraphPrePartitionPanel extends JPanel {
    private Graph graph;
    private Map<Integer, Point2D> vertexPositions = new HashMap<>();
    private boolean layoutGenerated = false;
    private double zoomLevel = 1.0;
    private Point2D panOffset = new Point2D.Double(0, 0);
    private Point lastMousePos;

    // Kolory i rozmiary
    private static final Color VERTEX_COLOR = new Color(54, 128, 210); // SteelBlue
    private static final Color EDGE_COLOR = new Color(206, 203, 203);
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
        int vertexCount = graph.getNumVertices();

        // Zwiększ obszar roboczy w zależności od rozmiaru grafu
        if (vertexCount > 30000) {
            width *= 8;
            height *= 4;  // Mniejszy mnożnik dla wysokości aby zachować proporcje prostokąta
        } else if (vertexCount > 5000) {
            width *= 6;
            height *= 3;  // Podobnie tutaj
        }

        if (vertexCount > 1000) {
            generateRectangularLayout(width, height, vertexCount);
            
            // Stosuj odpowiednie rozpraszanie w zależności od rozmiaru
            if (vertexCount > 30000) {
                applyLargeGraphSpacing(width, height, vertexCount);
            } else if (vertexCount > 5000) {
                applyMediumGraphSpacing(width, height, vertexCount);
            }
        } else {
            generateCircularLayout(width, height, vertexCount);
        }

        applyForceDirectedLayout(width, height, vertexCount);
        centerGraph(width, height);
    }

    private void generateRectangularLayout(int width, int height, int vertexCount) {
        double scaleFactor;
        if (vertexCount > 30000) {
            scaleFactor = Math.sqrt(vertexCount / 1000.0) * 6.0;
        } else if (vertexCount > 5000) {
            scaleFactor = Math.sqrt(vertexCount / 1000.0) * 4.0;
        } else {
            scaleFactor = Math.sqrt(vertexCount / 1000.0) * 2.0;
        }
        
        // Zachowaj proporcje prostokąta
        double usableWidth = width * scaleFactor;
        double usableHeight = height * (scaleFactor / 2.0); // Połowa dla efektu prostokąta
        
        // Oblicz wymiary siatki zachowując proporcje prostokąta
        double aspectRatio = usableWidth / usableHeight;
        int cols = (int) Math.ceil(Math.sqrt(vertexCount * aspectRatio));
        int rows = (int) Math.ceil((double) vertexCount / cols);
        
        double cellWidth = usableWidth / cols;
        double cellHeight = usableHeight / rows;
        
        Random rand = new Random(42);
        
        for (int i = 0; i < vertexCount; i++) {
            int row = i / cols;
            int col = i % cols;
        
            // Większe losowe przesunięcie w poziomie
            double randomOffsetX = (rand.nextDouble() - 0.5) * cellWidth * 
                (vertexCount > 5000 ? 1.2 : 0.8);
            double randomOffsetY = (rand.nextDouble() - 0.5) * cellHeight * 
                (vertexCount > 5000 ? 0.8 : 0.8);
        
            double x = col * cellWidth + cellWidth / 2 + randomOffsetX;
            double y = row * cellHeight + cellHeight / 2 + randomOffsetY;
        
            vertexPositions.put(i, new Point2D.Double(x, y));
        }
    }

    private void generateCircularLayout(int width, int height, int vertexCount) {
        // Oryginalny układ kołowy dla mniejszych grafów
        double radius = Math.min(width, height) * 0.35;
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        for (int i = 0; i < vertexCount; i++) {
            double angle = 2 * Math.PI * i / vertexCount;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            vertexPositions.put(i, new Point2D.Double(x, y));
        }
    }

    private void applyForceDirectedLayout(int width, int height, int vertexCount) {
        // Dostosowane parametry dla różnych rozmiarów grafów
        double k = Math.sqrt((width * height) / vertexCount);
        int iterations;
        double temperature;
        int skipFactor;

        if (vertexCount > 20000) {
            iterations = Math.min(15, Math.max(5, 100 / (int)Math.sqrt(vertexCount)));
            temperature = width / 12.0;
            skipFactor = Math.max(5, vertexCount / 200);
        } else if (vertexCount > 5000) {
            iterations = Math.min(25, Math.max(10, 200 / (int)Math.sqrt(vertexCount)));
            temperature = width / 8.0;
            skipFactor = Math.max(3, vertexCount / 300);
        } else {
            iterations = Math.min(60, Math.max(30, 300 / (int)Math.sqrt(vertexCount)));
            temperature = width / 6.0;
            skipFactor = Math.max(1, vertexCount / 300);
        }

        double coolingFactor = 0.92;
        double repulsionStrength = k * (vertexCount > 10000 ? 2.0 : 1.2);
        double attractionStrength = 1.0 / (k * (vertexCount > 10000 ? 4.0 : 2.5));

        // Force-directed layout
        for (int iter = 0; iter < iterations; iter++) {
            Map<Integer, Point2D.Double> forces = new HashMap<>();
            for (int i = 0; i < vertexCount; i++) {
                forces.put(i, new Point2D.Double(0, 0));
            }

            // Siły odpychania z większym skipFactor dla dużych grafów
            for (int i = 0; i < vertexCount; i += skipFactor) {
                Point2D v1 = vertexPositions.get(i);
                for (int j = i + skipFactor; j < vertexCount; j += skipFactor) {
                    Point2D v2 = vertexPositions.get(j);
                    double dx = v1.getX() - v2.getX();
                    double dy = v1.getY() - v2.getY();
                    double distSq = dx * dx + dy * dy;
                    double dist = Math.sqrt(distSq);

                    if (dist < 0.1) dist = 0.1;
                    double force = repulsionStrength / dist;

                    dx = (dx / dist) * force;
                    dy = (dy / dist) * force;

                    forces.get(i).x += dx;
                    forces.get(i).y += dy;
                    forces.get(j).x -= dx;
                    forces.get(j).y -= dy;
                }
            }

            // Siły przyciągania tylko dla krawędzi (z ograniczeniem dla dużych grafów)
            List<Integer>[] neighbors = graph.getNeighbors();
            for (int i = 0; i < vertexCount; i++) {
                Point2D v1 = vertexPositions.get(i);
                for (int neighbor : neighbors[i]) {
                    if (neighbor > i) {
                        Point2D v2 = vertexPositions.get(neighbor);
                        double dx = v1.getX() - v2.getX();
                        double dy = v1.getY() - v2.getY();
                        double dist = Math.sqrt(dx * dx + dy * dy);

                        if (dist > 0.1) {
                            dx *= attractionStrength;
                            dy *= attractionStrength;

                            forces.get(i).x -= dx;
                            forces.get(i).y -= dy;
                            forces.get(neighbor).x += dx;
                            forces.get(neighbor).y += dy;
                        }
                    }
                }
            }

            // Zastosuj siły z ograniczeniem
            double maxMove = temperature * 2;
            for (int i = 0; i < vertexCount; i++) {
                Point2D pos = vertexPositions.get(i);
                Point2D.Double force = forces.get(i);

                // Ograniczenie maksymalnego ruchu
                double magnitude = Math.sqrt(force.x * force.x + force.y * force.y);
                if (magnitude > maxMove) {
                    force.x = (force.x / magnitude) * maxMove;
                    force.y = (force.y / magnitude) * maxMove;
                }

                // Zachowaj w granicach z większym marginesem
                double margin = vertexCount > 10000 ? 0.05 : 0.1;
                double newX = Math.min(Math.max(width * margin, pos.getX() + force.x), width * (1 - margin));
                double newY = Math.min(Math.max(height * margin, pos.getY() + force.y), height * (1 - margin));
                pos.setLocation(newX, newY);
            }

            temperature *= coolingFactor;
        }
    }

    private void applyAdditionalSpacing(int width, int height, int vertexCount) {
        // Dodatkowe rozproszenie dla bardzo dużych grafów
        Map<Integer, Point2D.Double> adjustments = new HashMap<>();
        for (int i = 0; i < vertexCount; i++) {
            adjustments.put(i, new Point2D.Double(0, 0));
        }

        // Znajdź obszary o dużej gęstości i je rozproś
        double gridSize = Math.min(width, height) / 20.0;
        Map<String, Integer> densityMap = new HashMap<>();

        // Policz gęstość w siatce
        for (Point2D pos : vertexPositions.values()) {
            int gridX = (int) (pos.getX() / gridSize);
            int gridY = (int) (pos.getY() / gridSize);
            String key = gridX + "," + gridY;
            densityMap.put(key, densityMap.getOrDefault(key, 0) + 1);
        }

        // Rozproś wierzchołki w gęstych obszarach
        Random rand = new Random(123);
        for (int i = 0; i < vertexCount; i++) {
            Point2D pos = vertexPositions.get(i);
            int gridX = (int) (pos.getX() / gridSize);
            int gridY = (int) (pos.getY() / gridSize);
            String key = gridX + "," + gridY;

            int density = densityMap.getOrDefault(key, 0);
            if (density > 5) { // Jeśli obszar jest gęsty
                double spreadForce = Math.min(50, density * 2);
                double angle = rand.nextDouble() * 2 * Math.PI;
                double dx = Math.cos(angle) * spreadForce;
                double dy = Math.sin(angle) * spreadForce;

                double newX = Math.min(Math.max(width * 0.05, pos.getX() + dx), width * 0.95);
                double newY = Math.min(Math.max(height * 0.05, pos.getY() + dy), height * 0.95);
                pos.setLocation(newX, newY);
            }
        }
    }

    private void applyLargeGraphSpacing(int width, int height, int vertexCount) {
        // Podziel przestrzeń na sektory
        int sectorsX = (int) Math.sqrt(vertexCount / 100);
        int sectorsY = sectorsX;
        
        double sectorWidth = width / (double) sectorsX;
        double sectorHeight = height / (double) sectorsY;
        
        // Przydziel wierzchołki do sektorów
        Map<Integer, List<Integer>> sectors = new HashMap<>();
        Random rand = new Random(42);
        
        for (int i = 0; i < vertexCount; i++) {
            int sector = rand.nextInt(sectorsX * sectorsY);
            sectors.computeIfAbsent(sector, k -> new ArrayList<>()).add(i);
        }
        
        // Rozłóż wierzchołki w sektorach
        for (Map.Entry<Integer, List<Integer>> entry : sectors.entrySet()) {
            int sector = entry.getKey();
            List<Integer> vertices = entry.getValue();
            
            int sectorX = sector % sectorsX;
            int sectorY = sector / sectorsX;
            
            double startX = sectorX * sectorWidth;
            double startY = sectorY * sectorHeight;
            
            // Rozłóż wierzchołki w sektorze
            for (int i = 0; i < vertices.size(); i++) {
                int vertex = vertices.get(i);
                
                double offsetX = rand.nextDouble() * sectorWidth * 0.8 + sectorWidth * 0.1;
                double offsetY = rand.nextDouble() * sectorHeight * 0.8 + sectorHeight * 0.1;
                
                vertexPositions.get(vertex).setLocation(
                    startX + offsetX,
                    startY + offsetY
                );
            }
        }
    }

    private void centerGraph(int width, int height) {
        // Znajdź środek grafu
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (Point2D pos : vertexPositions.values()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
        }

        double offsetX = (width - (maxX + minX)) / 2;
        double offsetY = (height - (maxY + minY)) / 2;

        // Przesuń wszystkie wierzchołki
        for (Point2D pos : vertexPositions.values()) {
            pos.setLocation(pos.getX() + offsetX, pos.getY() + offsetY);
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

        // Dla bardzo dużych grafów rysuj mniej krawędzi przy małym zoomie
        if (graph.getNumVertices() > 10000 && zoomLevel < 0.3) {
            drawEdgesSimplified(g2d);
        } else {
            drawEdges(g2d);
        }

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

    private void drawEdgesSimplified(Graphics2D g2d) {
        // Dla dużych grafów przy małym zoomie rysuj co n-tą krawędź
        g2d.setColor(new Color(200, 200, 200, 128)); // Półprzezroczyste krawędzie
        List<Integer>[] neighbors = graph.getNeighbors();
        int skipFactor = Math.max(1, (int)(graph.getNumVertices() / 5000));

        for (int i = 0; i < neighbors.length; i += skipFactor) {
            Point2D p1 = vertexPositions.get(i);
            if (p1 == null) continue;

            for (int neighbor : neighbors[i]) {
                if (neighbor % skipFactor == 0) {
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
    }

    private void drawVertices(Graphics2D g2d) {
        int vertexSize = (int) (BASE_VERTEX_SIZE / Math.sqrt(zoomLevel));
        vertexSize = Math.max(1, Math.min(vertexSize, 15)); // Ogranicz rozmiar

        // Dla bardzo dużych grafów przy małym zoomie rysuj mniejsze wierzchołki
        if (graph.getNumVertices() > 20000 && zoomLevel < 0.5) {
            vertexSize = Math.max(1, vertexSize / 2);
        }

        g2d.setColor(VERTEX_COLOR);

        // Rysuj wierzchołki z numerami jeśli zoom jest wystarczający
        boolean showNumbers = zoomLevel > 0.8 && graph.getNumVertices() < 1000;

        if (showNumbers) {
            g2d.setFont(g2d.getFont().deriveFont((float)(8 / Math.sqrt(zoomLevel))));
        }

        for (Map.Entry<Integer, Point2D> entry : vertexPositions.entrySet()) {
            Point2D pos = entry.getValue();
            Integer vertexId = entry.getKey();

            if (pos != null) {
                // Rysuj wierzchołek
                g2d.fillOval(
                        (int) (pos.getX() - vertexSize/2),
                        (int) (pos.getY() - vertexSize/2),
                        vertexSize, vertexSize
                );

                // Rysuj numer wierzchołka jeśli zoom pozwala
                if (showNumbers) {
                    g2d.setColor(Color.BLACK);
                    String label = String.valueOf(vertexId);
                    g2d.drawString(label,
                            (int) (pos.getX() + vertexSize/2 + 2),
                            (int) (pos.getY() + vertexSize/2));
                    g2d.setColor(VERTEX_COLOR);
                }
            }
        }
    }

private void applyMediumGraphSpacing(int width, int height, int vertexCount) {
    // Podziel przestrzeń na sektory z zachowaniem proporcji prostokąta
    int sectorsX = (int) Math.sqrt(vertexCount / 50);  // Więcej kolumn niż wierszy
    int sectorsY = sectorsX / 2;  // Połowa wierszy dla efektu prostokąta
    
    double sectorWidth = width / (double) sectorsX;
    double sectorHeight = height / (double) sectorsY;
    
    // Przydziel wierzchołki do sektorów
    Map<Integer, List<Integer>> sectors = new HashMap<>();
    Random rand = new Random(42);
    
    for (int i = 0; i < vertexCount; i++) {
        int sector = rand.nextInt(sectorsX * sectorsY);
        sectors.computeIfAbsent(sector, k -> new ArrayList<>()).add(i);
    }
    
    // Rozłóż wierzchołki w sektorach
    for (Map.Entry<Integer, List<Integer>> entry : sectors.entrySet()) {
        int sector = entry.getKey();
        List<Integer> vertices = entry.getValue();
        
        int sectorX = sector % sectorsX;
        int sectorY = sector / sectorsX;
        
        double startX = sectorX * sectorWidth;
        double startY = sectorY * sectorHeight;
        
        // Rozłóż wierzchołki w sektorze z większym rozproszeniem w poziomie
        for (int i = 0; i < vertices.size(); i++) {
            int vertex = vertices.get(i);
            
            // Większe rozproszenie w poziomie
            double offsetX = rand.nextDouble() * sectorWidth * 0.9 + sectorWidth * 0.05;
            double offsetY = rand.nextDouble() * sectorHeight * 0.7 + sectorHeight * 0.15;
            
            vertexPositions.get(vertex).setLocation(
                startX + offsetX,
                startY + offsetY
            );
        }
    }
}
}