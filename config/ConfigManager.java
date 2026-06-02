package com.nomina.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Gestor de configuraciones y parámetros globales de la aplicación.
 * Persiste los datos en un archivo config.properties local.
 * Implementa el patrón Observer para notificar cambios en tiempo de ejecución.
 */
public final class ConfigManager {

    private static final String FILE_NAME = "config.properties";
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
     * Carga las propiedades del archivo local. Si no existe, lo crea con valores por defecto.
     */
    public static void load() {
        Path path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) {
            resetToDefaults();
            save();
            return;
        }

        try (InputStream input = new FileInputStream(FILE_NAME)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error al cargar la configuración: " + e.getMessage());
            resetToDefaults();
        }
    }

    /**
     * Guarda la configuración actual en el archivo config.properties.
     */
    public static void save() {
        try (OutputStream output = new FileOutputStream(FILE_NAME)) {
            properties.store(output, "Configuracion de Nomina Inteligente - Parametros Globales");
            notifyListeners();
        } catch (IOException e) {
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
