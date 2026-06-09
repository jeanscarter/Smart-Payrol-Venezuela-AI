package com.nomina.repository;

import com.nomina.model.Empleado;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de Persistencia y Gestión del Directorio de Empleados.
 * Guarda y recupera los datos localmente en la base de datos SQLite.
 */
public final class EmpleadoRepository {

    private static final List<Empleado> empleados = new ArrayList<>();
    private static final List<EmpleadoChangeListener> listeners = new ArrayList<>();

    @FunctionalInterface
    public interface EmpleadoChangeListener {
        void onEmpleadosChanged();
    }

    static {
        load();
    }

    private EmpleadoRepository() {
    }

    /**
     * Carga los empleados de la base de datos SQLite. Si no existen registros, inicializa con valores mock estándar.
     */
    public static void load() {
        empleados.clear();
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM empleados")) {

            while (rs.next()) {
                Empleado emp = new Empleado(
                        rs.getString("cedula"),
                        rs.getString("nombreCompleto"),
                        rs.getString("cargo"),
                        rs.getString("departamento"),
                        rs.getDouble("salarioUsd"),
                        rs.getString("tipoContrato"),
                        rs.getString("estado"),
                        rs.getString("fechaIngreso")
                );
                empleados.add(emp);
            }

            if (empleados.isEmpty()) {
                initMockData();
                save();
            }
        } catch (SQLException e) {
            System.err.println("Error cargando empleados: " + e.getMessage());
            initMockData();
            save();
        }
    }

    /**
     * Guarda la lista de empleados actual en la base de datos SQLite.
     */
    public static void save() {
        String sql = "INSERT OR REPLACE INTO empleados (cedula, nombreCompleto, cargo, departamento, salarioUsd, tipoContrato, estado, fechaIngreso) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Empleado emp : empleados) {
                pstmt.setString(1, emp.getCedula());
                pstmt.setString(2, emp.getNombreCompleto());
                pstmt.setString(3, emp.getCargo());
                pstmt.setString(4, emp.getDepartamento());
                pstmt.setDouble(5, emp.getSalarioUsd());
                pstmt.setString(6, emp.getTipoContrato());
                pstmt.setString(7, emp.getEstado());
                pstmt.setString(8, emp.getFechaIngreso());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error guardando empleados: " + e.getMessage());
        }
    }

    public static List<Empleado> getAll() {
        return new ArrayList<>(empleados);
    }

    public static void add(Empleado emp) {
        String sql = "INSERT OR REPLACE INTO empleados (cedula, nombreCompleto, cargo, departamento, salarioUsd, tipoContrato, estado, fechaIngreso) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, emp.getCedula());
            pstmt.setString(2, emp.getNombreCompleto());
            pstmt.setString(3, emp.getCargo());
            pstmt.setString(4, emp.getDepartamento());
            pstmt.setDouble(5, emp.getSalarioUsd());
            pstmt.setString(6, emp.getTipoContrato());
            pstmt.setString(7, emp.getEstado());
            pstmt.setString(8, emp.getFechaIngreso());
            pstmt.executeUpdate();

            empleados.removeIf(e -> e.getCedula().equalsIgnoreCase(emp.getCedula()));
            empleados.add(emp);
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error agregando empleado: " + e.getMessage());
        }
    }

    public static void addAll(List<Empleado> newEmps) {
        String sql = "INSERT OR REPLACE INTO empleados (cedula, nombreCompleto, cargo, departamento, salarioUsd, tipoContrato, estado, fechaIngreso) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Empleado emp : newEmps) {
                pstmt.setString(1, emp.getCedula());
                pstmt.setString(2, emp.getNombreCompleto());
                pstmt.setString(3, emp.getCargo());
                pstmt.setString(4, emp.getDepartamento());
                pstmt.setDouble(5, emp.getSalarioUsd());
                pstmt.setString(6, emp.getTipoContrato());
                pstmt.setString(7, emp.getEstado());
                pstmt.setString(8, emp.getFechaIngreso());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();

            for (Empleado emp : newEmps) {
                empleados.removeIf(e -> e.getCedula().equalsIgnoreCase(emp.getCedula()));
                empleados.add(emp);
            }
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error agregando lista de empleados: " + e.getMessage());
        }
    }

    public static void remove(String cedula) {
        String sql = "DELETE FROM empleados WHERE cedula = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cedula);
            pstmt.executeUpdate();

            empleados.removeIf(emp -> emp.getCedula().equalsIgnoreCase(cedula));
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error eliminando empleado: " + e.getMessage());
        }
    }

    public static void update(String oldCedula, Empleado emp) {
        String sql = "UPDATE empleados SET cedula = ?, nombreCompleto = ?, cargo = ?, departamento = ?, salarioUsd = ?, tipoContrato = ?, estado = ?, fechaIngreso = ? WHERE cedula = ?";
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, emp.getCedula());
                pstmt.setString(2, emp.getNombreCompleto());
                pstmt.setString(3, emp.getCargo());
                pstmt.setString(4, emp.getDepartamento());
                pstmt.setDouble(5, emp.getSalarioUsd());
                pstmt.setString(6, emp.getTipoContrato());
                pstmt.setString(7, emp.getEstado());
                pstmt.setString(8, emp.getFechaIngreso());
                pstmt.setString(9, oldCedula);
                pstmt.executeUpdate();
            }

            if (!emp.getCedula().equalsIgnoreCase(oldCedula)) {
                String updateNominas = "UPDATE nominas SET cedula = ? WHERE cedula = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateNominas)) {
                    pstmt.setString(1, emp.getCedula());
                    pstmt.setString(2, oldCedula);
                    pstmt.executeUpdate();
                }

                String updateMercancia = "UPDATE facturas_mercancia SET cedulaEmpleado = ? WHERE cedulaEmpleado = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateMercancia)) {
                    pstmt.setString(1, emp.getCedula());
                    pstmt.setString(2, oldCedula);
                    pstmt.executeUpdate();
                }
            } else {
                String updateNominasName = "UPDATE nominas SET nombreCompleto = ? WHERE cedula = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateNominasName)) {
                    pstmt.setString(1, emp.getNombreCompleto());
                    pstmt.setString(2, oldCedula);
                    pstmt.executeUpdate();
                }
            }

            conn.commit();

            empleados.removeIf(e -> e.getCedula().equalsIgnoreCase(oldCedula));
            empleados.add(emp);

            // Reload caches
            NominaRepository.load();
            MercanciaRepository.load();

            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error actualizando empleado: " + e.getMessage());
        }
    }

    public static void addListener(EmpleadoChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(EmpleadoChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (EmpleadoChangeListener listener : listeners) {
            listener.onEmpleadosChanged();
        }
    }

    private static void initMockData() {
        empleados.add(new Empleado("V-12.345.678", "Pedro Pérez", "Desarrollador Backend", "Tecnología", 850.00, "Indefinido", "Activo", "2023-03-01"));
        empleados.add(new Empleado("V-87.654.321", "María Rodríguez", "Directora de Finanzas", "Finanzas", 1500.00, "Indefinido", "Activo", "2021-08-15"));
        empleados.add(new Empleado("V-15.998.443", "Juan Castellanos", "Analista de RRHH", "Recursos Humanos", 600.00, "Indefinido", "Activo", "2024-02-10"));
        empleados.add(new Empleado("V-20.112.554", "Ana Luisa Gómez", "Diseñadora UX/UI", "Tecnología", 750.00, "Contrato Fijo", "Activo", "2024-05-20"));
    }
}
