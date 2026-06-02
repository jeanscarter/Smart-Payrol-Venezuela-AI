package com.nomina.repository;

import com.nomina.model.Empleado;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de Persistencia y Gestión del Directorio de Empleados.
 * Guarda y recupera los datos localmente en un archivo CSV llamado empleados.csv.
 */
public final class EmpleadoRepository {

    private static final String FILE_NAME = "empleados.csv";
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
     * Carga los empleados de empleados.csv. Si no existe, inicializa con valores mock estándar.
     */
    public static void load() {
        empleados.clear();
        Path path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) {
            initMockData();
            save();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line = br.readLine(); // Saltar header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Empleado emp = parseCsvLine(line);
                if (emp != null) {
                    empleados.add(emp);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando empleados: " + e.getMessage());
            initMockData();
            save();
        }
    }

    /**
     * Guarda la lista de empleados actual en el archivo empleados.csv.
     */
    public static void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            bw.write("Cedula,NombreCompleto,Cargo,Departamento,SalarioUsd,TipoContrato,Estado,FechaIngreso");
            bw.newLine();
            for (Empleado emp : empleados) {
                bw.write(emp.toString());
                bw.newLine();
            }
            notifyListeners();
        } catch (IOException e) {
            System.err.println("Error guardando empleados: " + e.getMessage());
        }
    }

    public static List<Empleado> getAll() {
        return new ArrayList<>(empleados);
    }

    public static void add(Empleado emp) {
        empleados.add(emp);
        save();
    }

    public static void addAll(List<Empleado> newEmps) {
        empleados.addAll(newEmps);
        save();
    }

    public static void remove(String cedula) {
        empleados.removeIf(emp -> emp.getCedula().equalsIgnoreCase(cedula));
        save();
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

    private static Empleado parseCsvLine(String line) {
        try {
            // Un split simple asumiendo formato plano sin comas complejas en strings
            String[] parts = line.split(",");
            if (parts.length < 7) return null;

            String cedula = parts[0].trim();
            String name = parts[1].trim();
            String cargo = parts[2].trim();
            String depto = parts[3].trim();
            double salary = Double.parseDouble(parts[4].trim());
            String contrato = parts[5].trim();
            String estado = parts[6].trim();
            String fechaIngreso = (parts.length >= 8) ? parts[7].trim() : "2024-01-15";

            return new Empleado(cedula, name, cargo, depto, salary, contrato, estado, fechaIngreso);
        } catch (Exception e) {
            System.err.println("Error parseando línea de empleado: " + line + " -> " + e.getMessage());
            return null;
        }
    }
}
