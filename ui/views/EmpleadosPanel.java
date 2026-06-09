package com.nomina.ui.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.nomina.model.Empleado;
import com.nomina.repository.EmpleadoRepository;
import com.nomina.service.CsvImporter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de Empleados de Nómina Inteligente.
 * Conectado reactivamente a {@link EmpleadoRepository}.
 */
public class EmpleadosPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JLabel lblCount;
    private final JButton btnEdit;
    private final JButton btnDelete;
    private final JTable table;
    private final JTextField txtSearch;
    private final JComboBox<String> cmbDepartaments;

    private static final String[] COLUMNS = {
            "✓", "Cédula", "Nombre Completo", "Cargo", "Departamento",
            "Salario Base (USD)", "Tipo Contrato", "Estado"
    };

    private static final String[] DEPARTMENTS = {
            "Administración", "Cobranzas", "Compras", "Finanzas", "Operaciones", "Recursos Humanos", "Seguridad", "Tecnología", "Ventas"
    };

    public EmpleadosPanel() {
        setLayout(new MigLayout("wrap, fillx, insets 24, gapy 16", "[grow, fill]", "[]12[][grow, fill]"));
        setOpaque(false);

        // --- TITLE & ACTION SECTION ---
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]8[]push[]12[]12[]12[]", "[]"));
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Directorio de Empleados");
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        headerPanel.add(title);

        lblCount = new JLabel();
        lblCount.putClientProperty(FlatClientProperties.STYLE, "font: $semibold; foreground: $Label.disabledForeground");
        headerPanel.add(lblCount);

        JButton btnImportCsv = new JButton("\uD83D\uDCE5  Cargar CSV");
        btnImportCsv.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnImportCsv.addActionListener(e -> importCsv());
        headerPanel.add(btnImportCsv);

        JButton btnNewEmployee = new JButton("➕  Nuevo Empleado");
        btnNewEmployee.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold; background: $accentColor; foreground: #ffffff");
        btnNewEmployee.addActionListener(e -> showNewEmployeeDialog());
        headerPanel.add(btnNewEmployee);

        btnEdit = new JButton("✏️  Editar");
        btnEdit.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(e -> editSelectedEmployee());
        headerPanel.add(btnEdit);

        btnDelete = new JButton("🗑️  Eliminar");
        btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: $semibold");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteSelectedEmployee());
        headerPanel.add(btnDelete);

        add(headerPanel, "growx");

        // --- SEARCH & FILTER BAR ---
        JPanel filterBar = new JPanel(new MigLayout("insets 12, gap 12", "[grow, fill][220!]", "[]"));
        filterBar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        txtSearch = new JTextField();
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre, cédula o cargo...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
        });

        List<String> deptoOptions = new ArrayList<>();
        deptoOptions.add("Todos los Departamentos");
        for (String d : DEPARTMENTS) {
            deptoOptions.add(d);
        }
        cmbDepartaments = new JComboBox<>(deptoOptions.toArray(new String[0]));
        cmbDepartaments.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        cmbDepartaments.addActionListener(e -> refreshTable());

        filterBar.add(txtSearch, "growx");
        filterBar.add(cmbDepartaments, "width 220!");
        add(filterBar, "growx");

        // --- TABLE CONTAINER ---
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");
        tableContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return super.getColumnClass(column);
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold; height: 32; background: $control");

        // Configurar ancho de la columna checkbox
        table.getColumnModel().getColumn(0).setMinWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);

        // Botones de selección rápida
        JPanel selectActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        selectActionsPanel.setOpaque(false);
        
        JButton btnSelectAll = new JButton("Seleccionar Todos");
        btnSelectAll.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: -1");
        btnSelectAll.addActionListener(ev -> {
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, table.getRowCount() - 1);
            }
        });
        
        JButton btnDeselectAll = new JButton("Deseleccionar Todos");
        btnDeselectAll.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: -1");
        btnDeselectAll.addActionListener(ev -> {
            table.clearSelection();
        });
        
        selectActionsPanel.add(btnSelectAll);
        selectActionsPanel.add(btnDeselectAll);
        tableContainer.add(selectActionsPanel, BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int[] selectedRows = table.getSelectedRows();
                boolean hasSelection = selectedRows.length > 0;
                btnEdit.setEnabled(hasSelection);
                btnDelete.setEnabled(hasSelection);

                // Sincronizar checkboxes de las filas
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    boolean isSel = false;
                    for (int selRow : selectedRows) {
                        if (selRow == i) {
                            isSel = true;
                            break;
                        }
                    }
                    if (tableModel.getRowCount() > i && !((Boolean) tableModel.getValueAt(i, 0)).equals(isSel)) {
                        tableModel.setValueAt(isSel, i, 0);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        tableContainer.add(scrollPane, BorderLayout.CENTER);
        add(tableContainer, "grow, pushy");

        // Carga inicial y listener
        refreshTable();
        EmpleadoRepository.addListener(this::refreshTable);
    }

    private void refreshTable() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        String searchText = txtSearch == null ? "" : txtSearch.getText().toLowerCase().trim();
        String selectedDepto = cmbDepartaments == null ? "Todos los Departamentos" : (String) cmbDepartaments.getSelectedItem();

        List<Empleado> lista = EmpleadoRepository.getAll();
        int shownCount = 0;
        for (Empleado emp : lista) {
            // Filtrado de búsqueda
            if (!searchText.isEmpty()) {
                boolean match = emp.getNombreCompleto().toLowerCase().contains(searchText)
                        || emp.getCedula().toLowerCase().contains(searchText)
                        || emp.getCargo().toLowerCase().contains(searchText);
                if (!match) continue;
            }
            // Filtrado de departamento
            if (selectedDepto != null && !selectedDepto.equals("Todos los Departamentos")) {
                if (!emp.getDepartamento().equalsIgnoreCase(selectedDepto)) {
                    continue;
                }
            }

            tableModel.addRow(new Object[]{
                    false, // Checkbox
                    emp.getCedula(),
                    emp.getNombreCompleto(),
                    emp.getCargo(),
                    emp.getDepartamento(),
                    String.format("%.2f", emp.getSalarioUsd()),
                    emp.getTipoContrato(),
                    emp.getEstado()
            });
            shownCount++;
        }
        
        if (lblCount != null) {
            lblCount.setText("(" + shownCount + " mostrados de " + lista.size() + " registrados)");
        }
        if (btnEdit != null) btnEdit.setEnabled(false);
        if (btnDelete != null) btnDelete.setEnabled(false);
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo CSV de empleados");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        String filePath = chooser.getSelectedFile().getAbsolutePath();
        try {
            CsvImporter.ImportResult importResult = CsvImporter.importFromFile(filePath);

            if (importResult.successCount() > 0) {
                EmpleadoRepository.addAll(importResult.empleados());
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Importación completada.\n\n");
            msg.append("✅ Empleados importados: ").append(importResult.successCount()).append("\n");

            if (importResult.hasErrors()) {
                msg.append("⚠ Errores encontrados: ").append(importResult.errores().size()).append("\n\n");
                int showMax = Math.min(importResult.errores().size(), 5);
                for (int i = 0; i < showMax; i++) {
                    msg.append("  • ").append(importResult.errores().get(i)).append("\n");
                }
                if (importResult.errores().size() > 5) {
                    msg.append("  ... y ").append(importResult.errores().size() - 5).append(" más.");
                }
            }

            JOptionPane.showMessageDialog(this, msg.toString(),
                    "Resultado de Importación CSV",
                    importResult.hasErrors() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al leer el archivo:\n" + ex.getMessage(),
                    "Error de Importación",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNewEmployeeDialog() {
        showEmployeeDialog(null);
    }

    private void editSelectedEmployee() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) return;

        if (selectedRows.length == 1) {
            int row = selectedRows[0];
            String cedula = (String) tableModel.getValueAt(row, 1);
            Empleado emp = EmpleadoRepository.getAll().stream()
                    .filter(e -> e.getCedula().equalsIgnoreCase(cedula))
                    .findFirst().orElse(null);
            if (emp != null) {
                showEmployeeDialog(emp);
            }
        } else {
            showMultiEditDialog(selectedRows);
        }
    }

    private void deleteSelectedEmployee() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) return;

        if (selectedRows.length == 1) {
            int row = selectedRows[0];
            String cedula = (String) tableModel.getValueAt(row, 1);
            String nombre = (String) tableModel.getValueAt(row, 2);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de eliminar al empleado:\n" + nombre + " (C.I. " + cedula + ")?",
                    "Eliminar Empleado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                EmpleadoRepository.remove(cedula);
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de eliminar a los " + selectedRows.length + " empleados seleccionados?",
                    "Eliminar Múltiples Empleados", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                List<String> cedulas = new ArrayList<>();
                for (int row : selectedRows) {
                    cedulas.add((String) tableModel.getValueAt(row, 1));
                }
                for (String cedula : cedulas) {
                    EmpleadoRepository.remove(cedula);
                }
            }
        }
    }

    private void showMultiEditDialog(int[] selectedRows) {
        JPanel form = new JPanel(new MigLayout("wrap 3, insets 8, gapy 8, gapx 12", "[][][grow, fill]", ""));

        JCheckBox chkCargo = new JCheckBox("Cargo:");
        JTextField txtCargo = new JTextField(18);
        txtCargo.setEnabled(false);
        chkCargo.addActionListener(e -> txtCargo.setEnabled(chkCargo.isSelected()));

        JCheckBox chkDepto = new JCheckBox("Departamento:");
        JComboBox<String> cmbDepto = new JComboBox<>(DEPARTMENTS);
        cmbDepto.setEnabled(false);
        chkDepto.addActionListener(e -> cmbDepto.setEnabled(chkDepto.isSelected()));

        JCheckBox chkSalario = new JCheckBox("Salario Base (USD):");
        JTextField txtSalario = new JTextField(18);
        txtSalario.setEnabled(false);
        chkSalario.addActionListener(e -> txtSalario.setEnabled(chkSalario.isSelected()));

        JCheckBox chkContrato = new JCheckBox("Tipo de Contrato:");
        JComboBox<String> cmbContrato = new JComboBox<>(new String[]{"Indefinido", "Contrato Fijo", "Temporal"});
        cmbContrato.setEnabled(false);
        chkContrato.addActionListener(e -> cmbContrato.setEnabled(chkContrato.isSelected()));

        JCheckBox chkEstado = new JCheckBox("Estado:");
        JComboBox<String> cmbEstado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
        cmbEstado.setEnabled(false);
        chkEstado.addActionListener(e -> cmbEstado.setEnabled(chkEstado.isSelected()));

        form.add(new JLabel("Modificar"), "span 2, header");
        form.add(new JLabel("Valor"), "header");

        form.add(chkCargo); form.add(new JLabel("")); form.add(txtCargo);
        form.add(chkDepto); form.add(new JLabel("")); form.add(cmbDepto);
        form.add(chkSalario); form.add(new JLabel("")); form.add(txtSalario);
        form.add(chkContrato); form.add(new JLabel("")); form.add(cmbContrato);
        form.add(chkEstado); form.add(new JLabel("")); form.add(cmbEstado);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Editar " + selectedRows.length + " Empleados",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            Double salarioVal = null;
            if (chkSalario.isSelected()) {
                salarioVal = Double.parseDouble(txtSalario.getText().trim().replace(",", "."));
                if (salarioVal < 0) {
                    JOptionPane.showMessageDialog(this, "El salario debe ser positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            List<String> cedulas = new ArrayList<>();
            for (int row : selectedRows) {
                cedulas.add((String) tableModel.getValueAt(row, 1));
            }

            for (String cedula : cedulas) {
                Empleado emp = EmpleadoRepository.getAll().stream()
                        .filter(e -> e.getCedula().equalsIgnoreCase(cedula))
                        .findFirst().orElse(null);
                if (emp != null) {
                    if (chkCargo.isSelected()) emp.setCargo(txtCargo.getText().trim());
                    if (chkDepto.isSelected()) emp.setDepartamento((String) cmbDepto.getSelectedItem());
                    if (chkSalario.isSelected() && salarioVal != null) emp.setSalarioUsd(salarioVal);
                    if (chkContrato.isSelected()) emp.setTipoContrato((String) cmbContrato.getSelectedItem());
                    if (chkEstado.isSelected()) emp.setEstado((String) cmbEstado.getSelectedItem());
                    EmpleadoRepository.update(emp.getCedula(), emp);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un salario numérico válido.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEmployeeDialog(Empleado emp) {
        boolean isEdit = (emp != null);
        JPanel form = new JPanel(new MigLayout("wrap 2, insets 8, gapy 8, gapx 12", "[][grow, fill]", ""));

        JTextField txtCedula = new JTextField(18);
        JTextField txtNombre = new JTextField(18);
        JTextField txtCargo = new JTextField(18);
        JComboBox<String> cmbDepto = new JComboBox<>(DEPARTMENTS);
        JTextField txtSalario = new JTextField(18);
        JComboBox<String> cmbContrato = new JComboBox<>(new String[]{"Indefinido", "Contrato Fijo", "Temporal"});
        JTextField txtFecha = new JTextField("2024-01-15", 18);

        if (isEdit) {
            txtCedula.setText(emp.getCedula());
            txtCedula.setEditable(true);
            txtNombre.setText(emp.getNombreCompleto());
            txtCargo.setText(emp.getCargo());
            cmbDepto.setSelectedItem(emp.getDepartamento());
            txtSalario.setText(String.format("%.2f", emp.getSalarioUsd()));
            cmbContrato.setSelectedItem(emp.getTipoContrato());
            txtFecha.setText(emp.getFechaIngreso());
        }

        form.add(new JLabel("Cédula:"));
        form.add(txtCedula);
        form.add(new JLabel("Nombre Completo:"));
        form.add(txtNombre);
        form.add(new JLabel("Cargo:"));
        form.add(txtCargo);
        form.add(new JLabel("Departamento:"));
        form.add(cmbDepto);
        form.add(new JLabel("Salario Base (USD):"));
        form.add(txtSalario);
        form.add(new JLabel("Tipo de Contrato:"));
        form.add(cmbContrato);
        form.add(new JLabel("Fecha Ingreso (YYYY-MM-DD):"));
        form.add(txtFecha);

        int result = JOptionPane.showConfirmDialog(this, form,
                isEdit ? "Editar Empleado" : "Registrar Nuevo Empleado",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String cedula = txtCedula.getText().trim();
            String nombre = txtNombre.getText().trim();
            String cargo = txtCargo.getText().trim();
            String depto = (String) cmbDepto.getSelectedItem();
            double salario = Double.parseDouble(txtSalario.getText().trim().replace(",", "."));
            String contrato = (String) cmbContrato.getSelectedItem();
            String fecha = txtFecha.getText().trim();

            if (cedula.isEmpty() || nombre.isEmpty() || fecha.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cédula, Nombre y Fecha son obligatorios.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validar duplicidad de cédula
            if (!isEdit || !cedula.equalsIgnoreCase(emp.getCedula())) {
                boolean exists = EmpleadoRepository.getAll().stream()
                        .anyMatch(e -> e.getCedula().equalsIgnoreCase(cedula));
                if (exists) {
                    JOptionPane.showMessageDialog(this, "La cédula ingresada ya pertenece a otro empleado.",
                            "Error de Duplicidad", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            Empleado nuevoEmp = new Empleado(cedula, nombre, cargo, depto, salario, contrato, isEdit ? emp.getEstado() : "Activo", fecha);

            if (isEdit) {
                EmpleadoRepository.update(emp.getCedula(), nuevoEmp);
            } else {
                EmpleadoRepository.add(nuevoEmp);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un salario numérico válido.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
