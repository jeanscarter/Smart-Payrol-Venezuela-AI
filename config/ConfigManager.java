package com.nomina.config;

import com.nomina.repository.DatabaseHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Gestor de configuraciones y parámetros globales de la aplicación.
 * Persiste los datos en la base de datos SQLite en la tabla configuracion.
 * Implementa el patrón Observer para notificar cambios en tiempo de ejecución.
 */
public final class ConfigManager {

    private static final Properties properties = new Properties();
    private static final List<ConfigChangeListener> listeners = new ArrayList<>();

    // --- PARÁMETROS POR DEFECTO ---
    private static final String DEFAULT_TASA_BCV = "36.50";
    private static final String DEFAULT_CESTA_TICKET = "1407.00";
    private static final String DEFAULT_IVSS_TRABAJADOR = "4.0";
    private static final String DEFAULT_FAOV_TRABAJADOR = "1.0";
    private static final String DEFAULT_FAOV_PATRONAL = "2.0";
    private static final String DEFAULT_RIF_PATRONO = "J-30001234-5";
    private static final String DEFAULT_PROPORCION_PAGO = "0.50";
    private static final String DEFAULT_CESTA_TICKET_FIJA_USD = "20.00";
    private static final String DEFAULT_SALARIO_MINIMO = "130.00";
    private static final String DEFAULT_PENSION_PATRONAL = "9.0";
    private static final String DEFAULT_LUGAR_EMISION = "Caracas";

    @FunctionalInterface
    public interface ConfigChangeListener {
        void onConfigChanged();
    }

    private ConfigManager() {
    }

    static {
        load();
    }

    /**
     * Carga las propiedades de la base de datos SQLite. Si no existen registros, inicializa con valores por defecto.
     */
    public static void load() {
        properties.clear();
        String sql = "SELECT clave, valor FROM configuracion";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasValues = false;
            while (rs.next()) {
                properties.setProperty(rs.getString("clave"), rs.getString("valor"));
                hasValues = true;
            }

            if (!hasValues) {
                resetToDefaults();
                save();
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar la configuración: " + e.getMessage());
            resetToDefaults();
            save();
        }
    }

    /**
     * Guarda la configuración actual en la base de datos SQLite.
     */
    public static void save() {
        String sql = "INSERT OR REPLACE INTO configuracion (clave, valor) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (String key : properties.stringPropertyNames()) {
                pstmt.setString(1, key);
                pstmt.setString(2, properties.getProperty(key));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            notifyListeners();
        } catch (SQLException e) {
            System.err.println("Error al guardar la configuración: " + e.getMessage());
        }
    }

    private static void resetToDefaults() {
        properties.setProperty("tasa_bcv", DEFAULT_TASA_BCV);
        properties.setProperty("cesta_ticket", DEFAULT_CESTA_TICKET);
        properties.setProperty("ivss_trabajador", DEFAULT_IVSS_TRABAJADOR);
        properties.setProperty("faov_trabajador", DEFAULT_FAOV_TRABAJADOR);
        properties.setProperty("faov_patronal", DEFAULT_FAOV_PATRONAL);
        properties.setProperty("rif_patrono", DEFAULT_RIF_PATRONO);
        properties.setProperty("proporcion_pago", DEFAULT_PROPORCION_PAGO);
        properties.setProperty("cesta_ticket_fija_usd", DEFAULT_CESTA_TICKET_FIJA_USD);
        properties.setProperty("salario_minimo", DEFAULT_SALARIO_MINIMO);
        properties.setProperty("pension_patronal", DEFAULT_PENSION_PATRONAL);
        properties.setProperty("lugar_emision", DEFAULT_LUGAR_EMISION);
    }

    // --- GETTERS & SETTERS CON CONVERSIÓN ---

    public static double getTasaBcv() {
        return getDoubleProperty("tasa_bcv", Double.parseDouble(DEFAULT_TASA_BCV));
    }

    public static void setTasaBcv(double value) {
        properties.setProperty("tasa_bcv", String.valueOf(value));
    }

    public static double getCestaTicket() {
        return getDoubleProperty("cesta_ticket", Double.parseDouble(DEFAULT_CESTA_TICKET));
    }

    public static void setCestaTicket(double value) {
        properties.setProperty("cesta_ticket", String.valueOf(value));
    }

    public static double getIvssTrabajador() {
        return getDoubleProperty("ivss_trabajador", Double.parseDouble(DEFAULT_IVSS_TRABAJADOR));
    }

    public static void setIvssTrabajador(double value) {
        properties.setProperty("ivss_trabajador", String.valueOf(value));
    }

    public static double getFaovTrabajador() {
        return getDoubleProperty("faov_trabajador", Double.parseDouble(DEFAULT_FAOV_TRABAJADOR));
    }

    public static void setFaovTrabajador(double value) {
        properties.setProperty("faov_trabajador", String.valueOf(value));
    }

    public static double getFaovPatronal() {
        return getDoubleProperty("faov_patronal", Double.parseDouble(DEFAULT_FAOV_PATRONAL));
    }

    public static void setFaovPatronal(double value) {
        properties.setProperty("faov_patronal", String.valueOf(value));
    }

    public static String getRifPatrono() {
        return properties.getProperty("rif_patrono", DEFAULT_RIF_PATRONO);
    }

    public static void setRifPatrono(String value) {
        properties.setProperty("rif_patrono", value);
    }

    public static double getProporcionPago() {
        return getDoubleProperty("proporcion_pago", Double.parseDouble(DEFAULT_PROPORCION_PAGO));
    }

    public static void setProporcionPago(double value) {
        properties.setProperty("proporcion_pago", String.valueOf(value));
    }

    public static double getCestaTicketFijaUsd() {
        return getDoubleProperty("cesta_ticket_fija_usd", Double.parseDouble(DEFAULT_CESTA_TICKET_FIJA_USD));
    }

    public static void setCestaTicketFijaUsd(double value) {
        properties.setProperty("cesta_ticket_fija_usd", String.valueOf(value));
    }

    public static double getSalarioMinimo() {
        return getDoubleProperty("salario_minimo", Double.parseDouble(DEFAULT_SALARIO_MINIMO));
    }

    public static void setSalarioMinimo(double value) {
        properties.setProperty("salario_minimo", String.valueOf(value));
    }

    public static double getPensionPatronal() {
        return getDoubleProperty("pension_patronal", Double.parseDouble(DEFAULT_PENSION_PATRONAL));
    }

    public static void setPensionPatronal(double value) {
        properties.setProperty("pension_patronal", String.valueOf(value));
    }

    public static String getLugarEmision() {
        return properties.getProperty("lugar_emision", DEFAULT_LUGAR_EMISION);
    }

    public static void setLugarEmision(String value) {
        properties.setProperty("lugar_emision", value);
    }

    // --- EVENTOS / OBSERVER ---

    public static void addListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (ConfigChangeListener listener : listeners) {
            listener.onConfigChanged();
        }
    }

    // --- HELPERS ---

    private static double getDoubleProperty(String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
