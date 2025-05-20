package org.example.io;

import org.example.model.Graph;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GraphLoaderCsrrg {
    private static final int MAX_VERTICES = 4000; // Stała powinna być zdefiniowana w Graph

    public static Graph loadGraph(String filePath) throws IOException {
        Graph graph = new Graph();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Sekcja 1: max_vertices
            int maxVertices = Integer.parseInt(reader.readLine().trim());
            graph.setMaxVertices(maxVertices);
            graph.initializeNeighbors(maxVertices);

            // Sekcja 2: col_index
            int[] colIndex = parseLineToIntArray(reader.readLine());

            // Sekcja 3: row_ptr
            int[] rowPtr = parseLineToIntArray(reader.readLine());
            graph.setCSRData(colIndex, rowPtr);

            // Sekcja 4: group_list
            int[] groupList = parseLineToIntArray(reader.readLine());

            // Sekcja 5: group_ptr
            int[] groupPtr = parseLineToIntArray(reader.readLine());
            graph.setGroupData(groupList, groupPtr);

            // Konwersja do listy sąsiedztwa
            convertCSRToNeighbors(graph);
        }

        return graph;
    }

    private static int[] parseLineToIntArray(String line) {
        String[] parts = line.split(";");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private static void convertCSRToNeighbors(Graph graph) {
        int[] groupList = graph.getGroupList();
        int[] groupPtr = graph.getGroupPtr();
        int[] groupAssignment = graph.getGroupAssignment();

        // Wyczyść istniejące połączenia
        graph.clearNeighbors();

        int totalGroups = 0;
        while (totalGroups < groupPtr.length &&
                (totalGroups == 0 || groupPtr[totalGroups] != 0)) {
            totalGroups++;
        }

        for (int g = 0; g < totalGroups; g++) {
            int startIdx = groupPtr[g];
            int endIdx = (g < totalGroups - 1) ? groupPtr[g + 1] - 1 : groupList.length - 1;
            if (endIdx < startIdx) continue;

            int leader = groupList[startIdx];
            groupAssignment[leader] = g + 1;

            for (int i = startIdx + 1; i <= endIdx; i++) {
                int member = groupList[i];
                graph.addEdge(leader, member);
                groupAssignment[member] = g + 1;
            }
        }
    }
}