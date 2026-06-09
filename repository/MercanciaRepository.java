package com.nomina.repository;

import com.nomina.model.FacturaMercancia;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de Persistencia y Gestión de Facturas de Mercancía.
 * Guarda y recupera los datos localmente en la base de datos SQLite.
 */
public final class MercanciaRepository {

    private static final List<FacturaMercancia> facturas = new ArrayList<>();
    private static final List<MercanciaChangeListener> listeners = new ArrayList<>();

    @FunctionalInterface
    public interface MercanciaChangeListener {
        void onMercanciaChanged();
    }

    static {
        load();
    }

    private MercanciaRepository() {
    }

    /**
     * Carga las facturas de mercancía desde la base de datos SQLite.
     */
    public static void load() {
        facturas.clear();
        String sql = "SELECT * FROM facturas_mercancia";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                FacturaMercancia f = new FacturaMercancia(
                        rs.getInt("id"),
                        rs.getString("cedulaEmpleado"),
                        rs.getString("numeroFactura"),
                        rs.getDouble("montoTotal"),
                        rs.getDouble("montoAbonado"),
                        rs.getString("fechaEmision"),
                        rs.getString("fechaVencimiento"),
                        rs.getInt("postergada") == 1,
                        rs.getString("estado"),
                        rs.getString("observaciones")
                );
                facturas.add(f);
            }
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error cargando facturas de mercancía: " + e.getMessage());
        }
    }

    public static List<FacturaMercancia> getAll() {
        return new ArrayList<>(facturas);
    }

    public static List<FacturaMercancia> obtenerPorEmpleado(String cedula) {
        List<FacturaMercancia> res = new ArrayList<>();
        for (FacturaMercancia f : facturas) {
            if (f.getCedulaEmpleado().equalsIgnoreCase(cedula)) {
                res.add(f);
            }
        }
        return res;
    }

    public static List<FacturaMercancia> obtenerPendientes() {
        List<FacturaMercancia> res = new ArrayList<>();
        for (FacturaMercancia f : facturas) {
            if (!"PAGADA".equalsIgnoreCase(f.getEstado())) {
                res.add(f);
            }
        }
        return res;
    }

    public static void add(FacturaMercancia f) {
        String sql = "INSERT INTO facturas_mercancia (cedulaEmpleado, numeroFactura, montoTotal, montoAbonado, fechaEmision, fechaVencimiento, postergada, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, f.getCedulaEmpleado());
            pstmt.setString(2, f.getNumeroFactura());
            pstmt.setDouble(3, f.getMontoTotal());
            pstmt.setDouble(4, f.getMontoAbonado());
            pstmt.setString(5, f.getFechaEmision());
            pstmt.setString(6, f.getFechaVencimiento());
            pstmt.setInt(7, f.isPostergada() ? 1 : 0);
            pstmt.setString(8, f.getEstado());
            pstmt.setString(9, f.getObservaciones());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    f.setId(generatedKeys.getInt(1));
                }
            }
            facturas.add(f);
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error agregando factura de mercancía: " + e.getMessage());
        }
    }

    public static void update(FacturaMercancia f) {
        String sql = "UPDATE facturas_mercancia SET montoTotal = ?, montoAbonado = ?, fechaEmision = ?, fechaVencimiento = ?, estado = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, f.getMontoTotal());
            pstmt.setDouble(2, f.getMontoAbonado());
            pstmt.setString(3, f.getFechaEmision());
            pstmt.setString(4, f.getFechaVencimiento());
            pstmt.setString(5, f.getEstado());
            pstmt.setInt(6, f.getId());
            pstmt.executeUpdate();

            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error actualizando factura de mercancía: " + e.getMessage());
        }
    }

    public static void registrarAbono(int facturaId, double monto) {
        FacturaMercancia f = encontrarPorId(facturaId);
        if (f == null) return;

        double nuevoAbono = f.getMontoAbonado() + monto;
        if (nuevoAbono > f.getMontoTotal()) {
            nuevoAbono = f.getMontoTotal();
        }
        String nuevoEstado = (nuevoAbono >= f.getMontoTotal()) ? "PAGADA" : "ABONANDO";

        String sql = "UPDATE facturas_mercancia SET montoAbonado = ?, estado = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, nuevoAbono);
            pstmt.setString(2, nuevoEstado);
            pstmt.setInt(3, facturaId);
            pstmt.executeUpdate();

            f.setMontoAbonado(nuevoAbono);
            f.setEstado(nuevoEstado);
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error registrando abono de mercancía: " + e.getMessage());
        }
    }

    public static void postergar(int facturaId) {
        FacturaMercancia f = encontrarPorId(facturaId);
        if (f == null) return;

        String sql = "UPDATE facturas_mercancia SET postergada = 1 WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, facturaId);
            pstmt.executeUpdate();

            f.setPostergada(true);
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error postergando factura de mercancía: " + e.getMessage());
        }
    }

    public static void pagarCompleto(int facturaId) {
        FacturaMercancia f = encontrarPorId(facturaId);
        if (f == null) return;

        double abonoRestante = f.getMontoTotal() - f.getMontoAbonado();
        registrarAbono(facturaId, abonoRestante);
    }

    public static void remove(int facturaId) {
        String sql = "DELETE FROM facturas_mercancia WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, facturaId);
            pstmt.executeUpdate();

            facturas.removeIf(f -> f.getId() == facturaId);
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error eliminando factura de mercancía: " + e.getMessage());
        }
    }

    private static FacturaMercancia encontrarPorId(int id) {
        for (FacturaMercancia f : facturas) {
            if (f.getId() == id) {
                return f;
            }
        }
        return null;
    }

    public static void addListener(MercanciaChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(MercanciaChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (MercanciaChangeListener listener : listeners) {
            listener.onMercanciaChanged();
        }
    }
}
