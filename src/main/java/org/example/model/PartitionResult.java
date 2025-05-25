package org.example.model;

import org.example.algorithm.GraphPartitioner;
import java.util.*;

public class PartitionResult {
    public static class PartitionInfo {
        private final int cutNumber;
        private final int numComponents;
        private final int marginPercent;
        private boolean isBalanced;

        private final Map<Integer, List<Integer>> componentVertices;

        public PartitionInfo(int cutNumber, int numComponents, int marginPercent) {
            this.cutNumber = cutNumber;
            this.numComponents = numComponents;
            this.marginPercent = marginPercent;
            this.componentVertices = new HashMap<>();
        }

        public void addComponentVertices(int componentId, List<Integer> vertices) {
            componentVertices.put(componentId, new ArrayList<>(vertices));
        }

        public int getCutNumber() {
            return cutNumber;
        }

        public int getNumComponents() {
            return numComponents;
        }

        public int getMarginPercent() {
            return marginPercent;
        }

        public boolean isBalanced() {
            return isBalanced;
        }

        public void setBalanced(boolean balanced) {
            isBalanced = balanced;
        }

        public Map<Integer, List<Integer>> getComponentVertices() {
            return componentVertices;
        }
    }

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
                
                // Skoro partitionGraph zwrócił true, to wiemy że podział jest zbalansowany
                partitionInfo.setBalanced(true);

                // Analyze and store component information
                analyzeAndStoreComponents(graph, partitionInfo);

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

    private static void analyzeAndStoreComponents(Graph graph, PartitionInfo partitionInfo) {
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
    }

    private static void printBalanceAnalysis(PartitionInfo partitionInfo) {
        System.out.printf("--- BALANCE ANALYSIS (Cut %d) ---\n", partitionInfo.getCutNumber());

        Map<Integer, List<Integer>> components = partitionInfo.getComponentVertices();
        
        // Print all component sizes
        for (Map.Entry<Integer, List<Integer>> entry : components.entrySet()) {
            System.out.printf("Component %d size: %d\n", entry.getKey(), entry.getValue().size());
        }

        if (components.size() >= 2) {
            // Znajdź dwa komponenty o najmniejszej sumie rozmiarów
            int smallestSum = Integer.MAX_VALUE;
            int comp1 = -1;
            int comp2 = -1;

            for (int i : components.keySet()) {
                for (int j : components.keySet()) {
                    if (i < j) {  // unikamy porównywania komponentu ze sobą i duplikatów
                        int sum = components.get(i).size() + components.get(j).size();
                        if (sum < smallestSum) {
                            smallestSum = sum;
                            comp1 = i;
                            comp2 = j;
                        }
                    }
                }
            }

            int size1 = components.get(comp1).size();
            int size2 = components.get(comp2).size();
            int diff = Math.abs(size1 - size2);
            int totalSize = size1 + size2;
        
        // Używamy tej samej logiki co w GraphPartitioner
        int allowedMargin = (partitionInfo.getMarginPercent() * totalSize) / 100;

        System.out.printf("Analyzing split result - comparing components from split\n");
        System.out.printf("Size difference: %d (between new components)\n", diff);
        System.out.printf("Components being compared: %d and %d (sizes: %d, %d)\n",
            comp1, comp2, size1, size2);
        System.out.printf("Allowed margin: %d (margin %d%% of total size %d)\n",
            allowedMargin, partitionInfo.getMarginPercent(), totalSize);
        System.out.printf("Balance check: %s (diff %d <= allowed %d)\n", 
            diff <= allowedMargin ? "✓ BALANCED" : "✗ UNBALANCED",
            diff, allowedMargin);
    } else {
        System.out.println("Cannot assess balance - need at least 2 components");
    }
    System.out.println();
}

    private static void printComponentAnalysis(Graph graph, int cutNumber) {
        System.out.printf("--- COMPONENT ANALYSIS (Cut %d) ---\n", cutNumber);
        int[] component = graph.getComponent();
        Map<Integer, List<Integer>> components = new HashMap<>();

        for (int i = 0; i < graph.getNumVertices(); i++) {
            components.computeIfAbsent(component[i], k -> new ArrayList<>()).add(i);
        }

        for (Map.Entry<Integer, List<Integer>> entry : components.entrySet()) {
            System.out.printf("Component %d: %s\n", entry.getKey(), entry.getValue());
        }
        System.out.println();
    }

    private static void printAdjacencyList(Graph graph, String title) {
        System.out.println("--- " + title + " ---");
        List<Integer>[] neighbors = graph.getNeighbors();
        for (int i = 0; i < graph.getNumVertices(); i++) {
            System.out.printf("Vertex %d: %s\n", i, neighbors[i]);
        }
        System.out.println();
    }

    private static void printFinalSummary(List<PartitionInfo> results, int targetCuts) {
        System.out.println("\n=== PARTITIONING SUMMARY ===");
        System.out.printf("Completed %d out of %d planned cuts\n", results.size(), targetCuts);
        System.out.println("\nCut History:");
        for (PartitionInfo info : results) {
            System.out.printf("  Cut %d: %d components, %s\n",
                    info.getCutNumber(),
                    info.getNumComponents(),
                    info.isBalanced() ? "Balanced" : "Unbalanced");
        }
    }
}