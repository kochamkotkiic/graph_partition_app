package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private int maxVertices;
    private int numVertices;
    private int numComponents;

    // Reprezentacja CSR
    private int[] colIndex;
    private int[] rowPtr;

    // Grupowanie wierzchołków
    private int[] groupList;
    private int[] groupPtr;

    // Lista sąsiedztwa
    private List<List<Integer>> neighbors;
    private int[] neighborCount;

    // Dane pomocnicze
    private int[] groupAssignment;
    private int[] component;

    public Graph() {
        this.neighbors = new ArrayList<>();
        this.neighborCount = new int[0];
        this.groupAssignment = new int[0];
        this.component = new int[0];
    }

    // Gettery i settery
    public int getMaxVertices() { return maxVertices; }
    public void setMaxVertices(int maxVertices) { this.maxVertices = maxVertices; }

    public int getNumVertices() { return numVertices; }
    public int getNumComponents() { return numComponents; }

    public int[] getColIndex() { return colIndex; }
    public int[] getRowPtr() { return rowPtr; }
    public int[] getGroupList() { return groupList; }
    public int[] getGroupPtr() { return groupPtr; }
    public int[] getGroupAssignment() { return groupAssignment; }
    public int[] getComponent() { return component; }

    public List<Integer> getNeighbors(int vertex) {
        if (vertex >= 0 && vertex < neighbors.size()) {
            return neighbors.get(vertex);
        }
        return new ArrayList<>();
    }

    public void setCSRData(int[] colIndex, int[] rowPtr) {
        this.colIndex = colIndex;
        this.rowPtr = rowPtr;
        this.numVertices = rowPtr.length - 1;
    }

    public void setGroupData(int[] groupList, int[] groupPtr) {
        this.groupList = groupList;
        this.groupPtr = groupPtr;
    }

    public void initializeNeighbors(int size) {
        this.neighbors = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            neighbors.add(new ArrayList<>());
        }
        this.neighborCount = new int[size];
        this.groupAssignment = new int[size];
        this.component = new int[size];
    }

    public void clearNeighbors() {
        for (List<Integer> neighborList : neighbors) {
            neighborList.clear();
        }
        this.neighborCount = new int[neighborCount.length];
    }

    public void addEdge(int u, int v) {
        if (u >= 0 && u < neighbors.size() && v >= 0 && v < neighbors.size()) {
            if (!neighbors.get(u).contains(v)) {
                neighbors.get(u).add(v);
                neighborCount[u]++;
            }
            if (!neighbors.get(v).contains(u)) {
                neighbors.get(v).add(u);
                neighborCount[v]++;
            }
        }
    }
}