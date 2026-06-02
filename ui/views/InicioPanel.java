package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.config.ConfigManager;
import com.nomina.model.Empleado;
import com.nomina.repository.EmpleadoRepository;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel de Inicio (Dashboard) para Nómina Inteligente.
 * Métricas conectadas reactivamente a {@link ConfigManager} y {@link EmpleadoRepository}.
 */
public class InicioPanel extends JPanel {

    private final JLabel lblEmpleadosVal;
    private final JLabel lblNominaVal;
    private final JLabel lblTasaVal;
    private final JLabel lblCestaVal;
    private final ChartPlaceholder chart;

    public InicioPanel() {
        setLayout(new MigLayout("wrap 4, fillx, insets 24, gap 16", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]16[]16[grow, fill]"));
        setOpaque(false);

        JLabel title = new JLabel("Resumen del Negocio");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        add(title, "span 4, wrap");

        // --- METRIC CARDS ---
        lblEmpleadosVal = new JLabel();
        lblEmpleadosVal.putClientProperty(FlatClientProperties.STYLE, "font: bold +12");
        add(buildCard("Total Empleados", "\uD83D\uDC65", lblEmpleadosVal, "Activos en el sistema", "$accentColor"));

        lblNominaVal = new JLabel();
        lblNominaVal.putClientProperty(FlatClientProperties.STYLE, "font: bold +12");
        add(buildCard("Nómina Estimada (USD)", "\uD83D\uDCB5", lblNominaVal, "Periodo actual", "$Button.default.background"));

        lblCestaVal = new JLabel();
        lblCestaVal.putClientProperty(FlatClientProperties.STYLE, "font: bold +12");
        add(buildCard("Cesta Ticket Total", "\uD83C\uDFAB", lblCestaVal, "Monto mensual completo", "@accentColor"));

        lblTasaVal = new JLabel();
        lblTasaVal.putClientProperty(FlatClientProperties.STYLE, "font: bold +12");
        add(buildCard("Tasa de Cambio BCV", "\uD83C\uDFE6", lblTasaVal, "Tasa oficial aplicada", "$Button.default.focusedBackground"));

        // --- CHART ---
        JPanel leftPanel = new JPanel(new MigLayout("wrap, insets 16", "[grow, fill]"));
        leftPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $control");
        JLabel lblChartTitle = new JLabel("Distribución de Costos de Nómina");
        lblChartTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold +2");
        leftPanel.add(lblChartTitle, "gapbottom 16");
        chart = new ChartPlaceholder();
        leftPanel.add(chart, "grow, h 250!");

        // --- ACCIONES RÁPIDAS ---
        JPanel rightPanel = new JPanel(new MigLayout("wrap, insets 16, gapy 12", "[grow, fill]"));
        rightPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $control");
        JLabel lblActionsTitle = new JLabel("Acciones Rápidas");
        lblActionsTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold +2");
        rightPanel.add(lblActionsTitle, "gapbottom 12");

        rightPanel.add(createActionButton("Calcular Nómina Actual", "\u2699"));
        rightPanel.add(createActionButton("Registrar Nuevo Empleado", "\uD83D\uDC64"));
        rightPanel.add(createActionButton("Importar Empleados (.CSV)", "\uD83D\uDCE5"));
        rightPanel.add(createActionButton("Generar Reporte de Ley (FAOV)", "\uD83D\uDCC4"));

        add(leftPanel, "span 3, growy");
        add(rightPanel, "span 1, growy");

        updateDynamicValues();
        ConfigManager.addListener(this::updateDynamicValues);
        EmpleadoRepository.addListener(this::updateDynamicValues);
    }

    private void updateDynamicValues() {
        List<Empleado> empleados = EmpleadoRepository.getAll();
        long activeCount = empleados.stream().filter(e -> "Activo".equalsIgnoreCase(e.getEstado())).count();
        double totalNominaUsd = empleados.stream()
                .filter(e -> "Activo".equalsIgnoreCase(e.getEstado()))
                .mapToDouble(Empleado::getSalarioUsd).sum();
        double totalCesta = activeCount * ConfigManager.getCestaTicket();
        double tasa = ConfigManager.getTasaBcv();

        lblEmpleadosVal.setText(String.valueOf(activeCount));
        lblNominaVal.setText(String.format("$%,.2f", totalNominaUsd));
        lblCestaVal.setText(String.format("Bs. %,.2f", totalCesta));
        lblTasaVal.setText(String.format("Bs. %,.2f", tasa));
        chart.repaint();
    }

    private JPanel buildCard(String titleText, String icon, JLabel valueLabel, String footerText, String accentKey) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 20, gapy 4", "[grow, fill]", "[]8[]8[]"));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $control");

        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        header.setOpaque(false);
        JLabel lblTitle = new JLabel(titleText);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: $semibold; foreground: $Label.disabledForeground");
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(lblIcon.getFont().deriveFont(20f));
        lblIcon.putClientProperty(FlatClientProperties.STYLE, "foreground: " + accentKey);
        header.add(lblTitle);
        header.add(lblIcon);

        JLabel lblFooter = new JLabel(footerText);
        lblFooter.putClientProperty(FlatClientProperties.STYLE, "font: -2; foreground: $Label.disabledForeground");

        card.add(header, "growx");
        card.add(valueLabel);
        card.add(lblFooter);
        return card;
    }

    private JButton createActionButton(String text, String icon) {
        JButton btn = new JButton(icon + "  " + text);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold; margin: 8,12,8,12");
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        return btn;
    }

    private static class ChartPlaceholder extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(UIManager.getColor("Component.borderColor"));
            for (int i = 1; i <= 4; i++) {
                int y = h - (i * h / 5);
                g2.drawLine(40, y, w - 20, y);
            }

            List<Empleado> emps = EmpleadoRepository.getAll().stream()
                    .filter(e -> "Activo".equalsIgnoreCase(e.getEstado())).toList();
            if (emps.isEmpty()) {
                g2.setColor(UIManager.getColor("@foreground"));
                g2.setFont(getFont().deriveFont(12f));
                g2.drawString("Sin datos de empleados activos", w / 2 - 80, h / 2);
                g2.dispose();
                return;
            }

            int barWidth = Math.max(8, (w - 80) / emps.size() - 12);
            Color accent = UIManager.getColor("Component.focusColor");
            if (accent == null) accent = Color.BLUE;

            double maxSalary = emps.stream().mapToDouble(Empleado::getSalarioUsd).max().orElse(1);

            for (int i = 0; i < emps.size(); i++) {
                Empleado emp = emps.get(i);
                int barHeight = (int) ((emp.getSalarioUsd() / maxSalary) * (h - 60));
                if (barHeight < 5) barHeight = 5;

                int x = 50 + i * (barWidth + 12);
                int y = h - 35 - barHeight;

                GradientPaint gp = new GradientPaint(x, y, accent, x, y + barHeight, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

                g2.setColor(UIManager.getColor("@foreground"));
                g2.setFont(getFont().deriveFont(9f));
                FontMetrics fm = g2.getFontMetrics();
                String label = emp.getNombreCompleto().split(" ")[0];
                int labelWidth = fm.stringWidth(label);
                g2.drawString(label, x + (barWidth - labelWidth) / 2, h - 15);
            }

            g2.dispose();
        }
    }
}
