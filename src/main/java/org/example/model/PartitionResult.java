package org.example.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing the result of a graph partition operation
 */
public class PartitionResult {
    // Assignment of vertices to groups (mapping vertex ID to group ID)
    private final int[] vertexGroupAssignment;

    // Number of groups/partitions
    private final int numberOfGroups;

    // Flag indicating whether each group is connected
    private final boolean[] isGroupConnected;

    // Adjacency list representation of the graph after partitioning
    private final Map<Integer, List<Integer>>[] adjacencyListsAfterPartitioning;

    /**
     * Creates a new partition result object
     *
     * @param numVertices Number of vertices in the graph
     * @param numGroups Number of groups/partitions
     */
    @SuppressWarnings("unchecked")
    public PartitionResult(int numVertices, int numGroups) {
        this.vertexGroupAssignment = new int[numVertices];
        this.numberOfGroups = numGroups;
        this.isGroupConnected = new boolean[numGroups];
        this.adjacencyListsAfterPartitioning = new HashMap[numGroups];

        // Initialize adjacency lists for each group
        for (int i = 0; i < numGroups; i++) {
            adjacencyListsAfterPartitioning[i] = new HashMap<>();
        }
    }

    /**
     * Sets the group assignment for a vertex
     *
     * @param vertexId The vertex ID
     * @param groupId The group ID to assign to the vertex
     */
    public void setVertexGroup(int vertexId, int groupId) {
        if (groupId >= 0 && groupId < numberOfGroups) {
            vertexGroupAssignment[vertexId] = groupId;
        } else {
            throw new IllegalArgumentException("Invalid group ID: " + groupId);
        }
    }

    /**
     * Sets the vertex group assignments in bulk
     *
     * @param groupAssignments Array of group assignments
     */
    public void setVertexGroups(int[] groupAssignments) {
        if (groupAssignments.length == vertexGroupAssignment.length) {
            System.arraycopy(groupAssignments, 0, vertexGroupAssignment, 0, groupAssignments.length);
        } else {
            throw new IllegalArgumentException("Array length mismatch");
        }
    }

    /**
     * Sets whether a group is connected
     *
     * @param groupId The group ID
     * @param isConnected True if the group is connected, false otherwise
     */
    public void setGroupConnected(int groupId, boolean isConnected) {
        if (groupId >= 0 && groupId < numberOfGroups) {
            isGroupConnected[groupId] = isConnected;
        } else {
            throw new IllegalArgumentException("Invalid group ID: " + groupId);
        }
    }

    /**
     * Adds an edge to the adjacency list of a group
     *
     * @param groupId The group ID
     * @param fromVertex The source vertex
     * @param toVertex The destination vertex
     */
    public void addEdgeToGroup(int groupId, int fromVertex, int toVertex) {
        if (groupId >= 0 && groupId < numberOfGroups) {
            Map<Integer, List<Integer>> adjacencyList = adjacencyListsAfterPartitioning[groupId];

            // Add the edge from -> to
            adjacencyList.computeIfAbsent(fromVertex, k -> new ArrayList<>()).add(toVertex);

            // Add the edge to -> from (for undirected graph)
            adjacencyList.computeIfAbsent(toVertex, k -> new ArrayList<>()).add(fromVertex);
        } else {
            throw new IllegalArgumentException("Invalid group ID: " + groupId);
        }
    }

    /**
     * Builds the adjacency lists for all groups based on the original graph
     *
     * @param originalGraph The original graph before partitioning
     */
    public void buildAdjacencyLists(Graph originalGraph) {
        // Clear existing adjacency lists
        for (int i = 0; i < numberOfGroups; i++) {
            adjacencyListsAfterPartitioning[i].clear();
        }

        // For each vertex
        for (int i = 0; i < originalGraph.getNumVertices(); i++) {
            int sourceGroup = vertexGroupAssignment[i];

            // Get all neighbors
            List<Integer> neighbors = originalGraph.getNeighbors(i);
            for (int neighbor : neighbors) {
                int neighborGroup = vertexGroupAssignment[neighbor];

                // Only add edges between vertices in the same group
                if (sourceGroup == neighborGroup) {
                    addEdgeToGroup(sourceGroup, i, neighbor);
                }
            }
        }
    }

    /**
     * Computes if each group is connected
     *
     * @param originalGraph The original graph before partitioning
     */
    public void computeGroupConnectivity(Graph originalGraph) {
        // For each group
        for (int groupId = 0; groupId < numberOfGroups; groupId++) {
            // Find vertices in this group
            List<Integer> groupVertices = new ArrayList<>();
            for (int i = 0; i < vertexGroupAssignment.length; i++) {
                if (vertexGroupAssignment[i] == groupId) {
                    groupVertices.add(i);
                }
            }

            if (groupVertices.isEmpty()) {
                isGroupConnected[groupId] = true;  // Empty groups are considered connected
                continue;
            }

            // Perform DFS to check connectivity
            boolean[] visited = new boolean[originalGraph.getNumVertices()];
            dfs(originalGraph, groupVertices.get(0), visited, groupId);

            // Check if all vertices in the group were visited
            boolean allVisited = true;
            for (int vertex : groupVertices) {
                if (!visited[vertex]) {
                    allVisited = false;
                    break;
                }
            }

            isGroupConnected[groupId] = allVisited;
        }
    }

    /**
     * Helper method for DFS to check connectivity
     */
    private void dfs(Graph graph, int vertex, boolean[] visited, int groupId) {
        visited[vertex] = true;

        List<Integer> neighbors = graph.getNeighbors(vertex);
        for (int neighbor : neighbors) {
            if (!visited[neighbor] && vertexGroupAssignment[neighbor] == groupId) {
                dfs(graph, neighbor, visited, groupId);
            }
        }
    }

    /**
     * Gets the group assignment for a vertex
     *
     * @param vertexId The vertex ID
     * @return The group ID
     */
    public int getVertexGroup(int vertexId) {
        return vertexGroupAssignment[vertexId];
    }

    /**
     * Gets all group assignments
     *
     * @return Array of group assignments
     */
    public int[] getVertexGroups() {
        return vertexGroupAssignment;
    }

    /**
     * Gets the number of groups
     *
     * @return Number of groups
     */
    public int getNumberOfGroups() {
        return numberOfGroups;
    }

    /**
     * Checks if a group is connected
     *
     * @param groupId The group ID
     * @return True if the group is connected, false otherwise
     */
    public boolean isGroupConnected(int groupId) {
        if (groupId >= 0 && groupId < numberOfGroups) {
            return isGroupConnected[groupId];
        } else {
            throw new IllegalArgumentException("Invalid group ID: " + groupId);
        }
    }

    /**
     * Gets the adjacency list for a group
     *
     * @param groupId The group ID
     * @return The adjacency list
     */
    public Map<Integer, List<Integer>> getGroupAdjacencyList(int groupId) {
        if (groupId >= 0 && groupId < numberOfGroups) {
            return adjacencyListsAfterPartitioning[groupId];
        } else {
            throw new IllegalArgumentException("Invalid group ID: " + groupId);
        }
    }

    /**
     * Gets all vertices in a group
     *
     * @param groupId The group ID
     * @return List of vertices in the group
     */
    public List<Integer> getVerticesInGroup(int groupId) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < vertexGroupAssignment.length; i++) {
            if (vertexGroupAssignment[i] == groupId) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Checks if all groups are connected
     *
     * @return True if all groups are connected, false otherwise
     */
    public boolean areAllGroupsConnected() {
        for (boolean connected : isGroupConnected) {
            if (!connected) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a string representation of the partition result
     *
     * @return String representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Partition Result:\n");
        sb.append("Number of groups: ").append(numberOfGroups).append("\n");

        // Group connectivity
        sb.append("Group connectivity:\n");
        for (int i = 0; i < numberOfGroups; i++) {
            sb.append("  Group ").append(i).append(": ")
                    .append(isGroupConnected[i] ? "Connected" : "Disconnected").append("\n");
        }

        // Vertex assignments
        sb.append("Vertex assignments:\n");
        for (int i = 0; i < vertexGroupAssignment.length; i++) {
            sb.append("  Vertex ").append(i).append(" -> Group ")
                    .append(vertexGroupAssignment[i]).append("\n");
        }

        // Adjacency lists
        sb.append("Adjacency lists after partitioning:\n");
        for (int i = 0; i < numberOfGroups; i++) {
            sb.append("  Group ").append(i).append(":\n");
            Map<Integer, List<Integer>> adjList = adjacencyListsAfterPartitioning[i];
            for (Map.Entry<Integer, List<Integer>> entry : adjList.entrySet()) {
                sb.append("    Vertex ").append(entry.getKey()).append(" -> ")
                        .append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }
}