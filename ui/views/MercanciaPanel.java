package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.model.Empleado;
import com.nomina.model.FacturaMercancia;
import com.nomina.repository.EmpleadoRepository;
import com.nomina.repository.MercanciaRepository;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de Deducción de Mercancía (Medicamentos/Farmacia).
 * Permite registrar facturas pendientes de cobro a empleados, registrar abonos,
 * postergar pagos de forma excepcional y visualizar el estado mediante un diseño maestro-detalle.
 */
public class MercanciaPanel extends JPanel {

    // KPI Card Labels
    private final JLabel lblKpiPendientesVal;
    private final JLabel lblKpiAdeudadoVal;
    private final JLabel lblKpiVencidasVal;
    private final JLabel lblKpiPostergadasVal;

    // Table
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<FacturaMercancia> facturasFiltradas = new ArrayList<>();

    // Filters
    private final JComboBox<Object> cmbFiltroEmpleado;
    private final JComboBox<String> cmbFiltroEstado;

    // Detail Panel fields
    private final JPanel detailPanel;
    private final JLabel lblDetalleFactura;
    private final JLabel lblDetalleEmpleado;
    private final JLabel lblDetalleFechas;
    private final JLabel lblDetalleMonto;
    private final JLabel lblDetalleAbonado;
    private final JLabel lblDetalleSaldo;
    private final JLabel lblDetalleEstado;
    private final JLabel lblDetallePostergada;
    private final JTextArea txtDetalleObservaciones;

    // Detail Panel Action Buttons
    private final JButton btnAbonar;
    private final JButton btnPagarCompleto;
    private final JButton btnPostergar;
    private final JButton btnEliminar;

    private FacturaMercancia facturaSeleccionada = null;

    private static final String[] COLUMNS = {
            "Factura", "Empleado", "Monto Total", "Abonado", "Saldo Pendiente", "Vencimiento", "Estado"
    };

    public MercanciaPanel() {
        setLayout(new MigLayout("wrap, fillx, insets 24, gapy 16", "[grow, fill]", "[]12[][][grow, fill]"));
        setOpaque(false);

        // --- HEADER SECTION ---
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]12[]", "[]"));
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Gestión de Deducción de Mercancía");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        headerPanel.add(title);

        JButton btnImportarPdf = new JButton("📂 Importar PDF de Profit");
        btnImportarPdf.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnImportarPdf.addActionListener(e -> importarPdfProfit());
        headerPanel.add(btnImportarPdf);

        JButton btnNuevaFactura = new JButton("➕ Registrar Nueva Factura");
        btnNuevaFactura.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold; background: $Button.default.background; foreground: #ffffff");
        btnNuevaFactura.addActionListener(e -> abrirDialogoNuevaFactura());
        headerPanel.add(btnNuevaFactura);

        add(headerPanel, "growx");

        // --- KPI SUMMARY CARDS ---
        JPanel kpiPanel = new JPanel(new MigLayout("insets 0, gap 16", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        kpiPanel.setOpaque(false);

        lblKpiPendientesVal = new JLabel("0");
        lblKpiAdeudadoVal = new JLabel("$0.00");
        lblKpiVencidasVal = new JLabel("0");
        lblKpiPostergadasVal = new JLabel("0");

        kpiPanel.add(buildKpiCard("Facturas Pendientes", lblKpiPendientesVal, "$accentColor"));
        kpiPanel.add(buildKpiCard("Monto Adeudado Total", lblKpiAdeudadoVal, "@accentColor"));
        kpiPanel.add(buildKpiCard("Facturas Vencidas", lblKpiVencidasVal, "#e74c3c"));
        kpiPanel.add(buildKpiCard("Facturas Postergadas", lblKpiPostergadasVal, "#f39c12"));

        add(kpiPanel, "growx");

        // --- CONTROLS / FILTERS BAR ---
        JPanel filtersBar = new JPanel(new MigLayout("insets 12, gap 16", "[]12[]12[]12[]push", "[]"));
        filtersBar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        filtersBar.add(new JLabel("Filtrar por Empleado:"));
        cmbFiltroEmpleado = new JComboBox<>();
        cmbFiltroEmpleado.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cmbFiltroEmpleado.addActionListener(e -> filtrarYRecargar());
        filtersBar.add(cmbFiltroEmpleado, "width 240!");

        filtersBar.add(new JLabel("Estado:"));
        cmbFiltroEstado = new JComboBox<>(new String[]{"Todas", "Pendientes", "Abonando", "Pagadas", "Vencidas", "Postergadas"});
        cmbFiltroEstado.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cmbFiltroEstado.addActionListener(e -> filtrarYRecargar());
        filtersBar.add(cmbFiltroEstado, "width 180!");

        add(filtersBar, "growx");

        // --- TABLE MASTER ---
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

        // Custom Cell Renderer to paint rows according to status and overdue warnings
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row >= 0 && row < facturasFiltradas.size()) {
                    FacturaMercancia f = facturasFiltradas.get(row);
                    if ("PAGADA".equalsIgnoreCase(f.getEstado())) {
                        c.setForeground(isSelected ? table.getSelectionForeground() : new Color(0x2e, 0x7d, 0x32)); // Verde oscuro
                    } else if (f.isPostergada()) {
                        c.setForeground(isSelected ? table.getSelectionForeground() : new Color(0xd3, 0x54, 0x00)); // Naranja postergada
                    } else {
                        try {
                            LocalDate due = LocalDate.parse(f.getFechaVencimiento());
                            LocalDate today = LocalDate.now();
                            if (due.isBefore(today)) {
                                c.setForeground(isSelected ? table.getSelectionForeground() : new Color(0xc6, 0x28, 0x28)); // Rojo Vencido
                            } else if (due.minusDays(3).isBefore(today) || due.isEqual(today)) {
                                c.setForeground(isSelected ? table.getSelectionForeground() : new Color(0xf5, 0x7f, 0x17)); // Amarillo oscuro
                            } else {
                                c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                            }
                        } catch (Exception e) {
                            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                        }
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // --- DETAIL PANEL ---
        detailPanel = new JPanel(new MigLayout("wrap, insets 16, gapy 10", "[grow, fill]", "[]4[]4[]4[]4[]4[]4[]4[]4[]12[]12[]"));
        detailPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        JLabel lblDetailTitle = new JLabel("Detalle de la Factura");
        lblDetailTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold +2; foreground: $accentColor");
        detailPanel.add(lblDetailTitle);

        lblDetalleFactura = new JLabel("Seleccione una factura");
        lblDetalleFactura.putClientProperty(FlatClientProperties.STYLE, "font: bold +1");
        detailPanel.add(lblDetalleFactura);

        lblDetalleEmpleado = new JLabel("—");
        lblDetalleEmpleado.putClientProperty(FlatClientProperties.STYLE, "font: $semibold -1; foreground: $Label.disabledForeground");
        detailPanel.add(lblDetalleEmpleado);

        detailPanel.add(new JSeparator(), "gaptop 4, gapbottom 4");

        lblDetalleFechas = new JLabel("Fecha Emisión: — | Vencimiento: —");
        lblDetalleFechas.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        detailPanel.add(lblDetalleFechas);

        lblDetalleMonto = new JLabel("Monto Facturado: —");
        lblDetalleMonto.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        detailPanel.add(lblDetalleMonto);

        lblDetalleAbonado = new JLabel("Monto Abonado: —");
        lblDetalleAbonado.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        detailPanel.add(lblDetalleAbonado);

        lblDetalleSaldo = new JLabel("Saldo Pendiente: —");
        lblDetalleSaldo.putClientProperty(FlatClientProperties.STYLE, "font: bold; foreground: $accentColor");
        detailPanel.add(lblDetalleSaldo);

        lblDetalleEstado = new JLabel("Estado: —");
        lblDetalleEstado.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        detailPanel.add(lblDetalleEstado);

        lblDetallePostergada = new JLabel("Postergada: —");
        lblDetallePostergada.putClientProperty(FlatClientProperties.STYLE, "font: -1");
        detailPanel.add(lblDetallePostergada);

        JLabel lblObs = new JLabel("Observaciones:");
        lblObs.putClientProperty(FlatClientProperties.STYLE, "font: $semibold -2");
        detailPanel.add(lblObs, "gaptop 6");

        txtDetalleObservaciones = new JTextArea();
        txtDetalleObservaciones.setEditable(false);
        txtDetalleObservaciones.setLineWrap(true);
        txtDetalleObservaciones.setWrapStyleWord(true);
        txtDetalleObservaciones.setBackground(detailPanel.getBackground());
        txtDetalleObservaciones.setBorder(null);
        detailPanel.add(txtDetalleObservaciones, "width 100%, height 60!");

        detailPanel.add(new JSeparator(), "gaptop 4, gapbottom 4");

        // Action panel for detail panel
        JPanel detailActions = new JPanel(new GridLayout(2, 2, 8, 8));
        detailActions.setOpaque(false);

        btnAbonar = new JButton("Registrar Abono");
        btnAbonar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: -1");
        btnAbonar.addActionListener(e -> registrarAbonoFactura());
        detailActions.add(btnAbonar);

        btnPagarCompleto = new JButton("Pago Completo");
        btnPagarCompleto.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: -1");
        btnPagarCompleto.addActionListener(e -> registrarPagoCompleto());
        detailActions.add(btnPagarCompleto);

        btnPostergar = new JButton("Postergar Pago");
        btnPostergar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: -1");
        btnPostergar.addActionListener(e -> postergarFactura());
        detailActions.add(btnPostergar);

        btnEliminar = new JButton("Eliminar");
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: -1; foreground: #e74c3c");
        btnEliminar.addActionListener(e -> eliminarFactura());
        detailActions.add(btnEliminar);

        detailPanel.add(detailActions, "gaptop 8");

        setDetailPanelEnabled(false);

        // Selection Listener for the Master table
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < facturasFiltradas.size()) {
                    facturaSeleccionada = facturasFiltradas.get(row);
                } else {
                    facturaSeleccionada = null;
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

        // Initialize combos, load data, and register listeners
        cargarComboEmpleados();
        filtrarYRecargar();

        MercanciaRepository.addListener(this::filtrarYRecargar);
        EmpleadoRepository.addListener(this::cargarComboEmpleados);
    }

    private void cargarComboEmpleados() {
        Object itemSeleccionado = cmbFiltroEmpleado.getSelectedItem();
        cmbFiltroEmpleado.removeAllItems();
        cmbFiltroEmpleado.addItem("Todos los Empleados");
        for (Empleado emp : EmpleadoRepository.getAll()) {
            cmbFiltroEmpleado.addItem(new EmpleadoComboItem(emp));
        }
        if (itemSeleccionado != null) {
            cmbFiltroEmpleado.setSelectedItem(itemSeleccionado);
        }
    }

    private void filtrarYRecargar() {
        facturasFiltradas.clear();
        List<FacturaMercancia> todas = MercanciaRepository.getAll();

        Object empSel = cmbFiltroEmpleado.getSelectedItem();
        String estadoSel = (String) cmbFiltroEstado.getSelectedItem();

        // Calculate KPIs first on global list
        calcularKpiCards(todas);

        for (FacturaMercancia f : todas) {
            boolean matchesEmpleado = true;
            if (empSel instanceof EmpleadoComboItem) {
                matchesEmpleado = f.getCedulaEmpleado().equalsIgnoreCase(((EmpleadoComboItem) empSel).cedula);
            }

            boolean matchesEstado = true;
            if (estadoSel != null) {
                switch (estadoSel) {
                    case "Pendientes":
                        matchesEstado = "PENDIENTE".equalsIgnoreCase(f.getEstado());
                        break;
                    case "Abonando":
                        matchesEstado = "ABONANDO".equalsIgnoreCase(f.getEstado());
                        break;
                    case "Pagadas":
                        matchesEstado = "PAGADA".equalsIgnoreCase(f.getEstado());
                        break;
                    case "Postergadas":
                        matchesEstado = f.isPostergada();
                        break;
                    case "Vencidas":
                        try {
                            LocalDate due = LocalDate.parse(f.getFechaVencimiento());
                            matchesEstado = due.isBefore(LocalDate.now()) && !"PAGADA".equalsIgnoreCase(f.getEstado());
                        } catch (Exception e) {
                            matchesEstado = false;
                        }
                        break;
                }
            }

            if (matchesEmpleado && matchesEstado) {
                facturasFiltradas.add(f);
            }
        }

        repopulateTable();
        
        // Mantener selección anterior si es posible
        if (facturaSeleccionada != null) {
            int newIdx = facturasFiltradas.indexOf(facturaSeleccionada);
            if (newIdx >= 0) {
                table.setRowSelectionInterval(newIdx, newIdx);
            } else {
                facturaSeleccionada = null;
                updateDetailPanel();
            }
        } else {
            updateDetailPanel();
        }
    }

    private void repopulateTable() {
        tableModel.setRowCount(0);
        for (FacturaMercancia f : facturasFiltradas) {
            Empleado emp = EmpleadoRepository.getAll().stream()
                    .filter(e -> e.getCedula().equalsIgnoreCase(f.getCedulaEmpleado()))
                    .findFirst().orElse(null);
            String nombreEmp = emp != null ? emp.getNombreCompleto() : f.getCedulaEmpleado();

            tableModel.addRow(new Object[]{
                    f.getNumeroFactura(),
                    nombreEmp,
                    String.format("$%,.2f", f.getMontoTotal()),
                    String.format("$%,.2f", f.getMontoAbonado()),
                    String.format("$%,.2f", f.getSaldo()),
                    f.getFechaVencimiento(),
                    f.getEstado() + (f.isPostergada() ? " (POSTERGADA)" : "")
            });
        }
    }

    private void calcularKpiCards(List<FacturaMercancia> todas) {
        int pendientes = 0;
        double deudas = 0;
        int vencidas = 0;
        int postergadas = 0;
        LocalDate today = LocalDate.now();

        for (FacturaMercancia f : todas) {
            if (!"PAGADA".equalsIgnoreCase(f.getEstado())) {
                pendientes++;
                deudas += f.getSaldo();

                try {
                    LocalDate due = LocalDate.parse(f.getFechaVencimiento());
                    if (due.isBefore(today)) {
                        vencidas++;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (f.isPostergada()) {
                postergadas++;
            }
        }

        lblKpiPendientesVal.setText(String.valueOf(pendientes));
        lblKpiAdeudadoVal.setText(String.format("$%,.2f", deudas));
        lblKpiVencidasVal.setText(String.valueOf(vencidas));
        lblKpiPostergadasVal.setText(String.valueOf(postergadas));
    }

    private void setDetailPanelEnabled(boolean enabled) {
        btnAbonar.setEnabled(enabled);
        btnPagarCompleto.setEnabled(enabled);
        btnPostergar.setEnabled(enabled);
        btnEliminar.setEnabled(enabled);
    }

    private void updateDetailPanel() {
        if (facturaSeleccionada == null) {
            lblDetalleFactura.setText("Seleccione una factura");
            lblDetalleEmpleado.setText("—");
            lblDetalleFechas.setText("Fecha Emisión: — | Vencimiento: —");
            lblDetalleMonto.setText("Monto Facturado: —");
            lblDetalleAbonado.setText("Monto Abonado: —");
            lblDetalleSaldo.setText("Saldo Pendiente: —");
            lblDetalleEstado.setText("Estado: —");
            lblDetallePostergada.setText("Postergada: —");
            txtDetalleObservaciones.setText("");
            setDetailPanelEnabled(false);
        } else {
            Empleado emp = EmpleadoRepository.getAll().stream()
                    .filter(e -> e.getCedula().equalsIgnoreCase(facturaSeleccionada.getCedulaEmpleado()))
                    .findFirst().orElse(null);
            String nombreEmp = emp != null ? emp.getNombreCompleto() : "Desconocido";

            lblDetalleFactura.setText("Factura Nro: " + facturaSeleccionada.getNumeroFactura());
            lblDetalleEmpleado.setText(String.format("%s (C.I. %s)", nombreEmp, facturaSeleccionada.getCedulaEmpleado()));
            lblDetalleFechas.setText(String.format("Fecha Emisión: %s | Vencimiento: %s", 
                    facturaSeleccionada.getFechaEmision(), facturaSeleccionada.getFechaVencimiento()));
            lblDetalleMonto.setText(String.format("Monto Facturado: $%,.2f USD", facturaSeleccionada.getMontoTotal()));
            lblDetalleAbonado.setText(String.format("Monto Abonado: $%,.2f USD", facturaSeleccionada.getMontoAbonado()));
            lblDetalleSaldo.setText(String.format("Saldo Pendiente: $%,.2f USD", facturaSeleccionada.getSaldo()));
            lblDetalleEstado.setText("Estado: " + facturaSeleccionada.getEstado());
            lblDetallePostergada.setText("Postergada: " + (facturaSeleccionada.isPostergada() ? "SÍ" : "NO"));
            txtDetalleObservaciones.setText(facturaSeleccionada.getObservaciones() != null ? facturaSeleccionada.getObservaciones() : "");

            boolean pagada = "PAGADA".equalsIgnoreCase(facturaSeleccionada.getEstado());
            btnAbonar.setEnabled(!pagada);
            btnPagarCompleto.setEnabled(!pagada);
            btnPostergar.setEnabled(!pagada && !facturaSeleccionada.isPostergada());
            btnEliminar.setEnabled(true);
        }
    }

    private void registrarAbonoFactura() {
        if (facturaSeleccionada == null) return;

        double saldo = facturaSeleccionada.getSaldo();
        String mStr = JOptionPane.showInputDialog(this, 
                String.format("Saldo Pendiente: $%,.2f\nIngrese el monto del abono en USD:", saldo), 
                "Registrar Abono", JOptionPane.PLAIN_MESSAGE);

        if (mStr == null || mStr.trim().isEmpty()) return;

        try {
            double monto = Double.parseDouble(mStr.trim().replace(",", "."));
            if (monto <= 0) {
                JOptionPane.showMessageDialog(this, "El abono debe ser mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (monto > saldo) {
                JOptionPane.showMessageDialog(this, "El abono no puede superar el saldo de la factura.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            MercanciaRepository.registrarAbono(facturaSeleccionada.getId(), monto);
            JOptionPane.showMessageDialog(this, "Abono registrado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un monto numérico válido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarPagoCompleto() {
        if (facturaSeleccionada == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, 
                String.format("¿Desea registrar el pago total de la factura %s por un monto de $%,.2f?", 
                        facturaSeleccionada.getNumeroFactura(), facturaSeleccionada.getSaldo()), 
                "Pago Completo", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            MercanciaRepository.pagarCompleto(facturaSeleccionada.getId());
            JOptionPane.showMessageDialog(this, "Factura pagada en su totalidad.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void postergarFactura() {
        if (facturaSeleccionada == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de postergar el cobro de esta factura?\nEsta acción es de carácter excepcional.", 
                "Postergar Cobro", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            MercanciaRepository.postergar(facturaSeleccionada.getId());
            JOptionPane.showMessageDialog(this, "Cobro de factura postergado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void eliminarFactura() {
        if (facturaSeleccionada == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de eliminar permanentemente este registro de factura?", 
                "Eliminar Registro", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            MercanciaRepository.remove(facturaSeleccionada.getId());
            JOptionPane.showMessageDialog(this, "Factura eliminada de la base de datos.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void abrirDialogoNuevaFactura() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Registrar Factura de Mercancía", true);
        dialog.setLayout(new MigLayout("wrap 2, insets 16, gapy 12", "[right][grow, fill]"));

        dialog.add(new JLabel("Empleado:"));
        JComboBox<EmpleadoComboItem> cmbEmps = new JComboBox<>();
        for (Empleado emp : EmpleadoRepository.getAll()) {
            if ("Activo".equalsIgnoreCase(emp.getEstado())) {
                cmbEmps.addItem(new EmpleadoComboItem(emp));
            }
        }
        dialog.add(cmbEmps);

        dialog.add(new JLabel("Número Factura:"));
        JTextField txtNumFactura = new JTextField();
        dialog.add(txtNumFactura);

        dialog.add(new JLabel("Monto Total (USD):"));
        JTextField txtMonto = new JTextField();
        dialog.add(txtMonto);

        dialog.add(new JLabel("Fecha Emisión (YYYY-MM-DD):"));
        JTextField txtFecha = new JTextField(LocalDate.now().toString());
        dialog.add(txtFecha);

        dialog.add(new JLabel("Observaciones:"));
        JTextArea txtObs = new JTextArea(3, 20);
        txtObs.setLineWrap(true);
        txtObs.setWrapStyleWord(true);
        JScrollPane scrollObs = new JScrollPane(txtObs);
        dialog.add(scrollObs);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());

        JButton btnGuardar = new JButton("Registrar");
        btnGuardar.addActionListener(e -> {
            EmpleadoComboItem itemEmp = (EmpleadoComboItem) cmbEmps.getSelectedItem();
            String numFact = txtNumFactura.getText().trim();
            String montoStr = txtMonto.getText().trim();
            String fechaStr = txtFecha.getText().trim();
            String observaciones = txtObs.getText().trim();

            if (itemEmp == null || numFact.isEmpty() || montoStr.isEmpty() || fechaStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Todos los campos obligatorios deben estar completos.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double total = Double.parseDouble(montoStr.replace(",", "."));
                if (total <= 0) {
                    JOptionPane.showMessageDialog(dialog, "El monto total debe ser mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                LocalDate fEmision = LocalDate.parse(fechaStr);
                LocalDate fVencimiento = fEmision.plusDays(15);

                FacturaMercancia f = new FacturaMercancia(
                        0,
                        itemEmp.cedula,
                        numFact,
                        total,
                        0.0,
                        fEmision.toString(),
                        fVencimiento.toString(),
                        false,
                        "PENDIENTE",
                        observaciones.isEmpty() ? null : observaciones
                );

                MercanciaRepository.add(f);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "El monto debe ser numérico válido.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "La fecha debe tener el formato YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(btnCancelar, "split 2, right");
        dialog.add(btnGuardar, "right");

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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

    private static class EmpleadoComboItem {
        final String cedula;
        final String nombre;

        EmpleadoComboItem(Empleado emp) {
            this.cedula = emp.getCedula();
            this.nombre = emp.getNombreCompleto();
        }

        @Override
        public String toString() {
            return nombre + " (" + cedula + ")";
        }
    }

    private void importarPdfProfit() {
        JFileChooser chooser = new JFileChooser(new java.io.File("."));
        chooser.setDialogTitle("Seleccionar PDF de Profit (Facturas Pendientes)");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Documentos PDF (*.pdf)", "pdf"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File selectedFile = chooser.getSelectedFile();
        try {
            com.nomina.service.PdfImporter.ImportResult importResult = com.nomina.service.PdfImporter.importFromPdf(selectedFile);

            StringBuilder msg = new StringBuilder();
            msg.append("Importación de PDF Profit completada:\n\n");
            msg.append("✅ Nuevas facturas registradas: ").append(importResult.getSuccessCount()).append("\n");
            msg.append("🔄 Facturas actualizadas (saldo/estado): ").append(importResult.getUpdatedCount()).append("\n");

            if (!importResult.getWarnings().isEmpty()) {
                msg.append("\n⚠️ Advertencias (Empleados no registrados en BD):\n");
                int limit = Math.min(importResult.getWarnings().size(), 10);
                for (int i = 0; i < limit; i++) {
                    msg.append("  • ").append(importResult.getWarnings().get(i)).append("\n");
                }
                if (importResult.getWarnings().size() > 10) {
                    msg.append("  • ... y ").append(importResult.getWarnings().size() - 10).append(" más.");
                }
            }

            JOptionPane.showMessageDialog(this, msg.toString(),
                    "Resultados de Importación PDF",
                    importResult.getWarnings().isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

            filtrarYRecargar(); // Refrescar vista
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al importar el PDF:\n" + ex.getMessage(),
                    "Error de Importación",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
