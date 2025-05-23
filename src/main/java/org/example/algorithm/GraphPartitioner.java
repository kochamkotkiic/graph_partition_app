package org.example.algorithm;
import org.example.model.Graph;
import org.example.model.PartitionResult;
import java.util.*;

public class GraphPartitioner {
    private static final int MAX_VERTICES = 4000;
    private static final int INF = Integer.MAX_VALUE;

    // ====================== HELPER FUNCTIONS ======================

    public static void dfsMarkComponents(Graph graph, int v, boolean[] visited, int[] component, int currentComponent) {
        visited[v] = true;
        component[v] = currentComponent;

        List<Integer>[] neighbors = graph.getNeighbors();
        for (int neighbor : neighbors[v]) {
            if (!visited[neighbor]) {
                dfsMarkComponents(graph, neighbor, visited, component, currentComponent);
            }
        }
    }

    // ====================== CONNECTED COMPONENTS ANALYSIS ======================

    public static void findConnectedComponents(Graph graph) {
        boolean[] visited = new boolean[MAX_VERTICES];
        int[] component = graph.getComponent();
        int numComponents = 0;

        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (!visited[i]) {
                dfsMarkComponents(graph, i, visited, component, numComponents);
                numComponents++;
            }
        }

        graph.setNumComponents(numComponents);
    }

    // ====================== DIJKSTRA ======================

    public static void dijkstra(Graph graph, int start) {
        int[] dist = new int[graph.getNumVertices()];
        boolean[] visited = new boolean[graph.getNumVertices()];
        List<Integer>[] neighbors = graph.getNeighbors();

        Arrays.fill(dist, INF);
        dist[start] = 0;

        for (int count = 0; count < graph.getNumVertices() - 1; count++) {
            int u = -1;
            int minDist = INF;

            for (int v = 0; v < graph.getNumVertices(); v++) {
                if (!visited[v] && dist[v] < minDist) {
                    minDist = dist[v];
                    u = v;
                }
            }

            if (u == -1) break;

            visited[u] = true;
            for (int v : neighbors[u]) {
                if (!visited[v] && dist[u] + 1 < dist[v]) {
                    dist[v] = dist[u] + 1;
                }
            }
        }

        int maxDist = 0;
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (dist[i] != INF && dist[i] > maxDist) {
                maxDist = dist[i];
            }
        }
        graph.getMaxDistances()[start] = maxDist;
    }

    // ====================== CONNECTIVITY FUNCTIONS ======================

    public static boolean isComponentConnected(Graph graph, boolean[] inComponent) {
        int start = -1;
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (inComponent[i]) {
                start = i;
                break;
            }
        }
        if (start == -1) return true;

        boolean[] visited = new boolean[MAX_VERTICES];
        Stack<Integer> stack = new Stack<>();
        int visitedCount = 0;
        List<Integer>[] neighbors = graph.getNeighbors();

        stack.push(start);
        visited[start] = true;

        while (!stack.isEmpty()) {
            int current = stack.pop();
            visitedCount++;

            for (int neighbor : neighbors[current]) {
                if (inComponent[neighbor] && !visited[neighbor]) {
                    visited[neighbor] = true;
                    stack.push(neighbor);
                }
            }
        }

        int componentSize = 0;
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (inComponent[i]) componentSize++;
        }

        return visitedCount == componentSize;
    }

    // ====================== GROUP BALANCING ======================

    static class VertexInfo {
        int vertex;
        int maxDist;

        VertexInfo(int vertex, int maxDist) {
            this.vertex = vertex;
            this.maxDist = maxDist;
        }
    }

    public static boolean balanceGroups(Graph graph, List<Integer> group1, List<Integer> group2, int margin) {
        List<VertexInfo> vertices = new ArrayList<>();
        int[] maxDistances = graph.getMaxDistances();
        int[] groupAssignment = graph.getGroupAssignment();

        // Prepare sorted list of vertices from group 1
        for (int v : group1) {
            vertices.add(new VertexInfo(v, maxDistances[v]));
        }

        // Sort by max distance (ascending)
        vertices.sort(Comparator.comparingInt(v -> v.maxDist));

        // Try to move vertices starting from smallest max_distance
        for (VertexInfo vertexInfo : vertices) {
            int v = vertexInfo.vertex;

            // Check if vertex is still in group 1
            if (!group1.contains(v)) continue;

            // Temporarily move to group 2
            groupAssignment[v] = 2;

            // Check group 1 connectivity without this vertex
            boolean group1Connected = true;
            if (group1.size() > 1) {
                boolean[] group1Included = new boolean[MAX_VERTICES];
                for (int vertex : group1) {
                    if (vertex != v) {
                        group1Included[vertex] = true;
                    }
                }
                group1Connected = isComponentConnected(graph, group1Included);
            }

            // Check group 2 connectivity with new vertex
            boolean group2Connected = true;
            if (!group2.isEmpty()) {
                boolean[] group2Included = new boolean[MAX_VERTICES];
                for (int vertex : group2) {
                    group2Included[vertex] = true;
                }
                group2Included[v] = true;
                group2Connected = isComponentConnected(graph, group2Included);
            }

            // Restore original assignment before making decision
            groupAssignment[v] = 1;

            if (group1Connected && group2Connected) {
                // Perform actual transfer
                groupAssignment[v] = 2;
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

    // ====================== MAIN PARTITIONING LOGIC ======================

    public static boolean partitionGraph(Graph graph, int margin) {
        int[] component = graph.getComponent();
        int[] maxDistances = graph.getMaxDistances();
        int[] groupAssignment = graph.getGroupAssignment();
        List<Integer>[] neighbors = graph.getNeighbors();

        // Try partitioning for each component
        for (int comp = 0; comp < graph.getNumComponents(); comp++) {
            // Find central vertex in component
            int center = -1;
            int minMaxDist = Integer.MAX_VALUE;

            for (int i = 0; i < graph.getNumVertices(); i++) {
                if (component[i] == comp) {
                    dijkstra(graph, i);
                    if (maxDistances[i] < minMaxDist) {
                        minMaxDist = maxDistances[i];
                        center = i;
                    }
                }
            }

            if (center == -1) continue;

            List<Integer> group1 = new ArrayList<>();
            List<Integer> group2 = new ArrayList<>();

            // Assign vertices to groups using DFS
            boolean[] visited = new boolean[MAX_VERTICES];
            Stack<Integer> stack = new Stack<>();
            int targetSize = 0;

            // Count vertices in this component
            for (int i = 0; i < graph.getNumVertices(); i++) {
                if (component[i] == comp) {
                    targetSize++;
                }
            }

            if (targetSize < 2) continue;

            margin = margin * targetSize / 100;
            targetSize /= 2;

            stack.push(center);
            visited[center] = true;
            group1.add(center);
            groupAssignment[center] = 1;

            while (!stack.isEmpty() && group1.size() < targetSize) {
                int current = stack.pop();

                int maxDist = -1;
                int nextVertex = -1;
                for (int neighbor : neighbors[current]) {
                    if (!visited[neighbor] && component[neighbor] == comp) {
                        if (maxDistances[neighbor] > maxDist) {
                            maxDist = maxDistances[neighbor];
                            nextVertex = neighbor;
                        }
                    }
                }

                if (nextVertex != -1) {
                    stack.push(current);
                    stack.push(nextVertex);
                    visited[nextVertex] = true;
                    group1.add(nextVertex);
                    groupAssignment[nextVertex] = 1;
                }
            }

            // Rest goes to group 2
            for (int i = 0; i < graph.getNumVertices(); i++) {
                if (component[i] == comp && !visited[i]) {
                    group2.add(i);
                    groupAssignment[i] = 2;
                }
            }

            // Check group 2 connectivity
            boolean[] group2Included = new boolean[MAX_VERTICES];
            for (int vertex : group2) {
                group2Included[vertex] = true;
            }

            if (!isComponentConnected(graph, group2Included)) {
                // Find largest connected part in group 2
                boolean[] largestComponent = new boolean[MAX_VERTICES];
                int largestSize = 0;
                boolean[] processed = new boolean[MAX_VERTICES];

                for (int v : group2) {
                    if (!processed[v]) {
                        boolean[] currentComponent = new boolean[MAX_VERTICES];
                        int currentSize = 0;
                        Stack<Integer> componentStack = new Stack<>();

                        componentStack.push(v);
                        currentComponent[v] = true;
                        processed[v] = true;

                        while (!componentStack.isEmpty()) {
                            int cv = componentStack.pop();
                            currentSize++;

                            for (int neighbor : neighbors[cv]) {
                                if (groupAssignment[neighbor] == 2 && !currentComponent[neighbor]) {
                                    currentComponent[neighbor] = true;
                                    processed[neighbor] = true;
                                    componentStack.push(neighbor);
                                }
                            }
                        }

                        if (currentSize > largestSize) {
                            largestSize = currentSize;
                            System.arraycopy(currentComponent, 0, largestComponent, 0,MAX_VERTICES);
                        }
                    }
                }

                // Move vertices outside largest component to group 1
                List<Integer> newGroup2 = new ArrayList<>();
                for (int v : group2) {
                    if (largestComponent[v]) {
                        newGroup2.add(v);
                    } else {
                        group1.add(v);
                        groupAssignment[v] = 1;
                    }
                }
                group2 = newGroup2;
            }

            // Check margin condition and balance if necessary
            int sizeDiff = Math.abs(group1.size() - group2.size());
            if (sizeDiff > margin) {
                if (balanceGroups(graph, group1, group2, margin)) {
                    splitGraph(graph);
                    return true;
                }
            } else {
                splitGraph(graph);
                return true;
            }
        }

        return false;
    }

    public static void splitGraph(Graph graph) {
        int newComponentId = graph.getNumComponents(); // new component
        int[] component = graph.getComponent();
        int[] groupAssignment = graph.getGroupAssignment();
        List<Integer>[] neighbors = graph.getNeighbors();
        int[] neighborCount = graph.getNeighborCount();

        // Update component assignments
        for (int i = 0; i < graph.getNumVertices(); i++) {
            if (groupAssignment[i] == 2) {
                component[i] = newComponentId;
            }
        }
        graph.setNumComponents(graph.getNumComponents() + 1);

        // Remove edges between groups
        for (int i = 0; i < graph.getNumVertices(); i++) {
            List<Integer> newNeighbors = new ArrayList<>();

            for (int neighbor : neighbors[i]) {
                // Keep only edges within the same group
                if (groupAssignment[i] == groupAssignment[neighbor]) {
                    newNeighbors.add(neighbor);
                }
            }

            // Update neighbor list
            neighbors[i].clear();
            neighbors[i].addAll(newNeighbors);
            neighborCount[i] = newNeighbors.size();
        }

        // Reset group assignments
        Arrays.fill(groupAssignment, 0);
    }
    public static List<PartitionResult.PartitionInfo> performPartitioning(Graph graph, int numCuts, int marginPercent) {
        return PartitionResult.performPartitioning(graph, numCuts, marginPercent);
    }
}