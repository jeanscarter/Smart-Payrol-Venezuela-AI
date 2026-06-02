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
import java.util.List;

/**
 * Panel de Empleados de Nómina Inteligente.
 * Conectado reactivamente a {@link EmpleadoRepository}.
 */
public class EmpleadosPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JLabel lblCount;

    private static final String[] COLUMNS = {
            "Cédula", "Nombre Completo", "Cargo", "Departamento",
            "Salario Base (USD)", "Tipo Contrato", "Estado"
    };

    public EmpleadosPanel() {
        setLayout(new MigLayout("wrap, fillx, insets 24, gapy 16", "[grow, fill]", "[]12[][grow, fill]"));
        setOpaque(false);

        // --- TITLE & ACTION SECTION ---
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]8[]push[]12[]", "[]"));
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

        add(headerPanel, "growx");

        // --- SEARCH & FILTER BAR ---
        JPanel filterBar = new JPanel(new MigLayout("insets 12, gap 12", "[grow, fill][180!]", "[]"));
        filterBar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: $control");

        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre, cédula o cargo...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

        JComboBox<String> cmbDepartaments = new JComboBox<>(new String[]{
                "Todos los Departamentos", "Tecnología", "Recursos Humanos", "Finanzas", "Operaciones"
        });
        cmbDepartaments.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

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
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font: bold; height: 32; background: $control");

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
        tableModel.setRowCount(0);
        List<Empleado> lista = EmpleadoRepository.getAll();
        for (Empleado emp : lista) {
            tableModel.addRow(new Object[]{
                    emp.getCedula(),
                    emp.getNombreCompleto(),
                    emp.getCargo(),
                    emp.getDepartamento(),
                    String.format("%.2f", emp.getSalarioUsd()),
                    emp.getTipoContrato(),
                    emp.getEstado()
            });
        }
        lblCount.setText("(" + lista.size() + " registrados)");
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
        JPanel form = new JPanel(new MigLayout("wrap 2, insets 8, gapy 8, gapx 12", "[][grow, fill]", ""));

        JTextField txtCedula = new JTextField(18);
        JTextField txtNombre = new JTextField(18);
        JTextField txtCargo = new JTextField(18);
        JComboBox<String> cmbDepto = new JComboBox<>(new String[]{"Tecnología", "Finanzas", "Recursos Humanos", "Operaciones"});
        JTextField txtSalario = new JTextField(18);
        JComboBox<String> cmbContrato = new JComboBox<>(new String[]{"Indefinido", "Contrato Fijo", "Temporal"});
        JTextField txtFecha = new JTextField("2024-01-15", 18);

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

        int result = JOptionPane.showConfirmDialog(this, form, "Registrar Nuevo Empleado",
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

            EmpleadoRepository.add(new Empleado(cedula, nombre, cargo, depto, salario, contrato, "Activo", fecha));

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un salario numérico válido.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
