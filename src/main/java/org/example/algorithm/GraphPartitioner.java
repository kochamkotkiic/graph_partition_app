package org.example.algorithm;
import org.example.model.Graph;
import org.example.model.PartitionResult;
import java.util.*;

public class GraphPartitioner {
    private static final int INF = Integer.MAX_VALUE;

    // ====================== HELPER FUNCTIONS ======================

    public static void dfsMarkComponents(Graph graph, int startVertex, boolean[] visited, int[] component, int currentComponent) {
        Stack<Integer> stack = new Stack<>();
        List<Integer>[] neighbors = graph.getNeighbors();

        stack.push(startVertex);
        visited[startVertex] = true;
        component[startVertex] = currentComponent;

        while (!stack.isEmpty()) {
            int v = stack.pop();

            for (int neighbor : neighbors[v]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    component[neighbor] = currentComponent;
                    stack.push(neighbor);
                }
            }
        }
    }

    // ====================== CONNECTED COMPONENTS ANALYSIS ======================

    public static void findConnectedComponents(Graph graph) {
        int numVertices = graph.getNumVertices();
        boolean[] visited = new boolean[numVertices];
        int[] component = graph.getComponent();
        int numComponents = 0;

        for (int i = 0; i < numVertices; i++) {
            if (!visited[i]) {
                dfsMarkComponents(graph, i, visited, component, numComponents);
                numComponents++;
            }
        }

        graph.setNumComponents(numComponents);
    }

    // ====================== DIJKSTRA ======================

    static class DistanceVertex implements Comparable<DistanceVertex> {
        int vertex;
        int distance;

        DistanceVertex(int vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(DistanceVertex other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    public static void dijkstra(Graph graph, int start) {
        int numVertices = graph.getNumVertices();
        int[] dist = new int[numVertices];
        boolean[] visited = new boolean[numVertices];
        List<Integer>[] neighbors = graph.getNeighbors();
        PriorityQueue<DistanceVertex> pq = new PriorityQueue<>();

        Arrays.fill(dist, INF);
        dist[start] = 0;
        pq.offer(new DistanceVertex(start, 0));

        while (!pq.isEmpty()) {
            DistanceVertex current = pq.poll();
            int u = current.vertex;

            if (visited[u]) continue;
            visited[u] = true;

            for (int v : neighbors[u]) {
                if (!visited[v] && dist[u] + 1 < dist[v]) {
                    dist[v] = dist[u] + 1;
                    pq.offer(new DistanceVertex(v, dist[v]));
                }
            }
        }

        int maxDist = 0;
        for (int i = 0; i < numVertices; i++) {
            if (dist[i] != INF && dist[i] > maxDist) {
                maxDist = dist[i];
            }
        }
        graph.getMaxDistances()[start] = maxDist;
    }

    // ====================== CONNECTIVITY FUNCTIONS ======================

    public static boolean isComponentConnected(Graph graph, boolean[] inComponent) {
        int numVertices = graph.getNumVertices();
        int start = -1;
        for (int i = 0; i < numVertices; i++) {
            if (inComponent[i]) {
                start = i;
                break;
            }
        }
        if (start == -1) return true;

        boolean[] visited = new boolean[numVertices];
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
        for (int i = 0; i < numVertices; i++) {
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
        int numVertices = graph.getNumVertices();

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
                boolean[] group1Included = new boolean[numVertices];
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
                boolean[] group2Included = new boolean[numVertices];
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
                    return true; // Successfully balanced within margin
                }

                // If still not balanced, continue trying
                // DON'T make recursive call here - continue with the loop instead
            }
        }

        // If we've tried all vertices and still can't balance within margin
        return false;
    }

    // ====================== MAIN PARTITIONING LOGIC ======================

    public static boolean partitionGraph(Graph graph, int marginPercent) {
        int[] component = graph.getComponent();
        int[] maxDistances = graph.getMaxDistances();
        int[] groupAssignment = graph.getGroupAssignment();
        List<Integer>[] neighbors = graph.getNeighbors();
        int numVertices = graph.getNumVertices();

        // Try partitioning for each component
        for (int comp = 0; comp < graph.getNumComponents(); comp++) {
            // Count vertices in this component
            int componentSize = 0;
            for (int i = 0; i < numVertices; i++) {
                if (component[i] == comp) {
                    componentSize++;
                }
            }

            if (componentSize < 2) continue;

            // Find central vertex in component
            int center = -1;
            int minMaxDist = Integer.MAX_VALUE;

            for (int i = 0; i < numVertices; i++) {
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

            // Calculate margin based on component size (not target size)
            int allowedMargin = marginPercent * componentSize / 100;
            int targetSize = componentSize / 2;

            // Assign vertices to groups using DFS
            boolean[] visited = new boolean[numVertices];
            Stack<Integer> stack = new Stack<>();

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
            for (int i = 0; i < numVertices; i++) {
                if (component[i] == comp && !visited[i]) {
                    group2.add(i);
                    groupAssignment[i] = 2;
                }
            }

            // Check group 2 connectivity and fix if needed
            boolean[] group2Included = new boolean[numVertices];
            for (int vertex : group2) {
                group2Included[vertex] = true;
            }

            if (!isComponentConnected(graph, group2Included)) {
                // Find largest connected part in group 2
                boolean[] largestComponent = new boolean[numVertices];
                int largestSize = 0;
                boolean[] processed = new boolean[numVertices];

                for (int v : group2) {
                    if (!processed[v]) {
                        boolean[] currentComponent = new boolean[numVertices];
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
                            System.arraycopy(currentComponent, 0, largestComponent, 0, numVertices);
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

            // Check margin condition
            int sizeDiff = Math.abs(group1.size() - group2.size());

            // If difference is within margin, perform split
            if (sizeDiff <= allowedMargin) {
                splitGraph(graph);
                return true;
            }
            // If difference exceeds margin, try to balance
            else {
                boolean balanced = balanceGroups(graph, group1, group2, allowedMargin);
                if (balanced) {
                    // Check final balance after balancing
                    int finalDiff = Math.abs(group1.size() - group2.size());
                    if (finalDiff <= allowedMargin) {
                        splitGraph(graph);
                        return true;
                    }
                }
                // If balancing failed or still exceeds margin, don't split this component
                // Reset group assignments for this component
                for (int i = 0; i < numVertices; i++) {
                    if (component[i] == comp) {
                        groupAssignment[i] = 0;
                    }
                }
            }
        }

        return false; // No component could be partitioned within margin
    }

    public static void splitGraph(Graph graph) {
        int newComponentId = graph.getNumComponents(); // new component
        int[] component = graph.getComponent();
        int[] groupAssignment = graph.getGroupAssignment();
        List<Integer>[] neighbors = graph.getNeighbors();
        int[] neighborCount = graph.getNeighborCount();
        int numVertices = graph.getNumVertices();

        // Update component assignments
        for (int i = 0; i < numVertices; i++) {
            if (groupAssignment[i] == 2) {
                component[i] = newComponentId;
            }
        }
        graph.setNumComponents(graph.getNumComponents() + 1);

        // Remove edges between groups
        for (int i = 0; i < numVertices; i++) {
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
        Arrays.fill(groupAssignment, 0, numVertices, 0);
    }

    public static List<PartitionResult.PartitionInfo> performPartitioning(Graph graph, int numCuts, int marginPercent) {
        return PartitionResult.performPartitioning(graph, numCuts, marginPercent);
    }
}