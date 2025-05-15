package org.example;

import javax.swing.*;
import javax.swing.JFrame;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Graph Partitioning");
        main_ui form = new main_ui();
        frame.setContentPane(form.getPanel());
    }
}