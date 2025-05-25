package org.example;
import org.example.model.PartitionResult;
import org.example.algorithm.GraphPartitioner;
import org.example.model.Graph;
import org.example.io.GraphLoaderCsrrg;
import org.example.io.GraphLoaderBin;
import org.example.model.Graph;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import org.example.model.GraphException;
import java.awt.geom.Point2D;
import java.util.Map;

public class MainFrame extends JFrame {
    private List<PartitionResult.PartitionInfo> partitionResults;
    private DetailsUI detailsUI;
    private MainUI mainUI;
    private Graph graph;
    private List<Integer>[] originalNeighbors;
    private PartitionUI partitionUI;
    
    // Dodaj pola dla CardLayout
    private JPanel contentPanel;
    private CardLayout cardLayout;
    
    // Stałe do identyfikacji paneli
    private static final String MAIN_PANEL = "MAIN_PANEL";
    private static final String DETAILS_PANEL = "DETAILS_PANEL";
    private static final String PARTITION_PANEL = "PARTITION_PANEL";

    public void switchToPanel(JPanel newPanel) {
        // Znajdź nazwę panelu
        String panelName = MAIN_PANEL; // domyślnie
        if (newPanel == detailsUI.getPanel()) {
            panelName = DETAILS_PANEL;
        } else if (newPanel == partitionUI.getPanel()) {
            panelName = PARTITION_PANEL;
        }

        cardLayout.show(contentPanel, panelName);
        revalidate();
        repaint();
    }

    // Zostawiamy tylko jedną wersję metody updatePartitionResult
    public void updatePartitionResult(List<PartitionResult.PartitionInfo> newResults) {
        this.partitionResults = newResults;
        if (newResults == null) {
            this.originalNeighbors = null;
            this.graph = null;
            this.detailsUI = null;
            if (this.partitionUI != null) {
                this.partitionUI.clearVisualization();
            }
        } else {
            // Aktualizuj wizualizację w PartitionUI tylko jeśli mamy graf i wyniki
            if (this.partitionUI != null && this.graph != null) {
                this.partitionUI.setGraph(graph, newResults);
            }
        }

        // Aktualizacja detailsUI...
    }

    private Graph originalGraph = null;


    private void copyGraphWithPositions(Graph source, Graph target) {
        // Skopiuj pozycje wierzchołków
        Map<Integer, Point2D> positions = source.getAllVertexPositions();
        target.setAllVertexPositions(positions);
    }

    public MainFrame() {
        setTitle("Aplikacja do podziału grafu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Inicjalizacja CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        setContentPane(contentPanel);

        // Inicjalizacja PartitionUI
        partitionUI = new PartitionUI();
        contentPanel.add(partitionUI.getPanel(), PARTITION_PANEL);

        // Dodaj obsługę przycisku powrotu
        partitionUI.getpartitionBackButton().addActionListener(e -> {
            cardLayout.show(contentPanel, MAIN_PANEL);
            if (originalGraph != null) {
                // Zamiast tworzyć nowy graf, przywróć oryginalny z zachowanymi pozycjami
                mainUI.restoreOriginalGraph(originalGraph);
            }
        });

        // Inicjalizacja komponentów UI
        mainUI = new MainUI(this);

        // Dodanie paneli do CardLayout
        contentPanel.add(mainUI.getPanel(), MAIN_PANEL);

        // Konfiguracja przycisków nawigacyjnych
        setupNavigation();

        // Utworzenie i dodanie menu
        createMenu();

        setSize(800, 600);
        setLocationRelativeTo(null);

        // Pokaż główny panel na starcie
        cardLayout.show(contentPanel, MAIN_PANEL);
    }

    private void setupNavigation() {
        // Przycisk Details
        mainUI.getdetailsButton().addActionListener(e -> {
            if (graph == null) {
                JOptionPane.showMessageDialog(this,
                        "Proszę najpierw wczytać graf.",
                        "Brak grafu",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            this.detailsUI = new DetailsUI(graph, originalNeighbors);
            if (partitionResults != null) {
                detailsUI.setPartitionResults(partitionResults);
            }

            // Dodaj panel details do CardLayout jeśli jeszcze nie został dodany
            contentPanel.add(detailsUI.getPanel(), DETAILS_PANEL);
            cardLayout.show(contentPanel, DETAILS_PANEL);

            detailsUI.getBackButton().addActionListener(ev ->
                cardLayout.show(contentPanel, MAIN_PANEL));
        });

        // Modyfikacja obsługi przycisku Partition
        mainUI.getpostPartitionButton().addActionListener(e -> {
            if (graph == null) {
                JOptionPane.showMessageDialog(this,
                        "Proszę najpierw wczytać graf.",
                        "Brak grafu",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (partitionResults == null || partitionResults.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Proszę najpierw wykonać podział grafu.",
                        "Brak wyników podziału",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Zapisz pozycje przed przełączeniem widoku
            mainUI.saveCurrentPositions();

            // Ustaw graf i wyniki podziału w PartitionUI
            partitionUI.setGraph(graph, partitionResults);

            // Przełącz widok
            cardLayout.show(contentPanel, PARTITION_PANEL);
        });
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
                    originalGraph = new Graph(graph);
                    copyGraphWithPositions(graph, originalGraph);
                    mainUI.setGraph(graph);
                } catch (GraphException ge) {
                    JOptionPane.showMessageDialog(this,
                            "Błąd podczas tworzenia struktury grafu:\n" + ge.getMessage(),
                            "Błąd struktury grafu",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this,
                            "Błąd podczas parsowania liczb w pliku: " + nfe.getMessage(),
                            "Błąd formatu danych",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Nieoczekiwany błąd podczas wczytywania pliku: " + ex.getMessage(),
                            "Błąd wczytywania",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                    // Zachowaj oryginalną listę sąsiedztwa zaraz po wczytaniu
                    originalNeighbors = graph.copyNeighbors();
                    // NIE twórz tutaj obiektu DetailsUI - zostanie on utworzony przy kliknięciu przycisku Details
                    mainUI.setGraph(graph);
                    JOptionPane.showMessageDialog(this,
                            "Wczytano graf z pliku tekstowego:\n" +
                                    selectedFile.getName() + "\n\n",
                            "Graf wczytany pomyślnie",
                            JOptionPane.INFORMATION_MESSAGE);

                // Po udanym wczytaniu grafu:
                if (partitionResults != null) {
                    partitionUI.setGraph(graph, partitionResults);
                } else {
                    partitionUI.clearVisualization();
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
                if (partitionResults != null) {
                    partitionUI.setGraph(graph, partitionResults);
                } else {
                    partitionUI.clearVisualization();
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