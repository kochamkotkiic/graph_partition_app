package org.example.model;
import java.awt.geom.Point2D;
import java.util.*;

public class Graph {
    private int maxVertices;
    private int numVertices;
    private int numComponents;

    // Dynamic adjacency lists
    private List<Integer>[] neighbors;
    private int[] neighborCount;

    // Additional data arrays
    private int[] maxDistances;
    private int[] groupAssignment;
    private int[] component;

    // CSR representation
    private int[] colIndex;
    private int[] rowPtr;
    private int[] groupList;
    private int[] groupPtr;
    private Map<Integer, Point2D> vertexPositions;

    @SuppressWarnings("unchecked")
    public Graph() {
        initGraph(40000); // Default capacity for large graphs
        vertexPositions = new HashMap<>();

    }

    @SuppressWarnings("unchecked")
    public Graph(int initialCapacity) {
        initGraph(initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public Graph(int initialCapacity, boolean exactSize) {
        if (exactSize) {
            initGraph(initialCapacity);
        } else {
            // Add some buffer for growth
            initGraph(Math.max(initialCapacity + initialCapacity / 4, initialCapacity + 1000));
        }
    }
    // Dodaj metody do zarządzania pozycjami:
    public void setVertexPosition(int vertex, Point2D position) {
        vertexPositions.put(vertex, (Point2D) position.clone());
    }

    public Point2D getVertexPosition(int vertex) {
        return vertexPositions.get(vertex);
    }

    public Map<Integer, Point2D> getAllVertexPositions() {
        return new HashMap<>(vertexPositions);
    }

    public void setAllVertexPositions(Map<Integer, Point2D> positions) {
        vertexPositions = new HashMap<>(positions);
    }

    @SuppressWarnings("unchecked")
    public Graph(Graph other) {
        // Copy basic properties
        this.maxVertices = other.maxVertices;
        this.numVertices = other.numVertices;
        this.numComponents = other.numComponents;

        // Initialize arrays with the same capacity as the source
        this.neighbors = new List[other.maxVertices];
        this.neighborCount = new int[other.maxVertices];
        this.maxDistances = new int[other.maxVertices];
        this.groupAssignment = new int[other.maxVertices];
        this.component = new int[other.maxVertices];

        // Deep copy of neighbors lists
        for (int i = 0; i < other.maxVertices; i++) {
            if (other.neighbors[i] != null) {
                this.neighbors[i] = new ArrayList<>(other.neighbors[i]);
            } else {
                this.neighbors[i] = new ArrayList<>();
            }
            this.neighborCount[i] = other.neighborCount[i];
        }

        // Copy arrays
        System.arraycopy(other.maxDistances, 0, this.maxDistances, 0, other.maxVertices);
        System.arraycopy(other.groupAssignment, 0, this.groupAssignment, 0, other.maxVertices);
        System.arraycopy(other.component, 0, this.component, 0, other.maxVertices);

        // Copy CSR representation if exists
        if (other.colIndex != null) {
            this.colIndex = Arrays.copyOf(other.colIndex, other.colIndex.length);
        }
        if (other.rowPtr != null) {
            this.rowPtr = Arrays.copyOf(other.rowPtr, other.rowPtr.length);
        }
        if (other.groupList != null) {
            this.groupList = Arrays.copyOf(other.groupList, other.groupList.length);
        }
        if (other.groupPtr != null) {
            this.groupPtr = Arrays.copyOf(other.groupPtr, other.groupPtr.length);
        }
    }

    @SuppressWarnings("unchecked")
    public void initGraph(int initialCapacity) {
        this.maxVertices = initialCapacity;
        this.numVertices = 0;
        this.numComponents = 0;

        // Initialize CSR pointers to null
        this.colIndex = null;
        this.rowPtr = null;
        this.groupList = null;
        this.groupPtr = null;

        // Allocate dynamic neighbor arrays
        this.neighbors = new List[initialCapacity];
        this.neighborCount = new int[initialCapacity];

        // Allocate additional dynamic arrays
        this.maxDistances = new int[initialCapacity];
        this.groupAssignment = new int[initialCapacity];
        this.component = new int[initialCapacity];

        // Initialize neighbor lists as empty
        for (int i = 0; i < initialCapacity; i++) {
            this.neighbors[i] = new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= maxVertices) {
            return;
        }

        // For large graphs, use more aggressive growth strategy
        // If we need much more space, allocate generously to avoid frequent reallocations
        int newCapacity;
        if (requiredCapacity > maxVertices * 2) {
            // If required capacity is much larger, round up to nearest 10k or use required + 25%
            newCapacity = Math.max(
                    ((requiredCapacity / 10000) + 1) * 10000, // Round up to nearest 10k
                    requiredCapacity + requiredCapacity / 4    // Or +25%
            );
        } else {
            // Standard doubling for smaller increases
            newCapacity = Math.max(maxVertices * 2, requiredCapacity + 1000);
        }

        // Resize neighbors array
        List<Integer>[] newNeighbors = new List[newCapacity];
        System.arraycopy(neighbors, 0, newNeighbors, 0, maxVertices);
        for (int i = maxVertices; i < newCapacity; i++) {
            newNeighbors[i] = new ArrayList<>();
        }
        neighbors = newNeighbors;

        // Resize other arrays
        neighborCount = Arrays.copyOf(neighborCount, newCapacity);
        maxDistances = Arrays.copyOf(maxDistances, newCapacity);
        groupAssignment = Arrays.copyOf(groupAssignment, newCapacity);
        component = Arrays.copyOf(component, newCapacity);

        maxVertices = newCapacity;
    }

    public void addEdge(int u, int v) {
        if (u < 0 || v < 0) {
            throw new GraphException("Błąd: Indeks wierzchołka nie może być ujemny (u=" + u + ", v=" + v + ")");
        }

        // Ensure capacity for both vertices
        int requiredCapacity = Math.max(u, v) + 1;
        ensureCapacity(requiredCapacity);

        // Add v to u's neighbors if not already present
        if (!neighbors[u].contains(v)) {
            neighbors[u].add(v);
            neighborCount[u]++;
        }

        // Add u to v's neighbors if not already present
        if (!neighbors[v].contains(u)) {
            neighbors[v].add(u);
            neighborCount[v]++;
        }

        // Update number of vertices
        if (u + 1 > numVertices) numVertices = u + 1;
        if (v + 1 > numVertices) numVertices = v + 1;
    }

    public void clearNeighbors() {
        for (int i = 0; i < maxVertices; i++) {
            neighbors[i].clear();
            neighborCount[i] = 0;
        }
        Arrays.fill(groupAssignment, 0, maxVertices, 0);
    }

    // Initialize neighbors array with specific size
    public void initializeNeighbors(int maxVertices) {
        if (maxVertices > this.maxVertices) {
            ensureCapacity(maxVertices);
        }
    }

    // Set CSR data
    public void setCSRData(int[] colIndex, int[] rowPtr) {
        this.colIndex = colIndex;
        this.rowPtr = rowPtr;
        this.numVertices = rowPtr.length - 1;

        // Ensure we have enough capacity for the vertices
        ensureCapacity(this.numVertices);
    }

    // Set group data
    public void setGroupData(int[] groupList, int[] groupPtr) {
        this.groupList = groupList;
        this.groupPtr = groupPtr;
    }

    // DFS on dynamic adjacency list - iterative version to avoid stack overflow
    public void dfs(int startVertex, boolean[] visited) {
        Stack<Integer> stack = new Stack<>();

        stack.push(startVertex);
        visited[startVertex] = true;

        while (!stack.isEmpty()) {
            int v = stack.pop();

            for (int neighbor : neighbors[v]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    stack.push(neighbor);
                }
            }
        }
    }

    public boolean isConnected() {
        if (numVertices == 0) return true;

        boolean[] visited = new boolean[numVertices];
        dfs(0, visited);

        for (int i = 0; i < numVertices; i++) {
            if (!visited[i]) {
                return false;
            }
        }
        return true;
    }

    public void printGraph() {
        System.out.printf("Graph (vertices: %d/%d):%n", numVertices, maxVertices);
        System.out.println("\nAdjacency list:");

        for (int i = 0; i < numVertices; i++) {
            System.out.printf("%d (group %d): ", i, component[i]);
            for (int neighbor : neighbors[i]) {
                System.out.printf("%d ", neighbor);
            }
            System.out.println();
        }

        System.out.printf("Number of components: %d%n", numComponents);
        for (int i = 0; i < numVertices; i++) {
            System.out.printf("%d: %d%n", i, component[i]);
        }
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    public List<Integer>[] copyNeighbors() {
        List<Integer>[] copy = new List[getNumVertices()];
        for (int i = 0; i < getNumVertices(); i++) {
            copy[i] = new ArrayList<>(neighbors[i]);
        }
        return copy;
    }

    // Method to trim arrays to actual size (optional memory optimization)
    @SuppressWarnings("unchecked")
    public void trimToSize() {
        if (numVertices < maxVertices) {
            // Resize neighbors array
            List<Integer>[] newNeighbors = new List[numVertices];
            System.arraycopy(neighbors, 0, newNeighbors, 0, numVertices);
            neighbors = newNeighbors;

            // Resize other arrays
            neighborCount = Arrays.copyOf(neighborCount, numVertices);
            maxDistances = Arrays.copyOf(maxDistances, numVertices);
            groupAssignment = Arrays.copyOf(groupAssignment, numVertices);
            component = Arrays.copyOf(component, numVertices);

            maxVertices = numVertices;
        }
    }

    // Getters and setters
    public int getMaxVertices() { return maxVertices; }
    public void setMaxVertices(int maxVertices) {
        if (maxVertices > this.maxVertices) {
            ensureCapacity(maxVertices);
        }
    }
    public int getNumVertices() { return numVertices; }
    public void setNumVertices(int numVertices) {
        this.numVertices = numVertices;
        ensureCapacity(numVertices);
    }
    public int getNumComponents() { return numComponents; }
    public void setNumComponents(int numComponents) { this.numComponents = numComponents; }

    public List<Integer>[] getNeighbors() { return neighbors; }
    public int[] getNeighborCount() { return neighborCount; }
    public int[] getMaxDistances() { return maxDistances; }
    public int[] getGroupAssignment() { return groupAssignment; }
    public int[] getComponent() { return component; }

    public int[] getColIndex() { return colIndex; }
    public int[] getRowPtr() { return rowPtr; }
    public int[] getGroupList() { return groupList; }
    public int[] getGroupPtr() { return groupPtr; }

    /**
     * Zwraca liczbę krawędzi w grafie
     * @return liczba krawędzi
     */
    public int getNumEdges() {
        List<Integer>[] neighbors = getNeighbors();
        int edgeCount = 0;
        
        // Sumujemy połowy długości list sąsiedztwa (każda krawędź jest liczona dwa razy)
        for (int i = 0; i < getNumVertices(); i++) {
            edgeCount += neighbors[i].size();
        }
        
        // Dzielimy przez 2, bo każda krawędź jest liczona dwa razy (raz dla każdego końca)
        return edgeCount / 2;
    }
}