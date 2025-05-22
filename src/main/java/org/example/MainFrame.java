package org.example;
import org.example.model.PartitionResult;
import org.example.algorithm.GraphPartitioner;
import org.example.model.Graph;
import org.example.io.GraphLoaderCsrrg;
import org.example.io.GraphLoaderBin;
import org.example.model.Graph;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class MainFrame extends JFrame {
    private List<PartitionResult.PartitionInfo> partitionResults;  // Changed type
    private DetailsUI detailsUI;  // This field exists but isn't being used properly
    private MainUI mainUI;
    private Graph graph;
    private List<Integer>[] originalNeighbors; // Zapisuje oryginalną listę sąsiedztwa


    public void switchToPanel(JPanel newPanel) {
        setContentPane(newPanel);
        revalidate(); // odświeżenie layoutu
        repaint();
    }

    // Zmień metodę updatePartitionResult
    public void updatePartitionResult(List<PartitionResult.PartitionInfo> newResults) {
        this.partitionResults = newResults;
        if (detailsUI != null) {
            detailsUI.setPartitionResults(partitionResults);
            // Jeśli masz oryginalne dane, przekaż je
            if (originalNeighbors != null) {
                detailsUI.setOriginalNeighbors(originalNeighbors);
            }
        }
    }
    // NOWA metoda do obsługi wyniku z oryginalnymi danymi
    public void updatePartitionResultWithOriginal(PartitionResult.PartitioningResult result) {
        this.partitionResults = result.getPartitionInfos();
        this.originalNeighbors = result.getOriginalNeighbors();

        if (detailsUI != null) {
            detailsUI.setPartitionResults(partitionResults);
            detailsUI.setOriginalNeighbors(originalNeighbors);
        }
    }
    public MainFrame() {
        // Podstawowa konfiguracja okna
        setTitle("Aplikacja do podziału grafu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Utworzenie głównego interfejsu
        mainUI = new MainUI(this);
        // In your constructor, modify the detailsButton listener:
        mainUI.getdetailsButton().addActionListener(e -> {
            if (graph == null) {
                JOptionPane.showMessageDialog(this,
                        "Proszę najpierw wczytać graf.",
                        "Brak grafu",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Użyj zachowanej oryginalnej listy sąsiedztwa zamiast tworzyć nową kopię
            this.detailsUI = new DetailsUI(graph, originalNeighbors);  // użyj zapisanej originalNeighbors

            if (partitionResults != null) {
                detailsUI.setPartitionResults(partitionResults);
            }
            switchToPanel(detailsUI.getPanel());

            detailsUI.getBackButton().addActionListener(ev -> {
                switchToPanel(mainUI.getPanel());
            });
        });

        switchToPanel(mainUI.getPanel());


        // Utworzenie i dodanie menu
        createMenu();

        // Ustawienie rozmiaru i wyświetlenie okna
        setSize(800, 600);
        setLocationRelativeTo(null); // centrowanie okna
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Menu Plik
        JMenu fileMenu = new JMenu("Plik");
        menuBar.add(fileMenu);

        // Pozycje menu Plik
        JMenuItem openTxt = new JMenuItem("Otwórz tekstowy");
        JMenuItem openBin = new JMenuItem("Otwórz binarnie");
        fileMenu.add(openTxt);
        fileMenu.add(openBin);
        fileMenu.addSeparator();
        // Menu Pomoc
        JMenu helpMenu = new JMenu("Pomoc");
        menuBar.add(helpMenu);
        //Pozycje menu Pomoc
        JMenuItem openHelp = new JMenuItem("Pomoc w obsłudze");
        JMenuItem openDescription= new JMenuItem ("O programie");
        helpMenu.add(openHelp);
        helpMenu.add(openDescription);


        // Dodanie akcji

        openTxt.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    ".csrrg ", "csrrg");
            fileChooser.setFileFilter(filter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.getName().toLowerCase().endsWith(".csrrg")) {
                    JOptionPane.showMessageDialog(this,
                            "Proszę wybrać plik z rozszerzeniem .csrrg",
                            "Nieprawidłowy typ pliku",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    graph = GraphLoaderCsrrg.loadGraph(selectedFile.getPath());
                    // Zachowaj oryginalną listę sąsiedztwa zaraz po wczytaniu
                    originalNeighbors = graph.copyNeighbors();
                    // NIE twórz tutaj obiektu DetailsUI - zostanie on utworzony przy kliknięciu przycisku Details
                    mainUI.setGraph(graph);
                    JOptionPane.showMessageDialog(this,
                            "Wczytano graf z pliku tekstowego:\n" +
                                    selectedFile.getName() + "\n\n",
                            "Graf wczytany pomyślnie",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Błąd podczas wczytywania grafu: " + ex.getMessage(),
                            "Błąd",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        openBin.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    ".bin ", "bin");
            fileChooser.setFileFilter(filter); // Set the filter
            fileChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.getName().toLowerCase().endsWith(".bin")) {
                    JOptionPane.showMessageDialog(this,
                            "Proszę wybrać plik z rozszerzeniem .bin",
                            "Nieprawidłowy typ pliku",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    graph = GraphLoaderBin.loadGraph(selectedFile.getPath());
                    // Zachowaj oryginalną listę sąsiedztwa zaraz po wczytaniu
                    originalNeighbors = graph.copyNeighbors();
                    // NIE twórz tutaj obiektu DetailsUI
                    mainUI.setGraph(graph);
                    JOptionPane.showMessageDialog(this,
                            "Wczytano graf z pliku binarnego:\n" +
                                    selectedFile.getName() + "\n\n",
                            "Graf wczytany pomyślnie",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Błąd podczas wczytywania grafu: " + ex.getMessage(),
                            "Błąd",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        openHelp.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Instrukcja obsługi programu:\n\n" +
                            "1. Wybierz plik z grafem (.csrrg lub .bin)\n" +
                            "2. (Opcjonalnie) Podaj wartość liczby przecięć grafu\n" +
                            "3. (Opcjonalnie) Podaj wartość marginesu\n" +
                            "4. Kliknij przycisk 'Podziel'\n\n" +
                            "Po wykonaniu powyższych kroków zostanie wyświetlony\n" +
                            "podzielony graf, gdzie każda spójna część grafu\n" +
                            "będzie w innym kolorze.",
                    "Pomoc w obsłudze",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        openDescription.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,"Celem aplikacji jest dokonanie określonej liczby podziałów w taki sposób aby przy każdym podziale liczba wierzchołków otrzymanych dwóch podgrafów była możliwie równa\n"+
                    "(z dopuszczalnym marginesem różnicy) oraz aby liczba przeciętych krawędzi była minimalna.\n"+
                    "Algorytm podziału grafu rozpoczyna się od wyznaczenia wierzchołka centralnego przy użyciu algorytmu Dijkstry, a następnie dzieli graf na dwie grupy metodą DFS,\n"+
                    "dbając o równą liczebność.\n"+
                    "Po podziale sprawdzana jest spójność obu grup — jeśli druga grupa nie jest spójna, zachowywana jest największa jej składowa, a pozostałe wierzchołki są przenoszone do pierwszej grupy.\n"+
                    "Weryfikowany jest też margines wielkości między grupami. Na koniec z każdej grupy tworzony jest oddzielny podgraf zawierający tylko wewnętrzne połączenia.",
                    "O programie",JOptionPane.INFORMATION_MESSAGE);
        });


    }
}