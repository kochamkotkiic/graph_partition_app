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
        JMenuItem openHelp = new JMenuItem("O programie");
        helpMenu.add(openHelp);


        // Dodanie akcji

        openTxt.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "tylko .csrrg pliki", "csrrg");

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
                    "tylko .bin pliki", "bin");
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
                    "(opcjonalnie) podaj wartość liczby" +
                            " przecięć grafu i marginesu, następnie kliknij przycisk Podziel-zostanie" +
                            " wyświetlony podzielony graf, gdzie każda spójna część grafu będzie w " +
                            " innym kolorze ",
                    "O programie",
                    JOptionPane.INFORMATION_MESSAGE);
        });


    }
}