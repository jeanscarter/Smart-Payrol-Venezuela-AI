package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Panel Acerca de (Sobre Nómina Inteligente).
 * Muestra información del software, versión y bases legales (LOTTT, IVSS, Banavih).
 */
public class AcercaPanel extends JPanel {

    public AcercaPanel() {
        setLayout(new MigLayout("wrap, fillx, insets 24, gapy 20", "[center, grow]", "[][grow, fill]"));
        setOpaque(false);

        JPanel card = new JPanel(new MigLayout("wrap, insets 40, gapy 12", "[center, grow]", ""));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 28; background: $control");

        // Icon
        JLabel logoIcon = new JLabel("\uD83D\uDCCA"); // 📊
        logoIcon.setFont(logoIcon.getFont().deriveFont(64f));

        // App title & version
        JLabel title = new JLabel("Nómina Inteligente");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +8");

        JLabel version = new JLabel("Versión 1.0.0 (Fase 2: Main Layout Architecture)");
        version.putClientProperty(FlatClientProperties.STYLE, "font: $semibold; foreground: $Label.disabledForeground");

        // Legal / Reference Text
        JSeparator sep = new JSeparator();
        sep.putClientProperty(FlatClientProperties.STYLE, "height: 1");

        JTextArea txtLegal = new JTextArea(
                "Este sistema está diseñado para cumplir a cabalidad con las normativas laborales de la República Bolivariana de Venezuela, " +
                "incluyendo la Ley Orgánica del Trabajo, los Trabajadores y las Trabajadoras (LOTTT), el Régimen Prestacional de Vivienda y Hábitat (Banavih/FAOV), " +
                "el Seguro Social Obligatorio (IVSS) y el Régimen de Prestaciones de Empleo (LPE/Paro Forzoso).\n\n" +
                "Desarrollado bajo la arquitectura premium Swing + FlatLaf + MigLayout."
        );
        txtLegal.setEditable(false);
        txtLegal.setFocusable(false);
        txtLegal.setWrapStyleWord(true);
        txtLegal.setLineWrap(true);
        txtLegal.setOpaque(false);
        txtLegal.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Label.disabledForeground");
        txtLegal.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Tech Info
        JPanel techPanel = new JPanel(new MigLayout("insets 8, gap 16", "[][]", "[]"));
        techPanel.setOpaque(false);
        techPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel lblJava = new JLabel("☕ Java 21");
        lblJava.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $accentColor");

        JLabel lblFlat = new JLabel("\uD83C\uDFA8 FlatLaf 3.5.4");
        lblFlat.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $Button.default.background");

        techPanel.add(lblJava);
        techPanel.add(lblFlat);

        card.add(logoIcon, "wrap");
        card.add(title, "wrap");
        card.add(version, "wrap, gapbottom 12");
        card.add(sep, "growx, gapbottom 12");
        card.add(txtLegal, "width 480!, wrap");
        card.add(techPanel, "wrap, gaptop 16");

        add(card, "wmin 540, wmax 600");
    }
}
