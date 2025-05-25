package org.example;
import org.example.algorithm.GraphPartitioner;
import org.example.model.PartitionResult;
import org.example.model.Graph;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import org.example.GraphVisualisation.GraphPrePartitionPanel;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

public class MainUI {
    private JPanel MainPage;
    private JLabel pageTitle;
    private JLabel instructionNumCuts;
    private JLabel instructionMargines;
    private JSpinner spinnerNumCuts;
    private JSpinner spinnerMargines; // Add this field
    private JButton buttonPodziel;
    private GraphPrePartitionPanel graphPrePartitionPanelPlaceholder; // ta sama nazwa, tylko inny typ
    private JLabel successfulCuts;
    private JButton resetujWidokButton;
    private JLabel questionIsGraphBalanced;
    private JLabel isGraphBalanced;  // pole klasy
    private JButton detailsButton;
    private JButton postPartitionButton;
    private Graph graph;
    private MainFrame mainFrame;
    private DetailsUI detailsUI;  // lub dostęp przez mainFrame.getDetailsUI()
    // ... istniejące pola ...
    private Map<Integer, Point2D> savedPrePartitionPositions = new HashMap<>();
    private Map<Integer, Point2D> savedPostPartitionPositions = new HashMap<>();

    public JButton getdetailsButton() {
        return detailsButton;
    }

    public JButton getpostPartitionButton() {
        return postPartitionButton;
    }

    public MainUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        $$$setupUI$$$();
        if (MainPage == null) {
            MainPage = new JPanel();
            MainPage.add(new JLabel("Default Panel Content"));
        }

        // Konfiguracja spinnera dla liczby podziałów
        SpinnerNumberModel spinnerNumCutsModel = new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1);
        spinnerNumCuts.setModel(spinnerNumCutsModel);

        // Konfiguracja spinnera dla marginesu
        SpinnerNumberModel spinnerMarginesModel = new SpinnerNumberModel(10, 0, 100, 1);
        spinnerMargines.setModel(spinnerMarginesModel); // Use the class field instead of finding by index
        buttonPodziel.addActionListener(e -> onPodzielButtonClick());
        resetujWidokButton.addActionListener(e -> onResetujWidokButtonClick());

    }
    public void saveCurrentPositions() {
        if (graphPrePartitionPanelPlaceholder != null) {
            savedPrePartitionPositions = graphPrePartitionPanelPlaceholder.getVertexPositions();
        }
    }

    private void onPodzielButtonClick() {
        try {
            if (graph == null) {
                JOptionPane.showMessageDialog(MainPage,
                        "Najpierw wygeneruj lub wczytaj graf",
                        "Brak grafu",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int margin = (Integer) spinnerMargines.getValue();
            int cuts = (Integer) spinnerNumCuts.getValue();

            // Get results using the static method
            List<PartitionResult.PartitionInfo> results = PartitionResult.performPartitioning(graph, cuts, margin);

            if (results != null && !results.isEmpty()) {
                // Update UI with results
                mainFrame.updatePartitionResult(results);

                // Get the final partition result

                PartitionResult.PartitionInfo lastResult = results.get(results.size() - 1);

                // Update successful cuts count
                successfulCuts.setText(String.valueOf(results.size()));
                GraphPartitioner.findConnectedComponents(graph); // zaktualizuje komponenty
                boolean isConnected = graph.getNumComponents() == results.size()+1;
                // Update balance status
                isGraphBalanced.setText(isConnected? "TAK" : "NIE");
                isGraphBalanced.setForeground(isConnected ? Color.GREEN : Color.RED);

            } else {
                JOptionPane.showMessageDialog(MainPage,
                        "Nie udało się podzielić grafu z podanymi parametrami",
                        "Błąd podziału",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(MainPage,
                    "Wystąpił błąd podczas podziału grafu: " + ex.getMessage(),
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onResetujWidokButtonClick() {
        try {
            if (graph != null) {
                mainFrame.updatePartitionResult(null);
                successfulCuts.setText("...");
                isGraphBalanced.setText("...");
                isGraphBalanced.setForeground(Color.BLACK);
                // Reset spinners to default values
                spinnerNumCuts.setValue(1);
                spinnerMargines.setValue(10);
                
                // Wyczyść zapisane pozycje
                savedPrePartitionPositions.clear();
                savedPostPartitionPositions.clear();
                
                if (graphPrePartitionPanelPlaceholder != null) {
                    graphPrePartitionPanelPlaceholder.clearVisualization();
                }
                graph = null;
                buttonPodziel.setEnabled(false);
                detailsButton.setEnabled(false);
                postPartitionButton.setEnabled(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(MainPage,
                    "Nie wybrano żadnego grafu:" + ex.getMessage(),
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void savePrePartitionPositions() {
        if (graphPrePartitionPanelPlaceholder != null) {
            savedPrePartitionPositions = graphPrePartitionPanelPlaceholder.getVertexPositions();
        }
    }

    public void restorePrePartitionPositions() {
        if (graphPrePartitionPanelPlaceholder != null && !savedPrePartitionPositions.isEmpty()) {
            graphPrePartitionPanelPlaceholder.setVertexPositions(savedPrePartitionPositions);
        }
    }

    // Method to set the graph (call it after generating/loading the graph)
    public void setGraph(Graph graph) {
        this.graph = graph;

        if (graph != null) {
            buttonPodziel.setEnabled(true);
            detailsButton.setEnabled(true);
            postPartitionButton.setEnabled(true);
            successfulCuts.setText("...");
            isGraphBalanced.setText("...");
            isGraphBalanced.setForeground(Color.BLACK);
        
            if (graphPrePartitionPanelPlaceholder != null) {
                // Sprawdź czy mamy zapisane pozycje pasujące do tego grafu
                if (!savedPrePartitionPositions.isEmpty() && 
                    savedPrePartitionPositions.size() == graph.getNumVertices()) {
                    graphPrePartitionPanelPlaceholder.setVertexPositions(savedPrePartitionPositions);
                }
                graphPrePartitionPanelPlaceholder.setGraph(graph);
                graphPrePartitionPanelPlaceholder.revalidate();
                graphPrePartitionPanelPlaceholder.repaint();
            }
        } else {
            buttonPodziel.setEnabled(false);
            detailsButton.setEnabled(false);
        }
    }

    // Dodaj metodę do przywracania pozycji
    public void restoreGraphState() {
        if (graph != null && graphPrePartitionPanelPlaceholder != null && 
            !savedPrePartitionPositions.isEmpty() && 
            savedPrePartitionPositions.size() == graph.getNumVertices()) {
            graphPrePartitionPanelPlaceholder.setVertexPositions(savedPrePartitionPositions);
            graphPrePartitionPanelPlaceholder.setGraph(graph);
        }
    }

    // Przywracanie pozycji gdy wracamy do oryginalnego grafu
    public void restoreOriginalGraph(Graph originalGraph) {
        if (originalGraph != null) {
            this.graph = originalGraph; // używamy oryginalnego grafu, nie jego kopii
            if (graphPrePartitionPanelPlaceholder != null) {
                if (!savedPrePartitionPositions.isEmpty() && 
                    savedPrePartitionPositions.size() == originalGraph.getNumVertices()) {
                    graphPrePartitionPanelPlaceholder.setVertexPositions(savedPrePartitionPositions);
                }
                graphPrePartitionPanelPlaceholder.setGraph(originalGraph);
            }
        }
    }


    public JPanel getPanel() {
        return MainPage;
    }

    private void createUIComponents() {
        MainPage = new JPanel();
        pageTitle = new JLabel();
        instructionNumCuts = new JLabel();
        instructionMargines = new JLabel();
        buttonPodziel = new JButton();

        // Initialize both spinners
        spinnerNumCuts = new JSpinner();
        spinnerMargines = new JSpinner();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        MainPage.setLayout(new GridBagLayout());
        MainPage.setAutoscrolls(true);
        Font MainPageFont = this.$$$getFont$$$(null, -1, 20, MainPage.getFont());
        if (MainPageFont != null) MainPage.setFont(MainPageFont);
        MainPage.setForeground(new Color(-5306302));
        MainPage.setRequestFocusEnabled(false);
        graphPrePartitionPanelPlaceholder = new GraphPrePartitionPanel();
        graphPrePartitionPanelPlaceholder.setLayout(new GridBagLayout());
        graphPrePartitionPanelPlaceholder.setPreferredSize(new Dimension(0, 0));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 11;
        gbc.weightx = 9.0;
        gbc.fill = GridBagConstraints.BOTH;
        MainPage.add(graphPrePartitionPanelPlaceholder, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        MainPage.add(panel1, gbc);
        instructionNumCuts = new JLabel();
        instructionNumCuts.setAlignmentY(0.5f);
        instructionNumCuts.setForeground(new Color(-4516742));
        instructionNumCuts.setHorizontalAlignment(10);
        instructionNumCuts.setHorizontalTextPosition(11);
        instructionNumCuts.setText("liczba przecięć");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 20);
        panel1.add(instructionNumCuts, gbc);
        buttonPodziel = new JButton();
        buttonPodziel.setBackground(new Color(-2192736));
        buttonPodziel.setForeground(new Color(-5306302));
        buttonPodziel.setText("Podziel\uD83D\uDE3C");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 9;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        panel1.add(buttonPodziel, gbc);
        instructionMargines = new JLabel();
        instructionMargines.setForeground(new Color(-4516742));
        instructionMargines.setText("margines %");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 20);
        panel1.add(instructionMargines, gbc);
        spinnerMargines = new JSpinner();
        spinnerMargines.setAutoscrolls(false);
        spinnerMargines.setBackground(new Color(-2192736));
        spinnerMargines.setForeground(new Color(-5306302));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        panel1.add(spinnerMargines, gbc);
        spinnerNumCuts = new JSpinner();
        spinnerNumCuts.setAutoscrolls(false);
        spinnerNumCuts.setBackground(new Color(-2192736));
        spinnerNumCuts.setForeground(new Color(-5306302));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        panel1.add(spinnerNumCuts, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel1.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel1.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel1.add(spacer3, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel1.add(spacer4, gbc);
        final JPanel spacer5 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel1.add(spacer5, gbc);
        final JPanel spacer6 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(spacer6, gbc);
        pageTitle = new JLabel();
        Font pageTitleFont = this.$$$getFont$$$(null, -1, 20, pageTitle.getFont());
        if (pageTitleFont != null) pageTitle.setFont(pageTitleFont);
        pageTitle.setForeground(new Color(-4516742));
        pageTitle.setHorizontalAlignment(0);
        pageTitle.setHorizontalTextPosition(0);
        pageTitle.setIconTextGap(4);
        pageTitle.setText("aplikacja do podziału grafu \uD83D\uDC31 ");
        pageTitle.setVerifyInputWhenFocusTarget(false);
        pageTitle.setVerticalTextPosition(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        MainPage.add(pageTitle, gbc);
        successfulCuts = new JLabel();
        successfulCuts.setText("...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 20);
        MainPage.add(successfulCuts, gbc);
        final JLabel label1 = new JLabel();
        label1.setForeground(new Color(-16098304));
        label1.setText("liczba udanych przecięć grafu");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 20);
        MainPage.add(label1, gbc);
        resetujWidokButton = new JButton();
        resetujWidokButton.setBackground(new Color(-2192736));
        resetujWidokButton.setForeground(new Color(-5306302));
        resetujWidokButton.setText("resetuj widok");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 11;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        MainPage.add(resetujWidokButton, gbc);
        questionIsGraphBalanced = new JLabel();
        questionIsGraphBalanced.setForeground(new Color(-4488797));
        questionIsGraphBalanced.setText("czy graf jest spójny?");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 20);
        MainPage.add(questionIsGraphBalanced, gbc);
        isGraphBalanced = new JLabel();
        isGraphBalanced.setRequestFocusEnabled(true);
        isGraphBalanced.setText("...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 20);
        MainPage.add(isGraphBalanced, gbc);
        detailsButton = new JButton();
        detailsButton.setForeground(new Color(-4488797));
        detailsButton.setText("Szczegóły techniczne");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        MainPage.add(detailsButton, gbc);
        postPartitionButton = new JButton();
        postPartitionButton.setForeground(new Color(-4488797));
        postPartitionButton.setHorizontalAlignment(0);
        postPartitionButton.setText("wizualizacja grafu po podziale");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 20);
        MainPage.add(postPartitionButton, gbc);
        final JPanel spacer7 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.VERTICAL;
        MainPage.add(spacer7, gbc);
        final JPanel spacer8 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.VERTICAL;
        MainPage.add(spacer8, gbc);
        final JPanel spacer9 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.VERTICAL;
        MainPage.add(spacer9, gbc);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return MainPage;
    }

}