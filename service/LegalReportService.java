package com.nomina.service;

import com.nomina.config.ConfigManager;
import com.nomina.model.Empleado;
import com.nomina.model.ReciboNomina;
import com.nomina.repository.EmpleadoRepository;
import com.nomina.repository.NominaRepository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Servicio para la generación de reportes legales y fiscales venezolanos.
 * <ul>
 *   <li>FAOV Banavih — Fondo de Ahorro Obligatorio para la Vivienda</li>
 *   <li>IVSS Forma 14-02 — Registro/Retiro de trabajadores ante el Seguro Social</li>
 *   <li>Prestaciones Sociales — Art. 142 LOTTT (Garantía + Antigüedad)</li>
 * </ul>
 */
public final class LegalReportService {

    private LegalReportService() {
    }

    // ════════════════════════════════════════════════════════════════
    //  FAOV BANAVIH
    // ════════════════════════════════════════════════════════════════

    /**
     * Genera el contenido del reporte de aportes FAOV Banavih para un mes específico.
     */
    public static String generarContenidoFaov(String mesId) {
        List<ReciboNomina> todosLosRecibos = NominaRepository.obtenerTodo();

        List<ReciboNomina> recibosMes = todosLosRecibos.stream()
                .filter(r -> r.getPeriodoId().startsWith(mesId))
                .toList();

        if (recibosMes.isEmpty()) {
            throw new IllegalStateException("No hay nóminas procesadas en el histórico para el mes: " + mesId);
        }

        Map<String, ConsolidadoEmpleado> consolidados = new HashMap<>();
        for (ReciboNomina r : recibosMes) {
            consolidados.computeIfAbsent(r.getCedula(), k -> new ConsolidadoEmpleado(r.getCedula(), r.getNombreCompleto()))
                    .acumular(r);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# REPORTE FAOV BANAVIH - MES: ").append(mesId).append("\n");
        sb.append("# RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("Nacionalidad;Cedula;Nombre Completo;Salario Mensual (VES);Aporte Trabajador (1%);Aporte Patrono (2%);Aporte Total (3%)\n");

        for (ConsolidadoEmpleado c : consolidados.values()) {
            double faovTrabajador = c.salarioAcumuladoVes * (ConfigManager.getFaovTrabajador() / 100.0);
            double faovPatronal = c.salarioAcumuladoVes * (ConfigManager.getFaovPatronal() / 100.0);
            double totalAporte = faovTrabajador + faovPatronal;

            String nac = "V";
            String cedulaNumerica = c.cedula;
            if (c.cedula.contains("-")) {
                String[] parts = c.cedula.split("-");
                nac = parts[0].trim().toUpperCase();
                cedulaNumerica = parts[1].replace(".", "").trim();
            } else {
                cedulaNumerica = cedulaNumerica.replace(".", "").trim();
            }

            sb.append(String.format("%s;%s;%s;%.2f;%.2f;%.2f;%.2f\n",
                    nac, cedulaNumerica, c.nombre, c.salarioAcumuladoVes,
                    faovTrabajador, faovPatronal, totalAporte));
        }

        return sb.toString();
    }

    public static void exportarFaovAArchivo(String mesId, String filePath) throws IOException {
        String contenido = generarContenidoFaov(mesId);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  IVSS — FORMA 14-02
    // ════════════════════════════════════════════════════════════════

    /**
     * Genera el contenido del formulario IVSS 14-02 (registro de trabajadores activos).
     * Formato oficial simplificado con datos fiscales del patrono.
     */
    public static String generarContenidoIvss() {
        List<Empleado> empleados = EmpleadoRepository.getAll();

        StringBuilder sb = new StringBuilder();
        sb.append("# FORMATO IVSS FORMA 14-02 — REGISTRO DE TRABAJADORES\n");
        sb.append("# RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("# FECHA GENERACIÓN: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        sb.append("# PORCENTAJE IVSS TRABAJADOR: ").append(ConfigManager.getIvssTrabajador()).append("%\n");
        sb.append("# PORCENTAJE IVSS PATRONO ESTIMADO: 9.0 - 11.0%\n\n");
        sb.append("Nac;Cedula;Nombre;Cargo;FechaIngreso;TipoContrato;Estado;SalarioMensualUSD;SalarioMensualVES\n");

        double tasa = ConfigManager.getTasaBcv();

        for (Empleado emp : empleados) {
            String nac = "V";
            String cedulaNum = emp.getCedula();
            if (emp.getCedula().contains("-")) {
                String[] parts = emp.getCedula().split("-");
                nac = parts[0].trim().toUpperCase();
                cedulaNum = parts[1].replace(".", "").trim();
            }

            sb.append(String.format("%s;%s;%s;%s;%s;%s;%s;%.2f;%.2f\n",
                    nac, cedulaNum, emp.getNombreCompleto(), emp.getCargo(),
                    emp.getFechaIngreso(), emp.getTipoContrato(), emp.getEstado(),
                    emp.getSalarioUsd(), emp.getSalarioUsd() * tasa));
        }

        return sb.toString();
    }

    public static void exportarIvssAArchivo(String filePath) throws IOException {
        String contenido = generarContenidoIvss();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }

    /**
     * Genera el contenido del registro de asegurados según Providencia Administrativa 003.
     */
    public static String generarContenidoIvssProvidencia003() {
        List<Empleado> empleados = EmpleadoRepository.getAll();
        double tasa = ConfigManager.getTasaBcv();

        StringBuilder sb = new StringBuilder();
        sb.append("# REGISTRO DE TRABAJADORES ASEGURADOS - PROVIDENCIA ADMINISTRATIVA N° 003\n");
        sb.append("# PATRONO: NÓMINA INTELIGENTE C.A. \n");
        sb.append("# RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("# FECHA DE EMISIÓN: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        sb.append("Nacionalidad;Cédula;Nombre Completo;Fecha Ingreso;Cargo;Salario Mensual (VES);Salario Semanal (VES);Aporte IVSS Trabajador (4%);Aporte IVSS Patrono (9%)\n");

        for (Empleado emp : empleados) {
            if (!"Activo".equalsIgnoreCase(emp.getEstado())) continue;

            double salarioMensualVes = emp.getSalarioUsd() * tasa;
            double salarioSemanalVes = (salarioMensualVes * 12.0) / 52.0;
            double aporteTrabajador = salarioMensualVes * 0.04;
            double aportePatrono = salarioMensualVes * 0.09;

            String nac = "V";
            String cedulaNum = emp.getCedula();
            if (emp.getCedula().contains("-")) {
                String[] parts = emp.getCedula().split("-");
                nac = parts[0].trim().toUpperCase();
                cedulaNum = parts[1].replace(".", "").trim();
            }

            sb.append(String.format("%s;%s;%s;%s;%s;%.2f;%.2f;%.2f;%.2f\n",
                    nac, cedulaNum, emp.getNombreCompleto(), emp.getFechaIngreso(), emp.getCargo(),
                    salarioMensualVes, salarioSemanalVes, aporteTrabajador, aportePatrono));
        }

        return sb.toString();
    }

    public static void exportarIvssProvidencia003AArchivo(String filePath) throws IOException {
        String contenido = generarContenidoIvssProvidencia003();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PRESTACIONES SOCIALES — ART. 142 LOTTT
    // ════════════════════════════════════════════════════════════════

    /**
     * Calcula las prestaciones sociales acumuladas por cada empleado activo.
     * Basado en el Art. 142 LOTTT:
     * <ul>
     *   <li>Garantía: 15 días de salario por cada trimestre de servicio.</li>
     *   <li>Antigüedad adicional: 2 días adicionales por año cumplido (acumulativo desde el 2do año hasta un máximo de 30 días).</li>
     * </ul>
     *
     * @return lista de resultados calculados por empleado
     */
    public static List<PrestacionesResult> calcularPrestaciones() {
        List<PrestacionesResult> resultados = new ArrayList<>();
        double tasa = ConfigManager.getTasaBcv();
        LocalDate hoy = LocalDate.now();

        for (Empleado emp : EmpleadoRepository.getAll()) {
            if (!"Activo".equalsIgnoreCase(emp.getEstado())) continue;

            LocalDate ingreso;
            try {
                ingreso = LocalDate.parse(emp.getFechaIngreso(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                ingreso = LocalDate.of(2024, 1, 15);
            }

            long mesesServicio = ChronoUnit.MONTHS.between(ingreso, hoy);
            int trimestres = (int) (mesesServicio / 3);
            int anosCompletos = (int) (mesesServicio / 12);

            // Salario diario (mensual / 30)
            double salarioDiarioVes = (emp.getSalarioUsd() * tasa) / 30.0;

            // Garantía: 15 días por trimestre
            double diasGarantia = trimestres * 15.0;
            double montoGarantia = diasGarantia * salarioDiarioVes;

            // Antigüedad adicional: 2 días por cada año desde el segundo, acumulativo, max 30 días/año
            double diasAntiguedad = 0;
            if (anosCompletos >= 2) {
                for (int a = 2; a <= anosCompletos; a++) {
                    diasAntiguedad += Math.min(2 * (a - 1), 30);
                }
            }
            double montoAntiguedad = diasAntiguedad * salarioDiarioVes;

            double totalVes = montoGarantia + montoAntiguedad;
            double totalUsd = totalVes / tasa;

            resultados.add(new PrestacionesResult(
                    emp.getCedula(),
                    emp.getNombreCompleto(),
                    emp.getFechaIngreso(),
                    anosCompletos,
                    (int) mesesServicio,
                    trimestres,
                    diasGarantia,
                    montoGarantia,
                    diasAntiguedad,
                    montoAntiguedad,
                    totalVes,
                    totalUsd
            ));
        }

        return resultados;
    }

    public static void exportarPrestacionesAArchivo(String filePath) throws IOException {
        List<PrestacionesResult> resultados = calcularPrestaciones();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("# ACUMULADO DE PRESTACIONES SOCIALES — ART. 142 LOTTT\n");
            bw.write("# RIF PATRONO: " + ConfigManager.getRifPatrono() + "\n");
            bw.write("# TASA BCV APLICADA: " + ConfigManager.getTasaBcv() + "\n");
            bw.write("# FECHA CÁLCULO: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n");
            bw.write("Cedula;Nombre;FechaIngreso;AñosServicio;MesesServicio;Trimestres;DiasGarantia;MontoGarantiaVES;DiasAntiguedad;MontoAntiguedadVES;TotalVES;TotalUSD\n");

            for (PrestacionesResult r : resultados) {
                bw.write(String.format("%s;%s;%s;%d;%d;%d;%.0f;%.2f;%.0f;%.2f;%.2f;%.2f\n",
                        r.cedula, r.nombre, r.fechaIngreso, r.anosServicio, r.mesesServicio, r.trimestres,
                        r.diasGarantia, r.montoGarantiaVes, r.diasAntiguedad, r.montoAntiguedadVes,
                        r.totalVes, r.totalUsd));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PENSIONES (SENIAT)
    // ════════════════════════════════════════════════════════════════

    /**
     * Genera el contenido del reporte de contribución de pensiones patronal (SENIAT) para un mes específico.
     */
    public static String generarContenidoPensiones(String mesId) {
        List<ReciboNomina> todosLosRecibos = NominaRepository.obtenerTodo();

        List<ReciboNomina> recibosMes = todosLosRecibos.stream()
                .filter(r -> r.getPeriodoId().startsWith(mesId))
                .toList();

        if (recibosMes.isEmpty()) {
            throw new IllegalStateException("No hay nóminas procesadas en el histórico para el mes: " + mesId);
        }

        Map<String, ConsolidadoPensiones> consolidados = new HashMap<>();
        for (ReciboNomina r : recibosMes) {
            consolidados.computeIfAbsent(r.getCedula(), k -> new ConsolidadoPensiones(r.getCedula(), r.getNombreCompleto()))
                    .acumular(r);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# REPORTE DECLARACIÓN CONTRIBUCIÓN ESPECIAL DE PENSIONES (SENIAT)\n");
        sb.append("# MES DE DECLARACIÓN: ").append(mesId).append("\n");
        sb.append("# RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("# ALÍCUOTA APLICADA: ").append(ConfigManager.getPensionPatronal()).append("%\n\n");
        sb.append("Nacionalidad;Cedula;Nombre Completo;Base Salarial (USD);Base Salarial (VES);Aporte Patronal (USD);Aporte Patronal (VES)\n");

        double totalBaseUsd = 0;
        double totalBaseVes = 0;
        double totalAporteUsd = 0;
        double totalAporteVes = 0;

        for (ConsolidadoPensiones c : consolidados.values()) {
            double baseUsd = c.sueldoBaseUsd + c.bonosUsd;
            double baseVes = baseUsd * c.tasaBcv;
            double aporteUsd = baseUsd * (ConfigManager.getPensionPatronal() / 100.0);
            double aporteVes = aporteUsd * c.tasaBcv;

            totalBaseUsd += baseUsd;
            totalBaseVes += baseVes;
            totalAporteUsd += aporteUsd;
            totalAporteVes += aporteVes;

            String nac = "V";
            String cedulaNumerica = c.cedula;
            if (c.cedula.contains("-")) {
                String[] parts = c.cedula.split("-");
                nac = parts[0].trim().toUpperCase();
                cedulaNumerica = parts[1].replace(".", "").trim();
            } else {
                cedulaNumerica = cedulaNumerica.replace(".", "").trim();
            }

            sb.append(String.format("%s;%s;%s;%.2f;%.2f;%.2f;%.2f\n",
                    nac, cedulaNumerica, c.nombre, baseUsd, baseVes, aporteUsd, aporteVes));
        }

        sb.append(String.format("\nTOTALES;;;%.2f;%.2f;%.2f;%.2f\n",
                totalBaseUsd, totalBaseVes, totalAporteUsd, totalAporteVes));

        return sb.toString();
    }

    public static void exportarPensionesAArchivo(String mesId, String filePath) throws IOException {
        String contenido = generarContenidoPensiones(mesId);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ARCV — COMPROBANTES DE RETENCIÓN ISLR
    // ════════════════════════════════════════════════════════════════

    /**
     * Genera comprobantes de retención de ISLR (ARCV) para cada empleado activo.
     * Calcula la retención estimada sobre la base salarial anualizada,
     * aplicando la tabla simplificada de ISLR para personas naturales residentes.
     */
    public static String generarComprobantesArcv(int anioFiscal) {
        List<Empleado> empleados = EmpleadoRepository.getAll();
        double tasa = ConfigManager.getTasaBcv();

        StringBuilder sb = new StringBuilder();
        sb.append("# COMPROBANTES DE RETENCIÓN DE ISLR (ARCV) — AÑO FISCAL ").append(anioFiscal).append("\n");
        sb.append("# RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("# PATRONO: NÓMINA INTELIGENTE C.A.\n");
        sb.append("# FECHA EMISIÓN: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        sb.append("Nac;Cédula;Nombre;Salario Anual (USD);Salario Anual (VES);Base Imponible (VES);Retención Estimada (VES);Retención Estimada (USD)\n");

        double salarioMinimo = ConfigManager.getSalarioMinimo();

        for (Empleado emp : empleados) {
            if (!"Activo".equalsIgnoreCase(emp.getEstado())) continue;

            double salarioAnualUsd = emp.getSalarioUsd() * 12.0;
            double salarioAnualVes = salarioAnualUsd * tasa;

            // Desgravamen único simplificado (774 UT aprox.)
            double ut = salarioMinimo > 0 ? salarioMinimo : 130.0;
            double desgravamen = 774.0 * ut;
            double rebajaPersonal = 10.0 * ut;

            double baseImponible = Math.max(0, salarioAnualVes - desgravamen);

            // Tabla simplificada ISLR personas naturales (tarifa N° 1)
            double retencion = 0;
            if (baseImponible > 0 && baseImponible <= 1000 * ut) {
                retencion = baseImponible * 0.06;
            } else if (baseImponible <= 1500 * ut) {
                retencion = (1000 * ut * 0.06) + ((baseImponible - 1000 * ut) * 0.09);
            } else if (baseImponible <= 2000 * ut) {
                retencion = (1000 * ut * 0.06) + (500 * ut * 0.09) + ((baseImponible - 1500 * ut) * 0.12);
            } else if (baseImponible <= 2500 * ut) {
                retencion = (1000 * ut * 0.06) + (500 * ut * 0.09) + (500 * ut * 0.12) + ((baseImponible - 2000 * ut) * 0.16);
            } else {
                retencion = (1000 * ut * 0.06) + (500 * ut * 0.09) + (500 * ut * 0.12) + (500 * ut * 0.16) + ((baseImponible - 2500 * ut) * 0.20);
            }

            retencion = Math.max(0, retencion - rebajaPersonal);
            double retencionUsd = tasa > 0 ? retencion / tasa : 0;

            String nac = "V";
            String cedulaNum = emp.getCedula();
            if (emp.getCedula().contains("-")) {
                String[] parts = emp.getCedula().split("-");
                nac = parts[0].trim().toUpperCase();
                cedulaNum = parts[1].replace(".", "").trim();
            }

            sb.append(String.format("%s;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    nac, cedulaNum, emp.getNombreCompleto(),
                    salarioAnualUsd, salarioAnualVes, baseImponible, retencion, retencionUsd));
        }

        return sb.toString();
    }

    public static void exportarArcvAArchivo(int anioFiscal, String filePath) throws IOException {
        String contenido = generarComprobantesArcv(anioFiscal);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PRESTACIONES DETALLADAS CON INTERESES
    // ════════════════════════════════════════════════════════════════

    /**
     * Calcula las prestaciones sociales detalladas incluyendo intereses sobre el acumulado.
     * Art. 143 LOTTT: los intereses se calculan mensualmente sobre el saldo acumulado
     * a la tasa promedio de depósitos a plazo del BCV (estimada al 12% anual = 1% mensual).
     */
    public static List<PrestacionesDetalladasResult> calcularPrestacionesDetalladas() {
        List<PrestacionesDetalladasResult> resultados = new ArrayList<>();
        double tasa = ConfigManager.getTasaBcv();
        LocalDate hoy = LocalDate.now();
        double tasaInteresMensual = 0.01; // 1% mensual (12% anual / 12)

        for (Empleado emp : EmpleadoRepository.getAll()) {
            if (!"Activo".equalsIgnoreCase(emp.getEstado())) continue;

            LocalDate ingreso;
            try {
                ingreso = LocalDate.parse(emp.getFechaIngreso(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                ingreso = LocalDate.of(2024, 1, 15);
            }

            long mesesServicio = ChronoUnit.MONTHS.between(ingreso, hoy);
            int trimestres = (int) (mesesServicio / 3);
            int anosCompletos = (int) (mesesServicio / 12);

            double salarioDiarioVes = (emp.getSalarioUsd() * tasa) / 30.0;

            // Garantía: 15 días por trimestre
            double diasGarantia = trimestres * 15.0;
            double montoGarantia = diasGarantia * salarioDiarioVes;

            // Antigüedad adicional acumulativa
            double diasAntiguedad = 0;
            if (anosCompletos >= 2) {
                for (int a = 2; a <= anosCompletos; a++) {
                    diasAntiguedad += Math.min(2 * (a - 1), 30);
                }
            }
            double montoAntiguedad = diasAntiguedad * salarioDiarioVes;

            // Intereses sobre prestaciones acumuladas (acumulación mensual)
            double acumulado = montoGarantia + montoAntiguedad;
            double interesesTotales = 0;
            for (int mes = 1; mes <= mesesServicio; mes++) {
                double saldoEstimadoMes = acumulado * ((double) mes / mesesServicio);
                interesesTotales += saldoEstimadoMes * tasaInteresMensual;
            }

            double totalVes = acumulado + interesesTotales;
            double totalUsd = tasa > 0 ? totalVes / tasa : 0;
            double interesesUsd = tasa > 0 ? interesesTotales / tasa : 0;

            resultados.add(new PrestacionesDetalladasResult(
                    emp.getCedula(),
                    emp.getNombreCompleto(),
                    emp.getFechaIngreso(),
                    anosCompletos,
                    (int) mesesServicio,
                    trimestres,
                    diasGarantia,
                    montoGarantia,
                    diasAntiguedad,
                    montoAntiguedad,
                    interesesTotales,
                    interesesUsd,
                    totalVes,
                    totalUsd
            ));
        }

        return resultados;
    }

    public static void exportarPrestacionesDetalladasAArchivo(String filePath) throws IOException {
        List<PrestacionesDetalladasResult> resultados = calcularPrestacionesDetalladas();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("# PRESTACIONES SOCIALES DETALLADAS CON INTERESES — ART. 142/143 LOTTT\n");
            bw.write("# RIF PATRONO: " + ConfigManager.getRifPatrono() + "\n");
            bw.write("# TASA BCV APLICADA: " + ConfigManager.getTasaBcv() + "\n");
            bw.write("# TASA INTERÉS MENSUAL: 1.0% (12% anual)\n");
            bw.write("# FECHA CÁLCULO: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n");
            bw.write("Cedula;Nombre;FechaIngreso;Años;Meses;Trimestres;DiasGarantia;MontoGarantia(VES);DiasAntig;MontoAntig(VES);Intereses(VES);Intereses(USD);TotalVES;TotalUSD\n");

            for (PrestacionesDetalladasResult r : resultados) {
                bw.write(String.format("%s;%s;%s;%d;%d;%d;%.0f;%.2f;%.0f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                        r.cedula, r.nombre, r.fechaIngreso, r.anosServicio, r.mesesServicio, r.trimestres,
                        r.diasGarantia, r.montoGarantiaVes, r.diasAntiguedad, r.montoAntiguedadVes,
                        r.interesesVes, r.interesesUsd, r.totalVes, r.totalUsd));
            }
        }
    }

    // ── Modelos internos ───────────────────────────────────────────

    private static class ConsolidadoPensiones {
        final String cedula;
        final String nombre;
        double sueldoBaseUsd = 0;
        double bonosUsd = 0;
        double tasaBcv = 0;

        ConsolidadoPensiones(String cedula, String nombre) {
            this.cedula = cedula;
            this.nombre = nombre;
        }

        void acumular(ReciboNomina r) {
            this.sueldoBaseUsd += r.getSueldoBasePeriodoUsd();
            this.bonosUsd += r.getBonosExtrasUsd();
            this.tasaBcv = r.getTasaBcv();
        }
    }

    private static class ConsolidadoEmpleado {
        final String cedula;
        final String nombre;
        double salarioAcumuladoVes = 0;

        ConsolidadoEmpleado(String cedula, String nombre) {
            this.cedula = cedula;
            this.nombre = nombre;
        }

        void acumular(ReciboNomina r) {
            this.salarioAcumuladoVes += r.getSueldoBasePeriodoVes();
        }
    }

    /**
     * Resultado del cálculo de prestaciones sociales de un empleado.
     */
    public record PrestacionesResult(
            String cedula,
            String nombre,
            String fechaIngreso,
            int anosServicio,
            int mesesServicio,
            int trimestres,
            double diasGarantia,
            double montoGarantiaVes,
            double diasAntiguedad,
            double montoAntiguedadVes,
            double totalVes,
            double totalUsd
    ) {}

    /**
     * Resultado del cálculo de prestaciones detalladas con intereses.
     */
    public record PrestacionesDetalladasResult(
            String cedula,
            String nombre,
            String fechaIngreso,
            int anosServicio,
            int mesesServicio,
            int trimestres,
            double diasGarantia,
            double montoGarantiaVes,
            double diasAntiguedad,
            double montoAntiguedadVes,
            double interesesVes,
            double interesesUsd,
            double totalVes,
            double totalUsd
    ) {}
}
