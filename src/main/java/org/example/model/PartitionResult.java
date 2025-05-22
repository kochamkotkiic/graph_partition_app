package org.example.model;
import org.example.algorithm.GraphPartitioner;
import org.example.model.Graph;
import java.util.*;

public class PartitionResult {

    public static class PartitionInfo {
        private int cutNumber;
        private int numComponents;
        private Map<Integer, List<Integer>> componentVertices;
        private boolean isBalanced;
        private int marginPercent;

        public PartitionInfo(int cutNumber, int numComponents, int marginPercent) {
            this.cutNumber = cutNumber;
            this.numComponents = numComponents;
            this.marginPercent = marginPercent;
            this.componentVertices = new HashMap<>();
            this.isBalanced = false;
        }

        // Getters
        public int getCutNumber() { return cutNumber; }
        public int getNumComponents() { return numComponents; }
        public Map<Integer, List<Integer>> getComponentVertices() { return componentVertices; }
        public boolean isBalanced() { return isBalanced; }
        public int getMarginPercent() { return marginPercent; }

        public void setBalanced(boolean balanced) { this.isBalanced = balanced; }
        public void addComponentVertices(int componentId, List<Integer> vertices) {
            this.componentVertices.put(componentId, new ArrayList<>(vertices));
        }
    }

    // DODANA BRAKUJĄCA KLASA
    public static class PartitioningResult {
        private final List<PartitionInfo> partitionInfos;
        private final List<Integer>[] originalNeighbors;

        public PartitioningResult(List<PartitionInfo> partitionInfos, List<Integer>[] originalNeighbors) {
            this.partitionInfos = new ArrayList<>(partitionInfos);
            this.originalNeighbors = deepCopyNeighbors(originalNeighbors);
        }

        public List<PartitionInfo> getPartitionInfos() {
            return new ArrayList<>(partitionInfos);
        }

        public List<Integer>[] getOriginalNeighbors() {
            return deepCopyNeighbors(originalNeighbors);
        }
    }

    // Usunąłem zduplikowaną metodę performPartitioningWithOriginal()
    public static PartitioningResult performPartitioningWithOriginal(Graph graph, int numCuts, int marginPercent) {
        List<Integer>[] originalNeighbors = deepCopyNeighbors(graph.getNeighbors());
        List<PartitionInfo> results = performPartitioning(graph, numCuts, marginPercent);
        return new PartitioningResult(results, originalNeighbors);
    }



    // Metoda do głębokiego kopiowania listy sąsiedztwa
    private static List<Integer>[] deepCopyNeighbors(List<Integer>[] neighbors) {
        if (neighbors == null) return null;

        @SuppressWarnings("unchecked")
        List<Integer>[] copy = new List[neighbors.length];
        for (int i = 0; i < neighbors.length; i++) {
            if (neighbors[i] != null) {
                copy[i] = new ArrayList<>(neighbors[i]);
            } else {
                copy[i] = new ArrayList<>();
            }
        }
        return copy;
    }

    /**
     * ORYGINALNA METODA - Performs recursive graph partitioning
     * @param graph The graph to partition
     * @param numCuts Number of cuts to perform
     * @param marginPercent Margin percentage for balancing
     * @return List of partition information for each successful cut
     */
    public static List<PartitionInfo> performPartitioning(Graph graph, int numCuts, int marginPercent) {
        List<PartitionInfo> results = new ArrayList<>();
        int successfulCuts = 0;

        System.out.println("=== STARTING GRAPH PARTITIONING ===");
        System.out.printf("Target cuts: %d, Margin: %d%%\n\n", numCuts, marginPercent);

        // Initial component analysis
        GraphPartitioner.findConnectedComponents(graph);
        System.out.printf("Initial graph has %d connected components\n", graph.getNumComponents());
        printAdjacencyList(graph, "INITIAL GRAPH");
        printComponentAnalysis(graph, 0);

        boolean partitionSuccess = true;

        while (successfulCuts < numCuts && partitionSuccess) {
            System.out.printf("\n=== ATTEMPTING CUT %d ===\n", successfulCuts + 1);

            // Store state before partition attempt
            int componentsBefore = graph.getNumComponents();

            // Attempt partition
            partitionSuccess = GraphPartitioner.partitionGraph(graph, marginPercent);

            if (partitionSuccess) {
                successfulCuts++;

                // Update components after partition
                GraphPartitioner.findConnectedComponents(graph);

                System.out.printf("✓ Cut %d successful! Components: %d -> %d\n",
                        successfulCuts, componentsBefore, graph.getNumComponents());

                // Create partition info
                PartitionInfo partitionInfo = new PartitionInfo(successfulCuts, graph.getNumComponents(), marginPercent);

                // Analyze and store component information
                analyzeComponents(graph, partitionInfo);

                // Print detailed results
                printAdjacencyList(graph, "AFTER CUT " + successfulCuts);
                printComponentAnalysis(graph, successfulCuts);
                printBalanceAnalysis(partitionInfo);

                results.add(partitionInfo);

            } else {
                System.err.printf("✗ Error: Failed to perform cut %d\n", successfulCuts + 1);
                System.err.println("Reason: No suitable partition found or connectivity constraints not met");
                break;
            }
        }

        // Final summary
        printFinalSummary(results, numCuts);

        return results;
    }

    /**
     * Analyzes components and determines if they are balanced
     */
    private static void analyzeComponents(Graph graph, PartitionInfo partitionInfo) {
        int[] component = graph.getComponent();
        Map<Integer, List<Integer>> componentMap = new HashMap<>();

        // Group vertices by component
        for (int i = 0; i < graph.getNumVertices(); i++) {
            int comp = component[i];
            componentMap.computeIfAbsent(comp, k -> new ArrayList<>()).add(i);
        }

        // Store component information
        for (Map.Entry<Integer, List<Integer>> entry : componentMap.entrySet()) {
            partitionInfo.addComponentVertices(entry.getKey(), entry.getValue());
        }

        // Check if components are balanced
        if (componentMap.size() >= 2) {
            List<Integer> sizes = new ArrayList<>();
            for (List<Integer> vertices : componentMap.values()) {
                sizes.add(vertices.size());
            }

            // Calculate balance
            int minSize = Collections.min(sizes);
            int maxSize = Collections.max(sizes);
            double imbalance = ((double)(maxSize - minSize) / minSize) * 100;

            partitionInfo.setBalanced(imbalance <= partitionInfo.getMarginPercent());
        }
    }

    /**
     * Prints the adjacency list of the graph
     */
    private static void printAdjacencyList(Graph graph, String title) {
        System.out.printf("\n--- %s - ADJACENCY LIST ---\n", title);
        List<Integer>[] neighbors = graph.getNeighbors();
        int[] component = graph.getComponent();

        for (int i = 0; i < graph.getNumVertices(); i++) {
            System.out.printf("Vertex %d (Component %d): ", i, component[i]);

            if (neighbors[i].isEmpty()) {
                System.out.print("[]");
            } else {
                System.out.print("[");
                for (int j = 0; j < neighbors[i].size(); j++) {
                    System.out.print(neighbors[i].get(j));
                    if (j < neighbors[i].size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.print("]");
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Prints component analysis
     */
    private static void printComponentAnalysis(Graph graph, int cutNumber) {
        System.out.printf("--- COMPONENT ANALYSIS (After Cut %d) ---\n", cutNumber);

        int[] component = graph.getComponent();
        Map<Integer, List<Integer>> componentMap = new HashMap<>();

        // Group vertices by component
        for (int i = 0; i < graph.getNumVertices(); i++) {
            int comp = component[i];
            componentMap.computeIfAbsent(comp, k -> new ArrayList<>()).add(i);
        }

        System.out.printf("Total components: %d\n", componentMap.size());

        for (Map.Entry<Integer, List<Integer>> entry : componentMap.entrySet()) {
            int compId = entry.getKey();
            List<Integer> vertices = entry.getValue();

            System.out.printf("Component %d: %d vertices -> ", compId, vertices.size());
            System.out.print("[");
            for (int i = 0; i < vertices.size(); i++) {
                System.out.print(vertices.get(i));
                if (i < vertices.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
        System.out.println();
    }

    /**
     * Prints balance analysis
     */
    private static void printBalanceAnalysis(PartitionInfo partitionInfo) {
        System.out.printf("--- BALANCE ANALYSIS (Cut %d) ---\n", partitionInfo.getCutNumber());

        Map<Integer, List<Integer>> components = partitionInfo.getComponentVertices();
        List<Integer> sizes = new ArrayList<>();

        for (Map.Entry<Integer, List<Integer>> entry : components.entrySet()) {
            int size = entry.getValue().size();
            sizes.add(size);
            System.out.printf("Component %d size: %d\n", entry.getKey(), size);
        }

        if (sizes.size() >= 2) {
            int minSize = Collections.min(sizes);
            int maxSize = Collections.max(sizes);
            double imbalance = ((double)(maxSize - minSize) / minSize) * 100;

            System.out.printf("Size difference: %d (min: %d, max: %d)\n", maxSize - minSize, minSize, maxSize);
            System.out.printf("Imbalance: %.2f%% (margin: %d%%)\n", imbalance, partitionInfo.getMarginPercent());
            System.out.printf("Balanced: %s\n", partitionInfo.isBalanced() ? "✓ YES" : "✗ NO");
        } else {
            System.out.println("Cannot assess balance - need at least 2 components");
        }
        System.out.println();
    }

    /**
     * Prints final summary of all partitioning results
     */
    private static void printFinalSummary(List<PartitionInfo> results, int targetCuts) {
        System.out.println("\n=== FINAL PARTITIONING SUMMARY ===");
        System.out.printf("Target cuts: %d\n", targetCuts);
        System.out.printf("Successful cuts: %d\n", results.size());
        System.out.printf("Success rate: %.2f%%\n", (double)results.size() / targetCuts * 100);

        if (!results.isEmpty()) {
            PartitionInfo lastResult = results.get(results.size() - 1);
            System.out.printf("Final components: %d\n", lastResult.getNumComponents());

            System.out.println("\nCut History:");
            for (PartitionInfo result : results) {
                System.out.printf("  Cut %d: %d components, %s\n",
                        result.getCutNumber(),
                        result.getNumComponents(),
                        result.isBalanced() ? "Balanced" : "Unbalanced");
            }

            System.out.println("\nFinal Component Sizes:");
            PartitionInfo finalResult = results.get(results.size() - 1);
            for (Map.Entry<Integer, List<Integer>> entry : finalResult.getComponentVertices().entrySet()) {
                System.out.printf("  Component %d: %d vertices\n", entry.getKey(), entry.getValue().size());
            }
        }

        System.out.println("=== END SUMMARY ===\n");
    }

    /**
     * Helper method to run partitioning with default settings and print results
     */
    public static void runPartitioningDemo(Graph graph, int numCuts, int marginPercent) {
        System.out.println("Starting graph partitioning demonstration...\n");

        List<PartitionInfo> results = performPartitioning(graph, numCuts, marginPercent);

        if (results.isEmpty()) {
            System.out.println("No successful partitions were made.");
        } else {
            System.out.printf("Partitioning completed with %d successful cuts.\n", results.size());
        }
    }
}