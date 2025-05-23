package org.example.model;
import java.util.*;

public class Graph {
    private static final int MAX_VERTICES = 4000;
    private static final int MAX_LINE_LENGTH = 2048;

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

    @SuppressWarnings("unchecked")
    public Graph() {
        initGraph();
    }


public Graph(Graph other) {
    // Copy basic properties
    this.maxVertices = other.maxVertices;
    this.numVertices = other.numVertices;
    this.numComponents = other.numComponents;

    // Initialize arrays
    this.neighbors = new List[MAX_VERTICES];
    this.neighborCount = new int[MAX_VERTICES];
    this.maxDistances = new int[MAX_VERTICES];
    this.groupAssignment = new int[MAX_VERTICES];
    this.component = new int[MAX_VERTICES];

    // Deep copy of neighbors lists
    for (int i = 0; i < MAX_VERTICES; i++) {
        this.neighbors[i] = new ArrayList<>(other.neighbors[i]);
        this.neighborCount[i] = other.neighborCount[i];
    }

    // Copy arrays
    System.arraycopy(other.maxDistances, 0, this.maxDistances, 0, MAX_VERTICES);
    System.arraycopy(other.groupAssignment, 0, this.groupAssignment, 0, MAX_VERTICES);
    System.arraycopy(other.component, 0, this.component, 0, MAX_VERTICES);

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

public void initGraph() {
    this.maxVertices = 0;
    this.numVertices = 0;
    this.numComponents = 0;

    // Initialize CSR pointers to null
    this.colIndex = null;
    this.rowPtr = null;
    this.groupList = null;
    this.groupPtr = null;

    // Allocate dynamic neighbor arrays
    this.neighbors = new List[MAX_VERTICES];
    this.neighborCount = new int[MAX_VERTICES];

    // Allocate additional dynamic arrays
    this.maxDistances = new int[MAX_VERTICES];
    this.groupAssignment = new int[MAX_VERTICES];
    this.component = new int[MAX_VERTICES];

    // Initialize neighbor lists as empty
    for (int i = 0; i < MAX_VERTICES; i++) {
        this.neighbors[i] = new ArrayList<>();
    }
}

public void addEdge(int u, int v) {
    if (u < 0 || v < 0) {
        throw new GraphException("Błąd: Indeks wierzchołka nie może być ujemny (u=" + u + ", v=" + v + ")");
    }
    
    if (u >= MAX_VERTICES || v >= MAX_VERTICES) {
        throw new GraphException("Błąd: Indeks wierzchołka przekracza maksymalną dozwoloną wartość " + 
            MAX_VERTICES + " (u=" + u + ", v=" + v + ")");
    }

    // Add v to u's neighbors if not already present
    if (!neighbors[u].contains(v)) {
        if (neighbors[u] == null) {
            throw new GraphException("Błąd: Lista sąsiedztwa dla wierzchołka " + u + " nie została zainicjalizowana");
        }
        neighbors[u].add(v);
        neighborCount[u]++;
    }

    // Add u to v's neighbors if not already present
    if (!neighbors[v].contains(u)) {
        if (neighbors[v] == null) {
            throw new GraphException("Błąd: Lista sąsiedztwa dla wierzchołka " + v + " nie została zainicjalizowana");
        }
        neighbors[v].add(u);
        neighborCount[v]++;
    }

    // Update number of vertices
    if (u + 1 > numVertices) numVertices = u + 1;
    if (v + 1 > numVertices) numVertices = v + 1;
}

public void clearNeighbors() {
    for (int i = 0; i < MAX_VERTICES; i++) {
        neighbors[i].clear();
        neighborCount[i] = 0;
    }
    Arrays.fill(groupAssignment, 0);
}

    // Initialize neighbors array with specific size
    public void initializeNeighbors(int maxVertices) {
        this.maxVertices = maxVertices;
    }

    // Set CSR data
    public void setCSRData(int[] colIndex, int[] rowPtr) {
        this.colIndex = colIndex;
        this.rowPtr = rowPtr;
        this.numVertices = rowPtr.length - 1;
    }

    // Set group data
    public void setGroupData(int[] groupList, int[] groupPtr) {
        this.groupList = groupList;
        this.groupPtr = groupPtr;
    }



    // DFS on dynamic adjacency list
    public void dfs(int v, boolean[] visited) {
        visited[v] = true;
        for (int neighbor : neighbors[v]) {
            if (!visited[neighbor]) {
                dfs(neighbor, visited);
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

public List<Integer>[] copyNeighbors() {
    @SuppressWarnings("unchecked")
    List<Integer>[] copy = new List[getNumVertices()];
    for (int i = 0; i < getNumVertices(); i++) {
        copy[i] = new ArrayList<>(neighbors[i]);
    }
    return copy;
}
    // Getters and setters
    public int getMaxVertices() { return maxVertices; }
    public void setMaxVertices(int maxVertices) { this.maxVertices = maxVertices; }
    public int getNumVertices() { return numVertices; }
    public void setNumVertices(int numVertices) { this.numVertices = numVertices; }
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
}