package com.nomina.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper para gestionar la conexión y creación de tablas en SQLite.
 */
public final class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:nomina_inteligente.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver no encontrado: " + e.getMessage());
        }
    }

    private DatabaseHelper() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS empleados (" +
                    "cedula TEXT PRIMARY KEY, " +
                    "nombreCompleto TEXT, " +
                    "cargo TEXT, " +
                    "departamento TEXT, " +
                    "salarioUsd REAL, " +
                    "tipoContrato TEXT, " +
                    "estado TEXT, " +
                    "fechaIngreso TEXT" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS nominas (" +
                    "periodoId TEXT, " +
                    "cedula TEXT, " +
                    "nombreCompleto TEXT, " +
                    "salarioMensualUsd REAL, " +
                    "tasaBcv REAL, " +
                    "sueldoBasePeriodoUsd REAL, " +
                    "sueldoBasePeriodoVes REAL, " +
                    "cestaTicketVes REAL, " +
                    "ivssVes REAL, " +
                    "faovVes REAL, " +
                    "netoVes REAL, " +
                    "netoUsd REAL, " +
                    "horasExtras REAL, " +
                    "horasNocturnas REAL, " +
                    "diasFeriados REAL, " +
                    "bonosExtrasUsd REAL, " +
                    "diasNoTrabajados REAL, " +
                    "adelantoVes REAL, " +
                    "adelantoUsd REAL, " +
                    "PRIMARY KEY (periodoId, cedula)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS configuracion (" +
                    "clave TEXT PRIMARY KEY, " +
                    "valor TEXT" +
                    ")");

        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }
}
