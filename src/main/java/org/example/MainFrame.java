package org.example;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class MainFrame extends JFrame {
    private MainUI mainUI;

    public MainFrame() {
        // Podstawowa konfiguracja okna
        setTitle("Aplikacja do podziału grafu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Utworzenie głównego interfejsu
        mainUI = new MainUI();
        setContentPane(mainUI.getPanel());

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
            fileChooser.setFileFilter(filter); // Set the filter
            fileChooser.setAcceptAllFileFilterUsed(false); // Disable "All files" option
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                // Here you can add code to handle the selected text file
                JOptionPane.showMessageDialog(this, "Selected file: " + selectedFile.getAbsolutePath());
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
                // Here you can add code to handle the selected binary file
                JOptionPane.showMessageDialog(this, "Selected file: " + selectedFile.getAbsolutePath());
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
                    "O programie",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        openDescription.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,"Celem aplikacji jest dokonanie określonej liczby podziałów w taki sposób aby przy każdym podziale liczba wierzchołków otrzymanych dwóch podgrafów była możliwie równa\n"+
                    "(z dopuszczalnym marginesem różnicy) oraz aby liczba przeciętych krawędzi była minimalna.\n"+
                    "Algorytm podziału grafu rozpoczyna się od wyznaczenia wierzchołka centralnego przy użyciu algorytmu Dijkstry, a następnie dzieli graf na dwie grupy metodą DFS,\n"+
                    "dbając o równą liczebność.\n"+
                    "Po podziale sprawdzana jest spójność obu grup — jeśli druga grupa nie jest spójna, zachowywana jest największa jej składowa, a pozostałe wierzchołki są przenoszone do pierwszej grupy.\n"+
                    "Weryfikowany jest też margines wielkości między grupami. Na koniec z każdej grupy tworzony jest oddzielny podgraf zawierający tylko wewnętrzne połączenia.");
        });


    }
}