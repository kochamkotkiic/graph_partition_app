package org.example.io;

import org.example.model.Graph;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GraphLoaderBin {

    public static Graph loadGraph(String filePath) throws IOException {
        Graph graph = new Graph();

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {

            // maxVerticesInRow
            int maxVertices = readUInt32(dis);
            graph.setMaxVertices(maxVertices); // Możesz nadpisać później, jeśli chcesz ustawić rzeczywistą sumę wierzchołków

            // vertices_by_rows (pomijamy, niepotrzebne do sąsiedztwa)
            int vertexRowListLength = readUInt32(dis);
            dis.skipBytes(vertexRowListLength * 4);

            // row_indexes (pomijamy, niepotrzebne do sąsiedztwa)
            int rowIndexListLength = readUInt32(dis);
            dis.skipBytes(rowIndexListLength * 4);

            // liczba grafów
            int graphCount = readUInt32(dis);

            // Dane do scalania
            int totalVertices = 0;
            int totalAdjLen = 0;
            int[][] allAdj = new int[graphCount][];
            int[][] allAdjIndex = new int[graphCount][];
            int[] vertexOffsets = new int[graphCount];

            // Wczytaj dane wszystkich grafów
            for (int g = 0; g < graphCount; g++) {
                int vertexCount = readUInt32(dis);
                int edgeCount = readUInt32(dis); // nieużywane

                int adjLen = readUInt32(dis);
                int[] adj = new int[adjLen];
                for (int i = 0; i < adjLen; i++) {
                    adj[i] = readUInt32(dis);
                }

                int adjIndexLen = readUInt32(dis);
                int[] adjIndex = new int[adjIndexLen];
                for (int i = 0; i < adjIndexLen; i++) {
                    adjIndex[i] = readUInt32(dis);
                }

                // Zapamiętaj offsety
                vertexOffsets[g] = totalVertices;
                totalVertices += vertexCount;
                totalAdjLen += adjLen;

                allAdj[g] = adj;
                allAdjIndex[g] = adjIndex;
            }

            // Tworzymy scalone struktury
            int[] mergedAdj = new int[totalAdjLen];
            int[] mergedAdjIndex = new int[totalVertices + 1];

            int adjPos = 0;
            int vertexPos = 0;

            for (int g = 0; g < graphCount; g++) {
                int[] adj = allAdj[g];
                int[] adjIdx = allAdjIndex[g];
                int vOffset = vertexOffsets[g];

                for (int v = 0; v < adjIdx.length - 1; v++) {
                    int start = adjIdx[v];
                    int end = adjIdx[v + 1];
                    mergedAdjIndex[vertexPos + v] = adjPos;

                    for (int i = start; i < end; i++) {
                        // dodajemy offset do sąsiada
                        mergedAdj[adjPos++] = adj[i] + vOffset;
                    }
                }
                vertexPos += allAdjIndex[g].length - 1;
            }
            mergedAdjIndex[totalVertices] = totalAdjLen;

            // Ustaw dane CSR w grafie
            graph.setCSRData(mergedAdj, mergedAdjIndex);

            // Konwersja do listy sąsiedztwa
            convertCSRToNeighbors(graph);
        }

        return graph;
    }


    private static void convertCSRToNeighbors(Graph graph) {
        graph.clearNeighbors();

        int[] colIndex = graph.getColIndex();
        int[] rowPtr = graph.getRowPtr();

        for (int i = 0; i < rowPtr.length - 1; i++) {
            for (int j = rowPtr[i]; j < rowPtr[i + 1]; j++) {
                int neighbor = colIndex[j];
                graph.addEdge(i, neighbor); // zakładamy nieskierowany graf
            }
        }
    }

    private static int readUInt32(DataInputStream dis) throws IOException {
        byte[] buffer = new byte[4];
        dis.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
