package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.repository.NominaRepository;
import com.nomina.service.LegalReportService;
import com.nomina.service.ContabilidadService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Panel de Generación de Reportes Legales y de Control Interno.
 * Integración completa con FAOV, IVSS, Prestaciones Sociales, ARCV y Asientos Contables.
 */
public class ReportesPanel extends JPanel {

    public ReportesPanel() {
        setLayout(new MigLayout("wrap 3, fillx, insets 24, gap 16", "[grow, fill][grow, fill][grow, fill]", "[]16[]16[grow, fill]"));
        setOpaque(false);

        // --- TITLE ---
        JLabel title = new JLabel("Reportes y Obligaciones de Ley");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        add(title, "span 3, wrap");

        // --- REPORT CARDS ---
        add(createReportCard(
                "FAOV Banavih",
                "Formato mensual exigido por Banavih para el Fondo de Ahorro Obligatorio para la Vivienda. Consolida los periodos del mes.",
                "\uD83C\uDFE0",
                "Generar Archivo .TXT",
                this::exportarFaov
        ));

        add(createReportCard(
                "IVSS - Forma 14-02",
                "Listado oficial de trabajadores para Registro o Retiro ante el Instituto Venezolano de los Seguros Sociales.",
                "\uD83D\uDCCB",
                "Exportar TXT Oficial",
                this::exportarIvss
        ));

        add(createReportCard(
                "IVSS - Providencia 003",
                "Registro patronal de asegurados activos, detallando cotizaciones del 4% del trabajador y 9% del patrono.",
                "\uD83D\uDEE1️",
                "Exportar Providencia 003",
                this::exportarIvssProvidencia003
        ));

        add(createReportCard(
                "Prestaciones con Intereses",
                "Historial de prestaciones sociales acumuladas según Art. 142/143 LOTTT, detallando garantía, antigüedad e intereses.",
                "\uD83D\uDCB8",
                "Ver Detalle y Exportar",
                this::verPrestaciones
        ));

        add(createReportCard(
                "Contribución de Pensiones",
                "Aporte patronal especial (decreto oficial SENIAT) sobre la base salarial integral para la protección de pensiones.",
                "\uD83C\uDFE2",
                "Generar Declaración SENIAT",
                this::exportarPensiones
        ));

        add(createReportCard(
                "ARCV (ISLR)",
                "Comprobante anual acumulado de retención de Impuesto sobre la Renta para personas naturales residentes.",
                "\uD83D\uDCCA",
                "Generar Comprobantes",
                this::exportarArcv
        ));

        add(createReportCard(
                "Resumen Contable quincenal",
                "Asiento contable agrupado por departamento para integración directa con sistemas ERP corporativos.",
                "\uD83D\uDCD4",
                "Generar Asiento Contable",
                this::exportarAsientoContable
        ));

        add(createReportCard(
                "Trimestral MinPPTRASS",
                "Declaración trimestral obligatoria de empleo, salarios y horas extras ante el Ministerio del Trabajo.",
                "\uD83C\uDFDB️",
                "Generar Declaración",
                this::exportarMinPPTRASS
        ));
    }

    private JPanel createReportCard(String title, String description, String icon, String actionText, Runnable action) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 20, gapy 12", "[grow, fill]", "[][grow, fill][]"));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $control");

        JPanel header = new JPanel(new MigLayout("insets 0, gap 12", "[]12[]", "[]"));
        header.setOpaque(false);
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(lblIcon.getFont().deriveFont(24f));
        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold +1");
        header.add(lblIcon);
        header.add(lblTitle);

        JTextArea txtDesc = new JTextArea(description);
        txtDesc.setEditable(false);
        txtDesc.setFocusable(false);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setLineWrap(true);
        txtDesc.setOpaque(false);
        txtDesc.putClientProperty(FlatClientProperties.STYLE, "font: -1; foreground: $Label.disabledForeground");

        JButton btnAction = new JButton(actionText);
        btnAction.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnAction.addActionListener(e -> action.run());

        card.add(header, "growx");
        card.add(txtDesc, "growx, pushy");
        card.add(btnAction, "growx");

        return card;
    }

    // ════════════════════════════════════════════════════════════════
    //  FAOV
    // ════════════════════════════════════════════════════════════════

    private void exportarFaov() {
        List<String> periodos = NominaRepository.obtenerPeriodosProcesados();
        List<String> meses = periodos.stream()
                .filter(p -> p.length() >= 7)
                .map(p -> p.substring(0, 7))
                .distinct()
                .sorted()
                .toList();

        if (meses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay nóminas procesadas en el histórico.\nPor favor procese una nómina en el panel de 'Nómina' primero.",
                    "Sin datos para FAOV",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel dialogPanel = new JPanel(new MigLayout("wrap, insets 8", "[grow, fill]", "[]8[]"));
        dialogPanel.add(new JLabel("Seleccione el mes a declarar en el BANAVIH:"));
        JComboBox<String> cmbMeses = new JComboBox<>(meses.toArray(new String[0]));
        cmbMeses.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        dialogPanel.add(cmbMeses);

        int option = JOptionPane.showConfirmDialog(this, dialogPanel,
                "Generar Declaración FAOV", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String mesSeleccionado = (String) cmbMeses.getSelectedItem();
        if (mesSeleccionado == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte FAOV Banavih");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("FAOV_Banavih_" + mesSeleccionado.replace("-", "_") + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            LegalReportService.exportarFaovAArchivo(mesSeleccionado, path);
            JOptionPane.showMessageDialog(this, "Reporte FAOV generado con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  IVSS
    // ════════════════════════════════════════════════════════════════

    private void exportarIvss() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte IVSS Forma 14-02");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("IVSS_Forma1402.txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            LegalReportService.exportarIvssAArchivo(path);
            JOptionPane.showMessageDialog(this, "Reporte IVSS Forma 14-02 generado con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Exportación", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarIvssProvidencia003() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Registro Providencia 003");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("IVSS_Providencia003.txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            LegalReportService.exportarIvssProvidencia003AArchivo(path);
            JOptionPane.showMessageDialog(this, "Registro Providencia 003 generado con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Exportación", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PRESTACIONES SOCIALES
    // ════════════════════════════════════════════════════════════════

    private void verPrestaciones() {
        List<LegalReportService.PrestacionesDetalladasResult> resultados = LegalReportService.calcularPrestacionesDetalladas();

        if (resultados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay empleados activos para calcular prestaciones.",
                    "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Diálogo con tabla detallada (incluye intereses según Art. 143 LOTTT)
        String[] columns = {"Cédula", "Empleado", "Ingreso", "Años", "Trim.",
                "Días Gar.", "Monto Gar. (Bs.)", "Días Ant.", "Monto Ant. (Bs.)", "Intereses (Bs.)", "Total (Bs.)", "Total (USD)"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (LegalReportService.PrestacionesDetalladasResult r : resultados) {
            model.addRow(new Object[]{
                    r.cedula(),
                    r.nombre(),
                    r.fechaIngreso(),
                    r.anosServicio(),
                    r.trimestres(),
                    String.format("%.0f", r.diasGarantia()),
                    String.format("Bs. %,.2f", r.montoGarantiaVes()),
                    String.format("%.0f", r.diasAntiguedad()),
                    String.format("Bs. %,.2f", r.montoAntiguedadVes()),
                    String.format("Bs. %,.2f", r.interesesVes()),
                    String.format("Bs. %,.2f", r.totalVes()),
                    String.format("$%,.2f", r.totalUsd())
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(1000, 350));

        JPanel dialogPanel = new JPanel(new MigLayout("wrap, insets 0", "[grow, fill]", "[]8[]"));
        dialogPanel.add(scrollPane, "grow");

        JButton btnExport = new JButton("Exportar a archivo .TXT");
        btnExport.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");

        dialogPanel.add(btnExport, "right");

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Libro de Prestaciones Sociales con Intereses (Art. 142/143 LOTTT)");
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(dialogPanel);
        dialog.setSize(1050, 480);
        dialog.setLocationRelativeTo(this);

        btnExport.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar Acumulado de Prestaciones Detalladas");
            fc.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
            fc.setSelectedFile(new File("Prestaciones_Detalladas_LOTTT.txt"));

            int sel = fc.showSaveDialog(dialog);
            if (sel != JFileChooser.APPROVE_OPTION) return;

            String path = ensureTxtExtension(fc.getSelectedFile().getAbsolutePath());

            try {
                LegalReportService.exportarPrestacionesDetalladasAArchivo(path);
                JOptionPane.showMessageDialog(dialog,
                        "Historial detallado de prestaciones exportado con éxito en:\n" + path,
                        "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════
    //  PENSIONES
    // ════════════════════════════════════════════════════════════════

    private void exportarPensiones() {
        List<String> periodos = NominaRepository.obtenerPeriodosProcesados();
        List<String> meses = periodos.stream()
                .filter(p -> p.length() >= 7)
                .map(p -> p.substring(0, 7))
                .distinct()
                .sorted()
                .toList();

        if (meses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay nóminas procesadas en el histórico.\nPor favor procese una nómina en el panel de 'Nómina' primero.",
                    "Sin datos para Pensiones",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel dialogPanel = new JPanel(new MigLayout("wrap, insets 8", "[grow, fill]", "[]8[]"));
        dialogPanel.add(new JLabel("Seleccione el mes a declarar (SENIAT):"));
        JComboBox<String> cmbMeses = new JComboBox<>(meses.toArray(new String[0]));
        cmbMeses.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        dialogPanel.add(cmbMeses);

        int option = JOptionPane.showConfirmDialog(this, dialogPanel,
                "Generar Declaración de Pensiones", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String mesSeleccionado = (String) cmbMeses.getSelectedItem();
        if (mesSeleccionado == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Declaración de Pensiones");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("Pensiones_SENIAT_" + mesSeleccionado.replace("-", "_") + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            LegalReportService.exportarPensionesAArchivo(mesSeleccionado, path);
            JOptionPane.showMessageDialog(this, "Declaración especial de pensiones generada con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ARCV (ISLR)
    // ════════════════════════════════════════════════════════════════

    private void exportarArcv() {
        int yearActual = Calendar.getInstance().get(Calendar.YEAR);
        String strYear = JOptionPane.showInputDialog(this, "Ingrese el Año Fiscal a Declarar:", String.valueOf(yearActual));
        if (strYear == null || strYear.trim().isEmpty()) return;

        int anio;
        try {
            anio = Integer.parseInt(strYear.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El año ingresado no es válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Comprobantes ARCV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("ARCV_ISLR_" + anio + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            LegalReportService.exportarArcvAArchivo(anio, path);
            JOptionPane.showMessageDialog(this, "Comprobantes de retención ARCV generados con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ASIENTO CONTABLE
    // ════════════════════════════════════════════════════════════════

    private void exportarAsientoContable() {
        List<String> periodos = NominaRepository.obtenerPeriodosProcesados();
        if (periodos.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay nóminas procesadas en el histórico.\nPor favor procese una nómina en el panel de 'Nómina' primero.",
                    "Sin datos para Contabilidad",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel dialogPanel = new JPanel(new MigLayout("wrap, insets 8", "[grow, fill]", "[]8[]"));
        dialogPanel.add(new JLabel("Seleccione el periodo de la quincena/mes:"));
        JComboBox<String> cmbPeriodos = new JComboBox<>(periodos.toArray(new String[0]));
        cmbPeriodos.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        dialogPanel.add(cmbPeriodos);

        int option = JOptionPane.showConfirmDialog(this, dialogPanel,
                "Generar Asiento Contable", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String periodoSeleccionado = (String) cmbPeriodos.getSelectedItem();
        if (periodoSeleccionado == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Asiento Contable");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("Asiento_Contable_" + periodoSeleccionado + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            ContabilidadService.exportarAsientoContable(periodoSeleccionado, path);
            JOptionPane.showMessageDialog(this, "Asiento contable de nómina generado con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  DECLARACIÓN TRIMESTRAL MinPPTRASS
    // ════════════════════════════════════════════════════════════════

    private void exportarMinPPTRASS() {
        int yearActual = Calendar.getInstance().get(Calendar.YEAR);
        
        JPanel panel = new JPanel(new MigLayout("wrap 2, insets 10", "[grow, fill][grow, fill]", "[]8[]"));
        panel.add(new JLabel("Año Fiscal:"));
        JTextField txtYear = new JTextField(String.valueOf(yearActual));
        txtYear.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        panel.add(txtYear);
        
        panel.add(new JLabel("Trimestre:"));
        JComboBox<String> cmbTrimestre = new JComboBox<>(new String[]{
                "Trimestre I (Ene - Mar)",
                "Trimestre II (Abr - Jun)",
                "Trimestre III (Jul - Sep)",
                "Trimestre IV (Oct - Dic)"
        });
        cmbTrimestre.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        panel.add(cmbTrimestre);

        int option = JOptionPane.showConfirmDialog(this, panel,
                "Generar Declaración Trimestral MinPPTRASS", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        String strYear = txtYear.getText();
        if (strYear == null || strYear.trim().isEmpty()) return;

        int anio;
        try {
            anio = Integer.parseInt(strYear.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El año ingresado no es válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int trimestre = cmbTrimestre.getSelectedIndex() + 1;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Declaración MinPPTRASS");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("MinPPTRASS_Trimestre_" + trimestre + "_" + anio + ".txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        String path = ensureTxtExtension(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            ContabilidadService.exportarTrimestralMinPPTRASS(anio, trimestre, path);
            JOptionPane.showMessageDialog(this, "Declaración Trimestral MinPPTRASS generada con éxito en:\n" + path,
                    "Archivo Guardado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private String ensureTxtExtension(String path) {
        if (!path.toLowerCase().endsWith(".txt")) {
            path += ".txt";
        }
        return path;
    }
}
