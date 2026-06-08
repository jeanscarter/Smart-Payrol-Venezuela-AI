package com.nomina.repository;

import com.nomina.model.ReciboNomina;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para la gestión y persistencia histórica de las nóminas procesadas.
 * Guarda los datos en la base de datos SQLite.
 */
public final class NominaRepository {

    private static final List<ReciboNomina> historico = new ArrayList<>();
    private static final List<NominaChangeListener> listeners = new ArrayList<>();

    @FunctionalInterface
    public interface NominaChangeListener {
        void onNominaChanged();
    }

    static {
        load();
    }

    private NominaRepository() {
    }

    /**
     * Carga el historial de nóminas desde la base de datos SQLite.
     */
    public static void load() {
        historico.clear();
        String sql = "SELECT * FROM nominas";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReciboNomina recibo = new ReciboNomina(
                        rs.getString("periodoId"),
                        rs.getString("cedula"),
                        rs.getString("nombreCompleto"),
                        rs.getDouble("salarioMensualUsd"),
                        rs.getDouble("tasaBcv"),
                        rs.getDouble("sueldoBasePeriodoUsd"),
                        rs.getDouble("sueldoBasePeriodoVes"),
                        rs.getDouble("cestaTicketVes"),
                        rs.getDouble("ivssVes"),
                        rs.getDouble("faovVes"),
                        rs.getDouble("netoVes"),
                        rs.getDouble("netoUsd"),
                        rs.getDouble("horasExtras"),
                        rs.getDouble("horasNocturnas"),
                        rs.getDouble("diasFeriados"),
                        rs.getDouble("bonosExtrasUsd"),
                        rs.getDouble("diasNoTrabajados"),
                        rs.getDouble("adelantoVes"),
                        rs.getDouble("adelantoUsd")
                );
                historico.add(recibo);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar histórico de nóminas: " + e.getMessage());
        }
    }

    /**
     * Guarda el historial de nóminas completo en la base de datos SQLite.
     */
    public static void save() {
        String sql = "INSERT OR REPLACE INTO nominas (periodoId, cedula, nombreCompleto, salarioMensualUsd, tasaBcv, " +
                "sueldoBasePeriodoUsd, sueldoBasePeriodoVes, cestaTicketVes, ivssVes, faovVes, netoVes, netoUsd, " +
                "horasExtras, horasNocturnas, diasFeriados, bonosExtrasUsd, diasNoTrabajados, adelantoVes, adelantoUsd) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (ReciboNomina r : historico) {
                pstmt.setString(1, r.getPeriodoId());
                pstmt.setString(2, r.getCedula());
                pstmt.setString(3, r.getNombreCompleto());
                pstmt.setDouble(4, r.getSalarioMensualUsd());
                pstmt.setDouble(5, r.getTasaBcv());
                pstmt.setDouble(6, r.getSueldoBasePeriodoUsd());
                pstmt.setDouble(7, r.getSueldoBasePeriodoVes());
                pstmt.setDouble(8, r.getCestaTicketVes());
                pstmt.setDouble(9, r.getIvssVes());
                pstmt.setDouble(10, r.getFaovVes());
                pstmt.setDouble(11, r.getNetoVes());
                pstmt.setDouble(12, r.getNetoUsd());
                pstmt.setDouble(13, r.getHorasExtras());
                pstmt.setDouble(14, r.getHorasNocturnas());
                pstmt.setDouble(15, r.getDiasFeriados());
                pstmt.setDouble(16, r.getBonosExtrasUsd());
                pstmt.setDouble(17, r.getDiasNoTrabajados());
                pstmt.setDouble(18, r.getAdelantoVes());
                pstmt.setDouble(19, r.getAdelantoUsd());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error al guardar histórico de nóminas: " + e.getMessage());
        }
    }

    /**
     * Registra o sobreescribe los recibos de un periodo específico.
     */
    public static void guardarPeriodo(String periodoId, List<ReciboNomina> recibos) {
        String deleteSql = "DELETE FROM nominas WHERE periodoId = ?";
        String insertSql = "INSERT OR REPLACE INTO nominas (periodoId, cedula, nombreCompleto, salarioMensualUsd, tasaBcv, " +
                "sueldoBasePeriodoUsd, sueldoBasePeriodoVes, cestaTicketVes, ivssVes, faovVes, netoVes, netoUsd, " +
                "horasExtras, horasNocturnas, diasFeriados, bonosExtrasUsd, diasNoTrabajados, adelantoVes, adelantoUsd) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                deletePstmt.setString(1, periodoId);
                deletePstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (ReciboNomina r : recibos) {
                    pstmt.setString(1, r.getPeriodoId());
                    pstmt.setString(2, r.getCedula());
                    pstmt.setString(3, r.getNombreCompleto());
                    pstmt.setDouble(4, r.getSalarioMensualUsd());
                    pstmt.setDouble(5, r.getTasaBcv());
                    pstmt.setDouble(6, r.getSueldoBasePeriodoUsd());
                    pstmt.setDouble(7, r.getSueldoBasePeriodoVes());
                    pstmt.setDouble(8, r.getCestaTicketVes());
                    pstmt.setDouble(9, r.getIvssVes());
                    pstmt.setDouble(10, r.getFaovVes());
                    pstmt.setDouble(11, r.getNetoVes());
                    pstmt.setDouble(12, r.getNetoUsd());
                    pstmt.setDouble(13, r.getHorasExtras());
                    pstmt.setDouble(14, r.getHorasNocturnas());
                    pstmt.setDouble(15, r.getDiasFeriados());
                    pstmt.setDouble(16, r.getBonosExtrasUsd());
                    pstmt.setDouble(17, r.getDiasNoTrabajados());
                    pstmt.setDouble(18, r.getAdelantoVes());
                    pstmt.setDouble(19, r.getAdelantoUsd());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            conn.commit();

            historico.removeIf(r -> r.getPeriodoId().equalsIgnoreCase(periodoId));
            historico.addAll(recibos);
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error al guardar periodo en DB: " + e.getMessage());
        }
    }

    /**
     * Retorna todos los recibos de un periodo.
     */
    public static List<ReciboNomina> obtenerPorPeriodo(String periodoId) {
        List<ReciboNomina> recibos = new ArrayList<>();
        String sql = "SELECT * FROM nominas WHERE periodoId = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, periodoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReciboNomina recibo = new ReciboNomina(
                            rs.getString("periodoId"),
                            rs.getString("cedula"),
                            rs.getString("nombreCompleto"),
                            rs.getDouble("salarioMensualUsd"),
                            rs.getDouble("tasaBcv"),
                            rs.getDouble("sueldoBasePeriodoUsd"),
                            rs.getDouble("sueldoBasePeriodoVes"),
                            rs.getDouble("cestaTicketVes"),
                            rs.getDouble("ivssVes"),
                            rs.getDouble("faovVes"),
                            rs.getDouble("netoVes"),
                            rs.getDouble("netoUsd"),
                            rs.getDouble("horasExtras"),
                            rs.getDouble("horasNocturnas"),
                            rs.getDouble("diasFeriados"),
                            rs.getDouble("bonosExtrasUsd"),
                            rs.getDouble("diasNoTrabajados"),
                            rs.getDouble("adelantoVes"),
                            rs.getDouble("adelantoUsd")
                    );
                    recibos.add(recibo);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener recibos por periodo: " + e.getMessage());
        }
        return recibos;
    }

    /**
     * Retorna los IDs de todos los periodos procesados de forma única.
     */
    public static List<String> obtenerPeriodosProcesados() {
        List<String> periodos = new ArrayList<>();
        String sql = "SELECT DISTINCT periodoId FROM nominas";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                periodos.add(rs.getString("periodoId"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener periodos procesados: " + e.getMessage());
        }
        return periodos;
    }

    /**
     * Retorna el histórico completo de recibos.
     */
    public static List<ReciboNomina> obtenerTodo() {
        List<ReciboNomina> todos = new ArrayList<>();
        String sql = "SELECT * FROM nominas";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ReciboNomina recibo = new ReciboNomina(
                        rs.getString("periodoId"),
                        rs.getString("cedula"),
                        rs.getString("nombreCompleto"),
                        rs.getDouble("salarioMensualUsd"),
                        rs.getDouble("tasaBcv"),
                        rs.getDouble("sueldoBasePeriodoUsd"),
                        rs.getDouble("sueldoBasePeriodoVes"),
                        rs.getDouble("cestaTicketVes"),
                        rs.getDouble("ivssVes"),
                        rs.getDouble("faovVes"),
                        rs.getDouble("netoVes"),
                        rs.getDouble("netoUsd"),
                        rs.getDouble("horasExtras"),
                        rs.getDouble("horasNocturnas"),
                        rs.getDouble("diasFeriados"),
                        rs.getDouble("bonosExtrasUsd"),
                        rs.getDouble("diasNoTrabajados"),
                        rs.getDouble("adelantoVes"),
                        rs.getDouble("adelantoUsd")
                );
                todos.add(recibo);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los recibos: " + e.getMessage());
        }
        return todos;
    }

    public static void addListener(NominaChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(NominaChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (NominaChangeListener listener : listeners) {
            listener.onNominaChanged();
        }
    }
}
