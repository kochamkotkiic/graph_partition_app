package org.example.algorithm;
import org.example.model.Graph;

import java.util.List;

/**
 * Implementation of Depth-First Search algorithm
 */
public class DFS {

    /**
     * Checks if a graph is connected using DFS
     *
     * @param graph The graph to check
     * @return True if graph is connected, false otherwise
     */
    public static boolean isConnected(Graph graph) {
        if (graph.getNumVertices() == 0) return true;

        boolean[] visited = new boolean[graph.getNumVertices()];
        dfs(graph, 0, visited);

        // Check if all vertices were visited
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (!visited[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Standard DFS traversal
     *
     * @param graph The graph
     * @param v Current vertex
     * @param visited Array of visited vertices
     */
    public static void dfs(Graph graph, int v, boolean[] visited) {
        visited[v] = true;

        List<Integer> neighbors = graph.getNeighbors(v);
        for (int neighbor : neighbors) {
            if (!visited[neighbor]) {
                dfs(graph, neighbor, visited);
            }
        }
    }

    /**
     * DFS traversal that marks vertices with component IDs
     *
     * @param graph The graph
     * @param v Current vertex
     * @param visited Array of visited vertices
     * @param component Array of component IDs
     * @param currentComponent Current component ID
     */
    public static void markComponent(Graph graph, int v, boolean[] visited,
                                     int[] component, int currentComponent) {
        visited[v] = true;
        component[v] = currentComponent;

        List<Integer> neighbors = graph.getNeighbors(v);
        for (int neighbor : neighbors) {
            if (!visited[neighbor]) {
                markComponent(graph, neighbor, visited, component, currentComponent);
            }
        }
    }
}