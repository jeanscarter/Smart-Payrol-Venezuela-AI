package com.nomina.repository;

import com.nomina.model.ReciboNomina;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repositorio para la gestión y persistencia histórica de las nóminas procesadas.
 * Guarda los datos en nominas_procesadas.csv.
 */
public final class NominaRepository {

    private static final String FILE_NAME = "nominas_procesadas.csv";
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
     * Carga el historial de nóminas desde el archivo CSV.
     */
    public static void load() {
        historico.clear();
        Path path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line = br.readLine(); // Saltar header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                ReciboNomina recibo = parseCsvLine(line);
                if (recibo != null) {
                    historico.add(recibo);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al cargar histórico de nóminas: " + e.getMessage());
        }
    }

    /**
     * Guarda el historial de nóminas completo en el archivo CSV.
     */
    public static void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            bw.write("PeriodoId,Cedula,NombreCompleto,SalarioMensualUsd,TasaBcv,SueldoBasePeriodoUsd," +
                    "SueldoBasePeriodoVes,CestaTicketVes,IvssVes,FaovVes,NetoVes,NetoUsd");
            bw.newLine();
            for (ReciboNomina recibo : historico) {
                bw.write(recibo.toString());
                bw.newLine();
            }
            notifyListeners();
        } catch (IOException e) {
            System.err.println("Error al guardar histórico de nóminas: " + e.getMessage());
        }
    }

    /**
     * Registra o sobreescribe los recibos de un periodo específico.
     */
    public static void guardarPeriodo(String periodoId, List<ReciboNomina> recibos) {
        // Eliminar registros anteriores de este mismo periodo si existen
        historico.removeIf(r -> r.getPeriodoId().equalsIgnoreCase(periodoId));
        historico.addAll(recibos);
        save();
    }

    /**
     * Retorna todos los recibos de un periodo.
     */
    public static List<ReciboNomina> obtenerPorPeriodo(String periodoId) {
        return historico.stream()
                .filter(r -> r.getPeriodoId().equalsIgnoreCase(periodoId))
                .collect(Collectors.toList());
    }

    /**
     * Retorna los IDs de todos los periodos procesados de forma única.
     */
    public static List<String> obtenerPeriodosProcesados() {
        return historico.stream()
                .map(ReciboNomina::getPeriodoId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Retorna el histórico completo de recibos.
     */
    public static List<ReciboNomina> obtenerTodo() {
        return new ArrayList<>(historico);
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

    private static ReciboNomina parseCsvLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length < 12) return null;

            String periodoId = parts[0].trim();
            String cedula = parts[1].trim();
            String nombreCompleto = parts[2].trim();
            double salarioMensualUsd = Double.parseDouble(parts[3].trim());
            double tasaBcv = Double.parseDouble(parts[4].trim());
            double sueldoBasePeriodoUsd = Double.parseDouble(parts[5].trim());
            double sueldoBasePeriodoVes = Double.parseDouble(parts[6].trim());
            double cestaTicketVes = Double.parseDouble(parts[7].trim());
            double ivssVes = Double.parseDouble(parts[8].trim());
            double faovVes = Double.parseDouble(parts[9].trim());
            double netoVes = Double.parseDouble(parts[10].trim());
            double netoUsd = Double.parseDouble(parts[11].trim());

            double horasExtras = 0;
            double horasNocturnas = 0;
            double diasFeriados = 0;
            double bonosExtrasUsd = 0;
            double diasNoTrabajados = 0;
            double adelantoVes = 0;
            double adelantoUsd = 0;

            if (parts.length >= 19) {
                horasExtras = Double.parseDouble(parts[12].trim());
                horasNocturnas = Double.parseDouble(parts[13].trim());
                diasFeriados = Double.parseDouble(parts[14].trim());
                bonosExtrasUsd = Double.parseDouble(parts[15].trim());
                diasNoTrabajados = Double.parseDouble(parts[16].trim());
                adelantoVes = Double.parseDouble(parts[17].trim());
                adelantoUsd = Double.parseDouble(parts[18].trim());
            }

            return new ReciboNomina(periodoId, cedula, nombreCompleto, salarioMensualUsd, tasaBcv,
                    sueldoBasePeriodoUsd, sueldoBasePeriodoVes, cestaTicketVes, ivssVes, faovVes, netoVes, netoUsd,
                    horasExtras, horasNocturnas, diasFeriados, bonosExtrasUsd, diasNoTrabajados, adelantoVes, adelantoUsd);
        } catch (Exception e) {
            System.err.println("Error al parsear línea de recibo: " + line + " -> " + e.getMessage());
            return null;
        }
    }
}
