package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.config.ConfigManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Panel de Configuración de Parámetros Globales.
 * Ajuste de Tasa de Cambio BCV, Cesta Ticket, porcentaje de aportes legales, etc.
 * Conectado reactivamente con {@link ConfigManager}.
 */
public class ConfiguracionPanel extends JPanel {

    private final JTextField txtRif;
    private final JTextField txtTasa;
    private final JTextField txtCesta;
    private final JTextField txtCestaFijaUsd;
    private final JTextField txtSalarioMinimo;
    private final JTextField txtIvss;
    private final JTextField txtFaov;
    private final JTextField txtFaovPatron;
    private final JTextField txtPensionPatronal;
    private final JTextField txtProporcion;

    public ConfiguracionPanel() {
        setLayout(new MigLayout("wrap, fillx, insets 24, gapy 20", "[grow, fill]", "[]12[][grow, fill]"));
        setOpaque(false);

        // --- TITLE ---
        JLabel title = new JLabel("Configuración del Sistema");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        add(title, "growx");

        // --- MAIN CONFIGURATION CARD ---
        JPanel card = new JPanel(new MigLayout("wrap 2, insets 24, gapy 16, gapx 32", "[grow, fill][grow, fill]", ""));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $control");

        // Section 1: Divisas y Conversión
        JLabel sec1Title = new JLabel("Parámetros de Conversión y Divisas");
        sec1Title.putClientProperty(FlatClientProperties.STYLE, "font: bold +2; foreground: $accentColor");
        card.add(sec1Title, "span 2, wrap, gapbottom 8");

        card.add(createFieldLabel("RIF del Patrono (Gubernamental):", "Registro de Información Fiscal de la empresa (ej. J-30001234-5)."));
        txtRif = new JTextField();
        txtRif.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtRif, "width 180!, right, wrap");

        card.add(createFieldLabel("Tasa de Cambio Oficial BCV (VES / USD):", "Fijada para los cálculos de conversión de salarios base."));
        txtTasa = new JTextField();
        txtTasa.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtTasa, "width 180!, right, wrap");

        card.add(createFieldLabel("Monto Cesta Ticket Mensual (VES):", "Establecido por decreto presidencial oficial (Gaceta Oficial)."));
        txtCesta = new JTextField();
        txtCesta.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtCesta, "width 180!, right, wrap");

        card.add(createFieldLabel("Monto Cesta Ticket Fijo Quincenal (USD):", "Cesta ticket base en USD por quincena (ej. $20.00)."));
        txtCestaFijaUsd = new JTextField();
        txtCestaFijaUsd.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtCestaFijaUsd, "width 180!, right, wrap");

        card.add(createFieldLabel("Salario Mínimo Nacional (VES):", "Salario mínimo oficial utilizado para topes de retenciones (ej. Bs. 130.00)."));
        txtSalarioMinimo = new JTextField();
        txtSalarioMinimo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtSalarioMinimo, "width 180!, right, wrap");

        // Section 2: Deducciones Legales
        JSeparator sep = new JSeparator();
        card.add(sep, "span 2, growx, gaptop 8, gapbottom 8, wrap");

        JLabel sec2Title = new JLabel("Deducciones, Aportes Patronales y Proporción");
        sec2Title.putClientProperty(FlatClientProperties.STYLE, "font: bold +2; foreground: $accentColor");
        card.add(sec2Title, "span 2, wrap, gapbottom 8");

        card.add(createFieldLabel("Retención IVSS Trabajador (%):", "Descuento quincenal/mensual del Seguro Social Obligatorio (Límite 5 Salarios Mínimos)."));
        txtIvss = new JTextField();
        txtIvss.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtIvss, "width 180!, right, wrap");

        card.add(createFieldLabel("Retención FAOV Trabajador (%):", "Fondo de Ahorro Obligatorio para la Vivienda (1% retenido al empleado)."));
        txtFaov = new JTextField();
        txtFaov.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtFaov, "width 180!, right, wrap");

        card.add(createFieldLabel("Aporte FAOV Patronal (%):", "Aporte obligatorio mensual realizado por el empleador (2%)."));
        txtFaovPatron = new JTextField();
        txtFaovPatron.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtFaovPatron, "width 180!, right, wrap");

        card.add(createFieldLabel("Contribución de Pensiones Patronal (%):", "Contribución patronal para la protección de pensiones (ej. 9.0%)."));
        txtPensionPatronal = new JTextField();
        txtPensionPatronal.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtPensionPatronal, "width 180!, right, wrap");

        card.add(createFieldLabel("Proporción de Pago Quincenal (%):", "Porcentaje de salario pagado por quincena (ej. 50.0% equivale a 0.50)."));
        txtProporcion = new JTextField();
        txtProporcion.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        card.add(txtProporcion, "width 180!, right, wrap");

        // Cargar valores iniciales de la configuración
        loadValues();

        // Action Buttons
        JPanel actionsPanel = new JPanel(new MigLayout("insets 0", "push[]12[]", "[]"));
        actionsPanel.setOpaque(false);

        JButton btnCancel = new JButton("Descartar");
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnCancel.addActionListener(e -> loadValues());

        JButton btnSave = new JButton("Guardar Parámetros");
        btnSave.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold; background: $accentColor; foreground: #ffffff");
        btnSave.addActionListener(e -> saveValues());

        actionsPanel.add(btnCancel);
        actionsPanel.add(btnSave);

        card.add(actionsPanel, "span 2, growx, gaptop 16");

        add(card, "growx");
    }

    private void loadValues() {
        txtRif.setText(ConfigManager.getRifPatrono());
        txtTasa.setText(String.format("%.2f", ConfigManager.getTasaBcv()));
        txtCesta.setText(String.format("%.2f", ConfigManager.getCestaTicket()));
        txtCestaFijaUsd.setText(String.format("%.2f", ConfigManager.getCestaTicketFijaUsd()));
        txtSalarioMinimo.setText(String.format("%.2f", ConfigManager.getSalarioMinimo()));
        txtIvss.setText(String.format("%.1f", ConfigManager.getIvssTrabajador()));
        txtFaov.setText(String.format("%.1f", ConfigManager.getFaovTrabajador()));
        txtFaovPatron.setText(String.format("%.1f", ConfigManager.getFaovPatronal()));
        txtPensionPatronal.setText(String.format("%.1f", ConfigManager.getPensionPatronal()));
        txtProporcion.setText(String.format("%.1f", ConfigManager.getProporcionPago() * 100.0));
    }

    private void saveValues() {
        try {
            String rif = txtRif.getText().trim();
            double tasa = parseDouble(txtTasa.getText());
            double cesta = parseDouble(txtCesta.getText());
            double cestaFija = parseDouble(txtCestaFijaUsd.getText());
            double salarioMin = parseDouble(txtSalarioMinimo.getText());
            double ivss = parseDouble(txtIvss.getText());
            double faov = parseDouble(txtFaov.getText());
            double faovPatron = parseDouble(txtFaovPatron.getText());
            double pension = parseDouble(txtPensionPatronal.getText());
            double proporcion = parseDouble(txtProporcion.getText()) / 100.0;

            if (rif.isEmpty()) {
                showError("El RIF del Patrono es obligatorio.");
                return;
            }

            if (tasa <= 0 || cesta < 0 || cestaFija < 0 || salarioMin < 0 || ivss < 0 || faov < 0 || faovPatron < 0 || pension < 0 || proporcion < 0) {
                showError("Todos los parámetros deben ser valores numéricos positivos.");
                return;
            }

            ConfigManager.setRifPatrono(rif);
            ConfigManager.setTasaBcv(tasa);
            ConfigManager.setCestaTicket(cesta);
            ConfigManager.setCestaTicketFijaUsd(cestaFija);
            ConfigManager.setSalarioMinimo(salarioMin);
            ConfigManager.setIvssTrabajador(ivss);
            ConfigManager.setFaovTrabajador(faov);
            ConfigManager.setFaovPatronal(faovPatron);
            ConfigManager.setPensionPatronal(pension);
            ConfigManager.setProporcionPago(proporcion);
            ConfigManager.save();

            JOptionPane.showMessageDialog(this,
                    "Configuraciones guardadas y aplicadas con éxito.",
                    "Parámetros Guardados",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            showError("Por favor ingrese valores numéricos válidos. Use coma o punto decimal.");
        }
    }

    private double parseDouble(String text) throws NumberFormatException {
        // Soporta formatos locales con coma o punto
        String cleaned = text.trim().replace(",", ".");
        return Double.parseDouble(cleaned);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Error de Validación",
                JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createFieldLabel(String labelText, String descText) {
        JPanel p = new JPanel(new MigLayout("wrap, insets 0, gapy 2", "[grow, fill]", ""));
        p.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.putClientProperty(FlatClientProperties.STYLE, "font: $semibold");
        JLabel desc = new JLabel(descText);
        desc.putClientProperty(FlatClientProperties.STYLE, "font: -2; foreground: $Label.disabledForeground");
        p.add(label);
        p.add(desc);
        return p;
    }
}
