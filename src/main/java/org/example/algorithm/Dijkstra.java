package org.example.algorithm;
import org.example.model.Graph;
import java.util.List;
/**
 * Implementation of Dijkstra's algorithm for finding shortest paths
 */
public class Dijkstra {
    private static final int INF = Integer.MAX_VALUE;

    /**
     * Calculates distances from a starting vertex to all other vertices
     * and stores results in the provided distances array
     *
     * @param graph The graph
     * @param start The starting vertex
     * @param distances Array to store the maximum distance for each vertex
     */
    public static void calculateDistances(Graph graph, int start, int[] distances) {
        int[] dist = new int[graph.getNumVertices()];
        boolean[] visited = new boolean[graph.getNumVertices()];

        // Initialize distances
        for (int i = 0; i < graph.getNumVertices(); i++) {
            dist[i] = INF;
        }
        dist[start] = 0;

        // Main Dijkstra loop
        for (int count = 0; count < graph.getNumVertices() - 1; count++) {
            int u = -1;
            int minDist = INF;

            // Find vertex with minimum distance
            for (int v = 0; v < graph.getNumVertices(); v++) {
                if (!visited[v] && dist[v] < minDist) {
                    minDist = dist[v];
                    u = v;
                }
            }

            if (u == -1) break;

            visited[u] = true;

            // Update distances to neighbors
            List<Integer> neighbors = graph.getNeighbors(u);
            for (int v : neighbors) {
                if (!visited[v] && dist[u] + 1 < dist[v]) {
                    dist[v] = dist[u] + 1;
                }
            }
        }

        // Store maximum reachable distance from this vertex
        int maxDist = 0;
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (dist[i] != INF && dist[i] > maxDist) {
                maxDist = dist[i];
            }
        }

        // Store the maximum distance in the provided array
        distances[start] = maxDist;
    }
}