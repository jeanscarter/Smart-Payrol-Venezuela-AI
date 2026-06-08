package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.config.ConfigManager;
import com.nomina.model.Empleado;
import com.nomina.model.ReciboNomina;
import com.nomina.repository.EmpleadoRepository;
import com.nomina.repository.NominaRepository;
import com.nomina.service.PayrollService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de Gestión de Nómina Multi-Moneda.
 * Permite seleccionar el periodo, alternar visualización USD/VES,
 * procesar nóminas y persistirlas en el histórico.
 */
public class NominaPanel extends JPanel {

    private final JComboBox<PeriodoComboItem> cmbPeriodo;
    private final JToggleButton btnVes;
    private final JToggleButton btnUsd;
    private final JLabel lblEstadoPeriodo;
    private final JButton btnProcess;

    // KPI Card Labels
    private final JLabel lblKpiSueldosVal;
    private final JLabel lblKpiCestaVal;
    private final JLabel lblKpiDeduccionesVal;
    private final JLabel lblKpiNetoVal;

    private final DefaultTableModel tableModel;
    private final JTable table;

    // Master-Detail Panel fields
    private final JPanel detailPanel;
    private final JLabel lblDetailName;
    private final JLabel lblDetailCedula;
    private final JTextField txtHorasExtras;
    private final JTextField txtHorasNocturnas;
    private final JTextField txtDiasFeriados;
    private final JTextField txtBonosExtras;
    private final JTextField txtDiasNoTrabajados;
    private final JTextField txtAdelantoVes;
    private final JTextField txtAdelantoUsd;
    private final JButton btnApplyVariables;
    private ReciboNomina reciboSeleccionado = null;

    private List<ReciboNomina> recibosCalculados = new ArrayList<>();
    private boolean verEnVes = true; // Por defecto se visualiza en VES

    private static final String[] COLUMNS = {
            "Cédula", "Empleado", "Sueldo Base", "Cesta Ticket", "Retención IVSS", "Retención FAOV", "Neto a Pagar"
    };

    public NominaPanel() {
        setLayout(new MigLayout("wrap, fillx, insets 24, gapy 16", "[grow, fill]", "[]12[][][grow, fill]"));
        setOpaque(false);

        // --- HEADER SECTION ---
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]12[]", "[]"));
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Cálculo y Procesamiento de Nómina");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        headerPanel.add(title);

        JButton btnExportPdf = new JButton("\uD83D\uDDF3\uFE0F  Generar Recibos PDF");
        btnExportPdf.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnExportPdf.addActionListener(e -> exportPdf());
        headerPanel.add(btnExportPdf);

        btnProcess = new JButton("\u2699  Procesar y Guardar Periodo");
        btnProcess.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold; background: $Button.default.background; foreground: #ffffff");
        btnProcess.addActionListener(e -> procesarYGuardarPeriodo());
        headerPanel.add(btnProcess);

        add(headerPanel, "growx");

        // --- CONTROLS BAR (Period & Currency Selector) ---
        JPanel controlsBar = new JPanel(new MigLayout("insets 12, gap 16", "[]12[]12[]push[]12[]", "[]"));
        controlsBar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        // Period Selector
        controlsBar.add(new JLabel("Periodo Activo:"));
        cmbPeriodo = new JComboBox<>(new DefaultComboBoxModel<>());
        inicializarComboPeriodos();
        cmbPeriodo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cmbPeriodo.addActionListener(e -> recalculate());
        controlsBar.add(cmbPeriodo, "width 280!");

        // Botón Nuevo Período
        JButton btnNuevoPeriodo = new JButton("➕ Nuevo Período");
        btnNuevoPeriodo.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnNuevoPeriodo.addActionListener(e -> abrirDialogoNuevoPeriodo());
        controlsBar.add(btnNuevoPeriodo);

        // Estado del Periodo
        lblEstadoPeriodo = new JLabel("BORRADOR");
        lblEstadoPeriodo.putClientProperty(FlatClientProperties.STYLE, "font: bold -1");
        controlsBar.add(lblEstadoPeriodo, "gapleft 8");

        // Currency Toggle
        controlsBar.add(new JLabel("Visualizar en:"));
        JPanel togglePanel = new JPanel(new GridLayout(1, 2, 0, 0));
        togglePanel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

        btnVes = new JToggleButton("Bs. (VES)", true);
        btnUsd = new JToggleButton("$ (USD)", false);

        ButtonGroup currencyGroup = new ButtonGroup();
        currencyGroup.add(btnVes);
        currencyGroup.add(btnUsd);

        btnVes.addActionListener(e -> {
            verEnVes = true;
            repopulateTable();
            updateKpiCards();
        });
        btnUsd.addActionListener(e -> {
            verEnVes = false;
            repopulateTable();
            updateKpiCards();
        });

        togglePanel.add(btnVes);
        togglePanel.add(btnUsd);
        controlsBar.add(togglePanel);

        add(controlsBar, "growx");

        // --- KPI SUMMARY CARDS ---
        JPanel kpiPanel = new JPanel(new MigLayout("insets 0, gap 16", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        kpiPanel.setOpaque(false);

        lblKpiSueldosVal = new JLabel("0.00");
        lblKpiCestaVal = new JLabel("0.00");
        lblKpiDeduccionesVal = new JLabel("0.00");
        lblKpiNetoVal = new JLabel("0.00");

        kpiPanel.add(buildKpiCard("Total Sueldos Base", lblKpiSueldosVal, "$accentColor"));
        kpiPanel.add(buildKpiCard("Total Cesta Ticket", lblKpiCestaVal, "@accentColor"));
        kpiPanel.add(buildKpiCard("Total Retenciones (IVSS/FAOV)", lblKpiDeduccionesVal, "#e74c3c"));
        kpiPanel.add(buildKpiCard("Neto Total Nómina", lblKpiNetoVal, "$Button.default.background"));

        add(kpiPanel, "growx");

        // --- TABLE ---
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");
        tableContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(38);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold; height: 32; background: $control");

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // --- DETAIL PANEL (Master-Detail) ---
        detailPanel = new JPanel(new MigLayout("wrap, insets 16, gapy 8", "[grow, fill]", "[]4[]12[]4[]4[]4[]4[]4[]4[]4[]4[]4[]4[]4[]12[]"));
        detailPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        JLabel lblDetailTitle = new JLabel("Variables del Período");
        lblDetailTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold +2; foreground: $accentColor");
        detailPanel.add(lblDetailTitle);

        lblDetailName = new JLabel("Seleccione un empleado");
        lblDetailName.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        lblDetailCedula = new JLabel("—");
        lblDetailCedula.putClientProperty(FlatClientProperties.STYLE, "font: -2; foreground: $Label.disabledForeground");

        detailPanel.add(lblDetailName);
        detailPanel.add(lblDetailCedula, "gapbottom 4");

        txtHorasExtras = createLabeledInputField(detailPanel, "Horas Extras (HE):", "Cantidad de horas extras a pagar.");
        txtHorasNocturnas = createLabeledInputField(detailPanel, "Horas Nocturnas (HN):", "Cantidad de horas nocturnas (recargo).");
        txtDiasFeriados = createLabeledInputField(detailPanel, "Días Feriados trabajados:", "Cantidad de días feriados laborados.");
        txtBonosExtras = createLabeledInputField(detailPanel, "Bonificaciones Extras (USD):", "Otros bonos en dólares del período.");
        txtDiasNoTrabajados = createLabeledInputField(detailPanel, "Días NO Trabajados (Inasistencias):", "Días de inasistencia a deducir.");
        txtAdelantoVes = createLabeledInputField(detailPanel, "Adelanto en Bs (VES):", "Adelantos entregados en bolívares.");
        txtAdelantoUsd = createLabeledInputField(detailPanel, "Adelanto en $ (USD):", "Adelantos entregados en dólares.");

        btnApplyVariables = new JButton("Aplicar Variables");
        btnApplyVariables.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold; background: $accentColor; foreground: #ffffff");
        btnApplyVariables.setEnabled(false);
        btnApplyVariables.addActionListener(e -> applySelectedVariables());
        detailPanel.add(btnApplyVariables, "gaptop 8");

        setDetailFieldsEnabled(false);

        // Selection listener for the table
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < recibosCalculados.size()) {
                    reciboSeleccionado = recibosCalculados.get(row);
                } else {
                    reciboSeleccionado = null;
                }
                updateDetailPanel();
            }
        });

        // --- SPLIT CONTAINER FOR MASTER-DETAIL ---
        JPanel contentPanel = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]16[320!, fill]", "[grow, fill]"));
        contentPanel.setOpaque(false);
        contentPanel.add(tableContainer, "grow");
        contentPanel.add(detailPanel, "growy");

        add(contentPanel, "grow, pushy");

        // Inicializar cálculos
        recalculate();

        // Listeners reactivos
        ConfigManager.addListener(this::recalculate);
        EmpleadoRepository.addListener(this::recalculate);
        NominaRepository.addListener(this::actualizarEstadoVisual);
    }

    private JTextField createLabeledInputField(JPanel container, String labelText, String tooltip) {
        JLabel lbl = new JLabel(labelText);
        lbl.putClientProperty(FlatClientProperties.STYLE, "font: $semibold -2");
        lbl.setToolTipText(tooltip);
        JTextField txt = new JTextField();
        txt.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        container.add(lbl, "gapbottom 0");
        container.add(txt, "width 100%, wrap, gapbottom 2");
        return txt;
    }

    private void setDetailFieldsEnabled(boolean enabled) {
        txtHorasExtras.setEnabled(enabled);
        txtHorasNocturnas.setEnabled(enabled);
        txtDiasFeriados.setEnabled(enabled);
        txtBonosExtras.setEnabled(enabled);
        txtDiasNoTrabajados.setEnabled(enabled);
        txtAdelantoVes.setEnabled(enabled);
        txtAdelantoUsd.setEnabled(enabled);
        btnApplyVariables.setEnabled(enabled);
    }

    private void updateDetailPanel() {
        if (reciboSeleccionado == null) {
            lblDetailName.setText("Seleccione un empleado");
            lblDetailCedula.setText("—");
            txtHorasExtras.setText("");
            txtHorasNocturnas.setText("");
            txtDiasFeriados.setText("");
            txtBonosExtras.setText("");
            txtDiasNoTrabajados.setText("");
            txtAdelantoVes.setText("");
            txtAdelantoUsd.setText("");
            setDetailFieldsEnabled(false);
        } else {
            lblDetailName.setText(reciboSeleccionado.getNombreCompleto());
            lblDetailCedula.setText("C.I. " + reciboSeleccionado.getCedula());
            txtHorasExtras.setText(String.format("%.2f", reciboSeleccionado.getHorasExtras()));
            txtHorasNocturnas.setText(String.format("%.2f", reciboSeleccionado.getHorasNocturnas()));
            txtDiasFeriados.setText(String.format("%.2f", reciboSeleccionado.getDiasFeriados()));
            txtBonosExtras.setText(String.format("%.2f", reciboSeleccionado.getBonosExtrasUsd()));
            txtDiasNoTrabajados.setText(String.format("%.2f", reciboSeleccionado.getDiasNoTrabajados()));
            txtAdelantoVes.setText(String.format("%.2f", reciboSeleccionado.getAdelantoVes()));
            txtAdelantoUsd.setText(String.format("%.2f", reciboSeleccionado.getAdelantoUsd()));

            PeriodoComboItem item = (PeriodoComboItem) cmbPeriodo.getSelectedItem();
            boolean yaProcesado = item != null && !NominaRepository.obtenerPorPeriodo(item.id).isEmpty();
            setDetailFieldsEnabled(!yaProcesado);
        }
    }

    private void applySelectedVariables() {
        if (reciboSeleccionado == null) return;
        try {
            double he = Double.parseDouble(txtHorasExtras.getText().trim().replace(",", "."));
            double hn = Double.parseDouble(txtHorasNocturnas.getText().trim().replace(",", "."));
            double df = Double.parseDouble(txtDiasFeriados.getText().trim().replace(",", "."));
            double bonos = Double.parseDouble(txtBonosExtras.getText().trim().replace(",", "."));
            double dnt = Double.parseDouble(txtDiasNoTrabajados.getText().trim().replace(",", "."));
            double adVes = Double.parseDouble(txtAdelantoVes.getText().trim().replace(",", "."));
            double adUsd = Double.parseDouble(txtAdelantoUsd.getText().trim().replace(",", "."));

            if (he < 0 || hn < 0 || df < 0 || bonos < 0 || dnt < 0 || adVes < 0 || adUsd < 0) {
                JOptionPane.showMessageDialog(this, "Todos los valores deben ser números positivos.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Empleado emp = EmpleadoRepository.getAll().stream()
                    .filter(e -> e.getCedula().equalsIgnoreCase(reciboSeleccionado.getCedula()))
                    .findFirst().orElse(null);

            if (emp != null) {
                double tasa = ConfigManager.getTasaBcv();
                PeriodoComboItem item = (PeriodoComboItem) cmbPeriodo.getSelectedItem();
                ReciboNomina nuevoRecibo = PayrollService.calcularReciboDetallado(
                        item.id, emp, tasa, he, hn, df, bonos, dnt, adVes, adUsd
                );

                int index = recibosCalculados.indexOf(reciboSeleccionado);
                if (index >= 0) {
                    recibosCalculados.set(index, nuevoRecibo);
                    reciboSeleccionado = nuevoRecibo;
                }

                repopulateTable();
                updateKpiCards();
                table.setRowSelectionInterval(index, index);
                updateDetailPanel();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese valores numéricos válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalculate() {
        PeriodoComboItem item = (PeriodoComboItem) cmbPeriodo.getSelectedItem();
        if (item == null) return;

        double tasa = ConfigManager.getTasaBcv();
        List<Empleado> empleadosActivos = EmpleadoRepository.getAll().stream()
                .filter(e -> "Activo".equalsIgnoreCase(e.getEstado()))
                .toList();

        List<ReciboNomina> historicos = NominaRepository.obtenerPorPeriodo(item.id);
        if (!historicos.isEmpty()) {
            recibosCalculados = historicos;
        } else {
            recibosCalculados = PayrollService.calcularPeriodo(item.id, empleadosActivos, tasa);
        }

        actualizarEstadoVisual();
        repopulateTable();
        updateKpiCards();

        reciboSeleccionado = null;
        updateDetailPanel();
    }

    private void actualizarEstadoVisual() {
        PeriodoComboItem item = (PeriodoComboItem) cmbPeriodo.getSelectedItem();
        if (item == null) return;

        boolean yaProcesado = !NominaRepository.obtenerPorPeriodo(item.id).isEmpty();
        if (yaProcesado) {
            lblEstadoPeriodo.setText("✓ PROCESADA (Guardada)");
            lblEstadoPeriodo.putClientProperty(FlatClientProperties.STYLE, "font: bold -1; foreground: $Button.default.background");
            btnProcess.setEnabled(false);
        } else {
            lblEstadoPeriodo.setText("⚠ BORRADOR (Sin procesar)");
            lblEstadoPeriodo.putClientProperty(FlatClientProperties.STYLE, "font: bold -1; foreground: #e67e22");
            btnProcess.setEnabled(true);
        }
        lblEstadoPeriodo.repaint();
    }

    private void repopulateTable() {
        tableModel.setRowCount(0);
        for (ReciboNomina r : recibosCalculados) {
            if (verEnVes) {
                tableModel.addRow(new Object[]{
                        r.getCedula(),
                        r.getNombreCompleto(),
                        String.format("Bs. %,.2f", r.getSueldoBasePeriodoVes()),
                        String.format("Bs. %,.2f", r.getCestaTicketVes()),
                        String.format("Bs. %,.2f", r.getIvssVes()),
                        String.format("Bs. %,.2f", r.getFaovVes()),
                        String.format("Bs. %,.2f", r.getNetoVes())
                });
            } else {
                tableModel.addRow(new Object[]{
                        r.getCedula(),
                        r.getNombreCompleto(),
                        String.format("$%,.2f", r.getSueldoBasePeriodoUsd()),
                        String.format("$%,.2f", r.getCestaTicketVes() / r.getTasaBcv()),
                        String.format("$%,.2f", r.getIvssVes() / r.getTasaBcv()),
                        String.format("$%,.2f", r.getFaovVes() / r.getTasaBcv()),
                        String.format("$%,.2f", r.getNetoUsd())
                });
            }
        }
    }

    private void updateKpiCards() {
        double totalSueldos = 0;
        double totalCesta = 0;
        double totalDeducciones = 0;
        double totalNeto = 0;

        for (ReciboNomina r : recibosCalculados) {
            if (verEnVes) {
                totalSueldos += r.getSueldoBasePeriodoVes();
                totalCesta += r.getCestaTicketVes();
                totalDeducciones += (r.getIvssVes() + r.getFaovVes());
                totalNeto += r.getNetoVes();
            } else {
                totalSueldos += r.getSueldoBasePeriodoUsd();
                totalCesta += (r.getCestaTicketVes() / r.getTasaBcv());
                totalDeducciones += ((r.getIvssVes() + r.getFaovVes()) / r.getTasaBcv());
                totalNeto += r.getNetoUsd();
            }
        }

        String prefijo = verEnVes ? "Bs. " : "$";
        lblKpiSueldosVal.setText(String.format("%s%,.2f", prefijo, totalSueldos));
        lblKpiCestaVal.setText(String.format("%s%,.2f", prefijo, totalCesta));
        lblKpiDeduccionesVal.setText(String.format("%s%,.2f", prefijo, totalDeducciones));
        lblKpiNetoVal.setText(String.format("%s%,.2f", prefijo, totalNeto));
    }

    private JPanel buildKpiCard(String title, JLabel lblValue, String colorKey) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 16, gapy 6", "[grow, fill]", "[]8[]"));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: $semibold -1; foreground: $Label.disabledForeground");
        lblValue.putClientProperty(FlatClientProperties.STYLE, "font: bold +6; foreground: " + colorKey);

        card.add(lblTitle);
        card.add(lblValue);
        return card;
    }

    private void procesarYGuardarPeriodo() {
        PeriodoComboItem item = (PeriodoComboItem) cmbPeriodo.getSelectedItem();
        if (item == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de procesar y archivar permanentemente la nómina del periodo:\n" + item.displayName + "?",
                "Procesar Nómina", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        NominaRepository.guardarPeriodo(item.id, recibosCalculados);
        recalculate();

        JOptionPane.showMessageDialog(this,
                "Nómina procesada con éxito y archivada en el histórico local.",
                "Nómina Procesada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportPdf() {
        if (recibosCalculados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay recibos calculados para exportar en este periodo.",
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccione carpeta para guardar los recibos");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File destFolder = chooser.getSelectedFile();
            try {
                com.nomina.service.PdfService.generarRecibos(recibosCalculados, destFolder);
                JOptionPane.showMessageDialog(this,
                        String.format("Se han generado con éxito %d recibos de pago en la carpeta:\n%s",
                                recibosCalculados.size(), destFolder.getAbsolutePath()),
                        "Recibos Generados", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al generar los recibos: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void inicializarComboPeriodos() {
        DefaultComboBoxModel<PeriodoComboItem> model = (DefaultComboBoxModel<PeriodoComboItem>) cmbPeriodo.getModel();
        model.removeAllElements();

        List<String> periodosProcesados = NominaRepository.obtenerPeriodosProcesados();
        if (periodosProcesados.isEmpty()) {
            model.addElement(crearItemDesdeId("2026-06-Q1"));
            model.addElement(crearItemDesdeId("2026-06-Q2"));
            model.addElement(crearItemDesdeId("2026-06-M"));
        } else {
            for (String pid : periodosProcesados) {
                model.addElement(crearItemDesdeId(pid));
            }
        }
    }

    private void abrirDialogoNuevoPeriodo() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nuevo Período", true);
        dialog.setLayout(new MigLayout("wrap 2, insets 16, gapy 12", "[right][grow, fill]"));

        dialog.add(new JLabel("Año:"));
        JSpinner spinnerAnio = new JSpinner(new SpinnerNumberModel(2026, 2000, 2100, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinnerAnio, "#");
        spinnerAnio.setEditor(editor);
        dialog.add(spinnerAnio);

        dialog.add(new JLabel("Mes:"));
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        JComboBox<String> cmbMes = new JComboBox<>(meses);
        cmbMes.setSelectedIndex(5); // Junio por defecto
        dialog.add(cmbMes);

        dialog.add(new JLabel("Tipo:"));
        String[] tipos = {"Primera Quincena", "Segunda Quincena", "Mensual Completo"};
        JComboBox<String> cmbTipo = new JComboBox<>(tipos);
        dialog.add(cmbTipo);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());

        JButton btnAceptar = new JButton("Crear");
        btnAceptar.addActionListener(e -> {
            int anio = (int) spinnerAnio.getValue();
            int mesIdx = cmbMes.getSelectedIndex() + 1;
            String mesStr = String.format("%02d", mesIdx);
            String tipoCode = switch (cmbTipo.getSelectedIndex()) {
                case 0 -> "Q1";
                case 1 -> "Q2";
                default -> "M";
            };

            String nuevoId = anio + "-" + mesStr + "-" + tipoCode;

            DefaultComboBoxModel<PeriodoComboItem> model = (DefaultComboBoxModel<PeriodoComboItem>) cmbPeriodo.getModel();
            boolean existe = false;
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).id.equals(nuevoId)) {
                    cmbPeriodo.setSelectedIndex(i);
                    existe = true;
                    break;
                }
            }

            if (!existe) {
                PeriodoComboItem nuevoItem = crearItemDesdeId(nuevoId);
                model.addElement(nuevoItem);
                cmbPeriodo.setSelectedItem(nuevoItem);
            }

            dialog.dispose();
            recalculate();
        });

        dialog.add(btnCancelar, "split 2, right");
        dialog.add(btnAceptar, "right");

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static PeriodoComboItem crearItemDesdeId(String id) {
        try {
            String[] parts = id.split("-");
            if (parts.length < 3) return new PeriodoComboItem(id, id);
            String anio = parts[0];
            String mesNum = parts[1];
            String tipo = parts[2];

            String mesStr = getNombreMes(mesNum);
            String tipoStr = switch (tipo) {
                case "Q1" -> "Primera Quincena";
                case "Q2" -> "Segunda Quincena";
                case "M" -> "Mensual Completo";
                default -> tipo;
            };

            return new PeriodoComboItem(id, mesStr + " " + anio + " — " + tipoStr);
        } catch (Exception e) {
            return new PeriodoComboItem(id, id);
        }
    }

    private static String getNombreMes(String mesNum) {
        return switch (mesNum) {
            case "01" -> "Enero";
            case "02" -> "Febrero";
            case "03" -> "Marzo";
            case "04" -> "Abril";
            case "05" -> "Mayo";
            case "06" -> "Junio";
            case "07" -> "Julio";
            case "08" -> "Agosto";
            case "09" -> "Septiembre";
            case "10" -> "Octubre";
            case "11" -> "Noviembre";
            case "12" -> "Diciembre";
            default -> mesNum;
        };
    }

    private static class PeriodoComboItem {
        final String id;
        final String displayName;

        PeriodoComboItem(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
