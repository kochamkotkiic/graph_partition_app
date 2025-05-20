package org.example.algorithm;

import org.example.model.Graph;
import org.example.model.PartitionResult;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for graph partitioning algorithm
 */
public class GraphPartitioner {
    private static final int MAX_VERTICES = 1000; // Adjust as needed
    private static final int INF = Integer.MAX_VALUE;

    // For storing maximum distances
    private static int[] maxDistances;

    /**
     * Partitions a graph into connected components
     * @param graph The graph to partition
     * @param cuts Number of desired cuts
     * @param margin The maximum allowed imbalance between partitions (percentage)
     * @return PartitionResult containing the partition information or null if partitioning failed
     */
    public static PartitionResult partition(Graph graph, int cuts, int margin) {
        // Initialize max distances array
        maxDistances = new int[graph.getNumVertices()];

        // Initialize components
        findConnectedComponents(graph);

        // Try to partition each component
        PartitionResult result = new PartitionResult(graph.getNumVertices(), cuts + 1);
        boolean success = partitionGraph(graph, margin, result);

        if (success) {
            // Build the final result
            result.setVertexGroups(graph.getComponent());
            result.buildAdjacencyLists(graph);
            result.computeGroupConnectivity(graph);
            return result;
        } else {
            return null;
        }
    }

    /**
     * Find connected components in the graph
     */
    private static void findConnectedComponents(Graph graph) {
        boolean[] visited = new boolean[graph.getNumVertices()];
        int numComponents = 0;

        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (!visited[i]) {
                DFS.markComponent(graph, i, visited, graph.getComponent(), numComponents);
                numComponents++;
            }
        }

        // We need to set the numComponents value, but there's no setter
        // This is a potential issue in the design
    }

    /**
     * Attempts to partition the graph
     */
    private static boolean partitionGraph(Graph graph, int margin, PartitionResult result) {
        // Determine the number of components
        int numComponents = 0;
        int[] component = graph.getComponent();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (component[i] > numComponents) {
                numComponents = component[i];
            }
        }
        numComponents++; // Adjust for 0-based indexing

        // For each component, try to partition
        for (int comp = 0; comp < numComponents; comp++) {
            // Find central vertex in this component
            int center = -1;
            int minMaxDist = Integer.MAX_VALUE;

            for (int i = 0; i < graph.getNumVertices(); i++) {
                if (graph.getComponent()[i] == comp) {
                    Dijkstra.calculateDistances(graph, i, maxDistances);
                    if (maxDistances[i] < minMaxDist) {
                        minMaxDist = maxDistances[i];
                        center = i;
                    }
                }
            }

            if (center == -1) continue;

            // Prepare lists for the two groups
            List<Integer> group1 = new ArrayList<>();
            List<Integer> group2 = new ArrayList<>();

            // Count vertices in this component
            int targetSize = 0;
            for (int i = 0; i < graph.getNumVertices(); i++) {
                if (graph.getComponent()[i] == comp) {
                    targetSize++;
                }
            }

            if (targetSize < 2) continue;

            // Calculate margin based on component size
            int actualMargin = margin * targetSize / 100;

            // Target size for first group
            targetSize /= 2;

            // Use DFS to assign vertices to group1
            boolean[] visited = new boolean[graph.getNumVertices()];
            int[] stack = new int[MAX_VERTICES];
            int stackSize = 0;

            stack[stackSize++] = center;
            visited[center] = true;
            group1.add(center);
            graph.getGroupAssignment()[center] = 1;

            while (stackSize > 0 && group1.size() < targetSize) {
                int current = stack[--stackSize];

                int maxDist = -1;
                int nextVertex = -1;

                // Find next vertex with maximum distance
                List<Integer> neighbors = graph.getNeighbors(current);
                for (int neighbor : neighbors) {
                    if (!visited[neighbor] && graph.getComponent()[neighbor] == comp) {
                        if (maxDistances[neighbor] > maxDist) {
                            maxDist = maxDistances[neighbor];
                            nextVertex = neighbor;
                        }
                    }
                }

                if (nextVertex != -1) {
                    stack[stackSize++] = current;
                    stack[stackSize++] = nextVertex;
                    visited[nextVertex] = true;
                    group1.add(nextVertex);
                    graph.getGroupAssignment()[nextVertex] = 1;
                }
            }

            // Assign remaining vertices to group2
            for (int i = 0; i < graph.getNumVertices(); i++) {
                if (graph.getComponent()[i] == comp && !visited[i]) {
                    group2.add(i);
                    graph.getGroupAssignment()[i] = 2;
                }
            }

            // Check if group2 is connected
            boolean[] group2Included = new boolean[graph.getNumVertices()];
            for (int v : group2) {
                group2Included[v] = true;
            }

            if (!isComponentConnected(graph, group2Included)) {
                // Find largest connected component in group2
                boolean[] largestComponent = new boolean[graph.getNumVertices()];
                int largestSize = 0;
                boolean[] processed = new boolean[graph.getNumVertices()];

                for (int v : group2) {
                    if (!processed[v]) {
                        boolean[] currentComponent = new boolean[graph.getNumVertices()];
                        int currentSize = 0;
                        int[] componentStack = new int[MAX_VERTICES];
                        int cstackSize = 0;

                        componentStack[cstackSize++] = v;
                        currentComponent[v] = true;
                        processed[v] = true;

                        while (cstackSize > 0) {
                            int cv = componentStack[--cstackSize];
                            currentSize++;

                            List<Integer> neighbors = graph.getNeighbors(cv);
                            for (int neighbor : neighbors) {
                                if (graph.getGroupAssignment()[neighbor] == 2 && !currentComponent[neighbor]) {
                                    currentComponent[neighbor] = true;
                                    processed[neighbor] = true;
                                    componentStack[cstackSize++] = neighbor;
                                }
                            }
                        }

                        if (currentSize > largestSize) {
                            largestSize = currentSize;
                            System.arraycopy(currentComponent, 0, largestComponent, 0, graph.getNumVertices());
                        }
                    }
                }

                // Move non-largest component vertices to group1
                List<Integer> newGroup2 = new ArrayList<>();
                for (int v : group2) {
                    if (largestComponent[v]) {
                        newGroup2.add(v);
                    } else {
                        group1.add(v);
                        graph.getGroupAssignment()[v] = 1;
                    }
                }
                group2 = newGroup2;
            }

            // Check margin and balance if needed
            int sizeDiff = Math.abs(group1.size() - group2.size());
            if (sizeDiff > actualMargin) {
                if (balanceGroups(graph, group1, group2, actualMargin)) {
                    int newComponentId = numComponents + 1;
                    splitGraph(graph, newComponentId);

                    // Update the result with the new partitioning
                    result.setVertexGroups(graph.getComponent());
                    return true;
                }
            } else {
                int newComponentId = numComponents + 1;
                splitGraph(graph, newComponentId);

                // Update the result with the new partitioning
                result.setVertexGroups(graph.getComponent());
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a component is connected
     */
    private static boolean isComponentConnected(Graph graph, boolean[] inComponent) {
        int start = -1;
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (inComponent[i]) {
                start = i;
                break;
            }
        }

        if (start == -1) return true;

        boolean[] visited = new boolean[graph.getNumVertices()];
        int[] stack = new int[MAX_VERTICES];
        int stackSize = 0;
        int visitedCount = 0;

        stack[stackSize++] = start;
        visited[start] = true;

        while (stackSize > 0) {
            int current = stack[--stackSize];
            visitedCount++;

            List<Integer> neighbors = graph.getNeighbors(current);
            for (int neighbor : neighbors) {
                if (inComponent[neighbor] && !visited[neighbor]) {
                    visited[neighbor] = true;
                    stack[stackSize++] = neighbor;
                }
            }
        }

        int componentSize = 0;
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (inComponent[i]) componentSize++;
        }

        return visitedCount == componentSize;
    }

    /**
     * Balances group sizes while maintaining connectivity
     */
    private static boolean balanceGroups(Graph graph, List<Integer> group1, List<Integer> group2, int margin) {
        // Sort vertices by max distance
        List<VertexInfo> vertices = new ArrayList<>();

        for (int v : group1) {
            vertices.add(new VertexInfo(v, maxDistances[v]));
        }

        // Sort by max distance
        vertices.sort((a, b) -> Integer.compare(a.maxDist, b.maxDist));

        // Try to move vertices from group1 to group2
        for (VertexInfo vInfo : vertices) {
            int v = vInfo.vertex;

            // Check if vertex is still in group1
            if (!group1.contains(v)) continue;

            // Temporarily move to group2
            graph.getGroupAssignment()[v] = 2;

            // Check group1 connectivity without this vertex
            boolean group1Connected = true;
            if (group1.size() > 1) {
                boolean[] group1Included = new boolean[graph.getNumVertices()];
                for (int vertex : group1) {
                    if (vertex != v) {
                        group1Included[vertex] = true;
                    }
                }
                group1Connected = isComponentConnected(graph, group1Included);
            }

            // Check group2 connectivity with this vertex
            boolean group2Connected = true;
            if (!group2.isEmpty()) {
                boolean[] group2Included = new boolean[graph.getNumVertices()];
                for (int vertex : group2) {
                    group2Included[vertex] = true;
                }
                group2Included[v] = true;
                group2Connected = isComponentConnected(graph, group2Included);
            }

            // Restore original assignment before decision
            graph.getGroupAssignment()[v] = 1;

            if (group1Connected && group2Connected) {
                // Move vertex from group1 to group2
                graph.getGroupAssignment()[v] = 2;
                group2.add(v);
                group1.remove(Integer.valueOf(v));

                // Check margin condition
                int sizeDiff = Math.abs(group1.size() - group2.size());
                if (sizeDiff <= margin) {
                    return true;
                }

                // Continue balancing
                return balanceGroups(graph, group1, group2, margin);
            }
        }

        return false;
    }

    /**
     * Splits the graph by removing edges between groups
     */
    private static void splitGraph(Graph graph, int newComponentId) {
        // Update component assignments
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (graph.getGroupAssignment()[i] == 2) {
                graph.getComponent()[i] = newComponentId;
            }
        }

        // Reset group assignments
        Arrays.fill(graph.getGroupAssignment(), 0);
    }

    /**
     * Helper class to store vertex information for sorting
     */
    private static class VertexInfo {
        int vertex;
        int maxDist;

        VertexInfo(int vertex, int maxDist) {
            this.vertex = vertex;
            this.maxDist = maxDist;
        }
    }
}