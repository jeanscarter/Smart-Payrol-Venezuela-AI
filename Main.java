package com.nomina;

import com.nomina.theme.ThemeManager;
import com.nomina.ui.MainFrame;

import javax.swing.*;

/**
 * Punto de entrada de la aplicación Nómina Inteligente.
 * Inicializa el tema FlatLaf y construye el frame principal en el EDT.
 */
public class Main {

    public static void main(String[] args) {
        // Inicializar el sistema de temas antes de cualquier componente Swing
        ThemeManager.init();

        // Construir la UI en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
