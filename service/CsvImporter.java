package com.nomina.service;

import com.nomina.model.Empleado;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de ingesta de archivos CSV para carga masiva de empleados.
 * Soporta formatos con separador coma y punto-coma, con o sin header.
 */
public final class CsvImporter {

    private CsvImporter() {
    }

    /**
     * Parsea un archivo CSV y retorna la lista de empleados encontrados.
     *
     * @param filePath ruta absoluta del archivo CSV
     * @return lista de empleados parseados
     * @throws IOException si hay error de lectura
     */
    public static ImportResult importFromFile(String filePath) throws IOException {
        List<Empleado> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // header
            lineNumber++;

            if (line == null) {
                errors.add("El archivo está vacío.");
                return new ImportResult(imported, errors);
            }

            String separator = detectSeparator(line);

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                try {
                    Empleado emp = parseLine(line, separator);
                    imported.add(emp);
                } catch (Exception e) {
                    errors.add("Línea " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        return new ImportResult(imported, errors);
    }

    private static String detectSeparator(String headerLine) {
        if (headerLine.contains(";")) return ";";
        return ",";
    }

    private static Empleado parseLine(String line, String separator) {
        String[] parts = line.split(separator, -1);

        if (parts.length < 7) {
            throw new IllegalArgumentException("Se esperan al menos 7 columnas, se encontraron " + parts.length);
        }

        String cedula = parts[0].trim();
        String nombre = parts[1].trim();
        String cargo = parts[2].trim();
        String departamento = parts[3].trim();
        double salario;
        try {
            salario = Double.parseDouble(parts[4].trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Salario inválido: '" + parts[4].trim() + "'");
        }
        String tipoContrato = parts[5].trim();
        String estado = parts[6].trim();
        String fechaIngreso = (parts.length >= 8) ? parts[7].trim() : "2024-01-15";

        if (cedula.isEmpty() || nombre.isEmpty()) {
            throw new IllegalArgumentException("Cédula y Nombre no pueden estar vacíos.");
        }

        return new Empleado(cedula, nombre, cargo, departamento, salario, tipoContrato, estado, fechaIngreso);
    }

    /**
     * Resultado de una operación de importación.
     */
    public record ImportResult(List<Empleado> empleados, List<String> errores) {
        public boolean hasErrors() {
            return !errores.isEmpty();
        }

        public int successCount() {
            return empleados.size();
        }
    }
}
