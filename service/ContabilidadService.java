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
import java.util.*;

/**
 * Servicio encargado de generar reportes de control contable y de integración.
 * Permite resumir asientos de nómina por departamento y preparar declaraciones del MinPPTRASS.
 */
public final class ContabilidadService {

    private ContabilidadService() {
    }

    public static class ResumenDepartamento {
        public String departamento;
        public double sueldosVes = 0;
        public double cestaTicketVes = 0;
        public double heVes = 0;
        public double hnVes = 0;
        public double feriadosVes = 0;
        public double bonosVes = 0;
        public double deduccionNoTrabajadosVes = 0;
        public double adelantoVes = 0;
        
        public double ivssTrabajadorVes = 0;
        public double faovTrabajadorVes = 0;
        public double ivssPatronalVes = 0;
        public double faovPatronalVes = 0;
        public double pensionPatronalVes = 0;
        
        public double netoVes = 0;
    }

    /**
     * Calcula el resumen contable agrupado por departamento para un periodo.
     */
    public static Map<String, ResumenDepartamento> calcularResumenContable(String periodoId) {
        List<ReciboNomina> todosLosRecibos = NominaRepository.obtenerTodo();
        List<ReciboNomina> recibosPeriodo = todosLosRecibos.stream()
                .filter(r -> r.getPeriodoId().equals(periodoId))
                .toList();

        List<Empleado> empleados = EmpleadoRepository.getAll();
        Map<String, String> empDeptMap = new HashMap<>();
        for (Empleado emp : empleados) {
            empDeptMap.put(emp.getCedula(), emp.getDepartamento());
        }

        Map<String, ResumenDepartamento> resumen = new HashMap<>();

        for (ReciboNomina r : recibosPeriodo) {
            String dept = empDeptMap.getOrDefault(r.getCedula(), "General");
            ResumenDepartamento rd = resumen.computeIfAbsent(dept, k -> {
                ResumenDepartamento newRd = new ResumenDepartamento();
                newRd.departamento = k;
                return newRd;
            });

            double tasa = r.getTasaBcv();
            rd.sueldosVes += r.getSueldoBasePeriodoVes();
            rd.cestaTicketVes += r.getCestaTicketVes();
            
            // Re-calculo de asignaciones por variables de periodo en VES
            double baseDiariaUsd = r.getSalarioMensualUsd() / 30.0;
            double baseHoraUsd = baseDiariaUsd / 8.0;
            
            double heVes = r.getHorasExtras() * baseHoraUsd * 1.50 * tasa;
            double hnVes = r.getHorasNocturnas() * baseHoraUsd * 0.30 * tasa;
            double feriadosVes = r.getDiasFeriados() * baseDiariaUsd * 1.50 * tasa;
            double bonosVes = r.getBonosExtrasUsd() * tasa;
            double noTrabajadosVes = r.getDiasNoTrabajados() * baseDiariaUsd * tasa;
            double adelantoVes = r.getAdelantoVes() + (r.getAdelantoUsd() * tasa);

            rd.heVes += heVes;
            rd.hnVes += hnVes;
            rd.feriadosVes += feriadosVes;
            rd.bonosVes += bonosVes;
            rd.deduccionNoTrabajadosVes += noTrabajadosVes;
            rd.adelantoVes += adelantoVes;

            // Retenciones obligatorias (IVSS y FAOV)
            rd.ivssTrabajadorVes += r.getIvssVes();
            rd.faovTrabajadorVes += r.getFaovVes();

            // Aportes Patronales en VES
            double baseAporteVes = r.getSueldoBasePeriodoVes() - noTrabajadosVes;
            if (baseAporteVes < 0) baseAporteVes = 0;

            rd.ivssPatronalVes += baseAporteVes * 0.09;
            rd.faovPatronalVes += baseAporteVes * 0.02;
            rd.pensionPatronalVes += (r.getSueldoBasePeriodoUsd() + r.getBonosExtrasUsd()) * (ConfigManager.getPensionPatronal() / 100.0) * tasa;

            rd.netoVes += r.getNetoVes();
        }

        return resumen;
    }

    /**
     * Genera el contenido del asiento contable de nómina.
     */
    public static String generarContenidoAsientoContable(String periodoId) {
        Map<String, ResumenDepartamento> resumen = calcularResumenContable(periodoId);
        if (resumen.isEmpty()) {
            throw new IllegalStateException("No hay datos calculados para el período: " + periodoId);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================\n");
        sb.append("   ASIENTO CONTABLE DE NÓMINA (INTEGRACIÓN ERP) - PERÍODO: ").append(periodoId).append("\n");
        sb.append("   RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("   FECHA DE GENERACIÓN: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        sb.append("=========================================================================\n\n");
        
        sb.append(String.format("%-12s | %-45s | %18s | %18s\n", "CÓDIGO CTA", "DESCRIPCIÓN DE LA CUENTA / DEPARTAMENTO", "DEBE (VES)", "HABER (VES)"));
        sb.append("-------------+----------------------------------------------+--------------------+--------------------\n");

        double totalDebe = 0;
        double totalHaber = 0;

        double totalRetIvss = 0;
        double totalRetFaov = 0;
        double totalApoIvss = 0;
        double totalApoFaov = 0;
        double totalApoPension = 0;
        double totalNeto = 0;

        for (ResumenDepartamento rd : resumen.values()) {
            double gastoPersonalVes = rd.sueldosVes + rd.heVes + rd.hnVes + rd.feriadosVes + rd.bonosVes - rd.deduccionNoTrabajadosVes;
            
            // Gasto de Salarios y Sueldos por Departamento (DEBE)
            sb.append(String.format("%-12s | Gasto de Salarios - Depto: %-20s | %18.2f | %18.2f\n",
                    "5.1.01.01", rd.departamento, gastoPersonalVes, 0.0));
            totalDebe += gastoPersonalVes;

            // Gasto de Cesta Ticket por Departamento (DEBE)
            sb.append(String.format("%-12s | Gasto Cesta Ticket - Depto: %-19s | %18.2f | %18.2f\n",
                    "5.1.01.02", rd.departamento, rd.cestaTicketVes, 0.0));
            totalDebe += rd.cestaTicketVes;

            // Gasto Aportes Patronales (DEBE)
            sb.append(String.format("%-12s | Gasto Aporte Pat. IVSS - Depto: %-13s | %18.2f | %18.2f\n",
                    "5.1.01.03", rd.departamento, rd.ivssPatronalVes, 0.0));
            totalDebe += rd.ivssPatronalVes;

            sb.append(String.format("%-12s | Gasto Aporte Pat. FAOV - Depto: %-13s | %18.2f | %18.2f\n",
                    "5.1.01.04", rd.departamento, rd.faovPatronalVes, 0.0));
            totalDebe += rd.faovPatronalVes;

            sb.append(String.format("%-12s | Gasto Aporte Pat. Pensión - Depto: %-10s | %18.2f | %18.2f\n",
                    "5.1.01.05", rd.departamento, rd.pensionPatronalVes, 0.0));
            totalDebe += rd.pensionPatronalVes;

            // Acumular Haberes
            totalRetIvss += rd.ivssTrabajadorVes;
            totalRetFaov += rd.faovTrabajadorVes;
            totalApoIvss += rd.ivssPatronalVes;
            totalApoFaov += rd.faovPatronalVes;
            totalApoPension += rd.pensionPatronalVes;
            totalNeto += rd.netoVes;
        }

        sb.append("-------------+----------------------------------------------+--------------------+--------------------\n");
        // Escribir los Pasivos
        sb.append(String.format("%-12s | Retenciones IVSS por Pagar (4%% Trab.)       | %18.2f | %18.2f\n",
                "2.1.03.01", 0.0, totalRetIvss));
        totalHaber += totalRetIvss;

        sb.append(String.format("%-12s | Retenciones FAOV por Pagar (1%% Trab.)       | %18.2f | %18.2f\n",
                "2.1.03.02", 0.0, totalRetFaov));
        totalHaber += totalRetFaov;

        sb.append(String.format("%-12s | Aportes Patronales IVSS por Pagar (9%% Pat.)  | %18.2f | %18.2f\n",
                "2.1.03.03", 0.0, totalApoIvss));
        totalHaber += totalApoIvss;

        sb.append(String.format("%-12s | Aportes Patronales FAOV por Pagar (2%% Pat.)  | %18.2f | %18.2f\n",
                "2.1.03.04", 0.0, totalApoFaov));
        totalHaber += totalApoFaov;

        sb.append(String.format("%-12s | Aporte Especial Pensiones SENIAT por Pagar    | %18.2f | %18.2f\n",
                "2.1.03.05", 0.0, totalApoPension));
        totalHaber += totalApoPension;

        // Banco / Sueldos por Pagar
        sb.append(String.format("%-12s | Nómina y Sueldos Netos por Pagar (Banco)     | %18.2f | %18.2f\n",
                "2.1.01.01", 0.0, totalNeto));
        totalHaber += totalNeto;

        sb.append("-------------+----------------------------------------------+--------------------+--------------------\n");
        sb.append(String.format("%-12s | %-45s | %18.2f | %18.2f\n",
                "TOTALES", "CUADRE DE ASIENTO CONTABLE", totalDebe, totalHaber));
        sb.append("=========================================================================\n");

        return sb.toString();
    }

    public static void exportarAsientoContable(String periodoId, String filePath) throws IOException {
        String contenido = generarContenidoAsientoContable(periodoId);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }

    /**
     * Genera la Declaración Trimestral para el Ministerio del Trabajo (MinPPTRASS)
     * de acuerdo a los recibos acumulados en el trimestre seleccionado.
     */
    public static String generarContenidoTrimestralMinPPTRASS(int anio, int trimestre) {
        List<ReciboNomina> todosLosRecibos = NominaRepository.obtenerTodo();
        
        List<String> meses = switch (trimestre) {
            case 1 -> List.of("01", "02", "03");
            case 2 -> List.of("04", "05", "06");
            case 3 -> List.of("07", "08", "09");
            case 4 -> List.of("10", "11", "12");
            default -> throw new IllegalArgumentException("Trimestre inválido: " + trimestre);
        };

        String prefijoTrim = anio + "-";
        List<ReciboNomina> recibosTrimestre = todosLosRecibos.stream()
                .filter(r -> {
                    String pId = r.getPeriodoId();
                    if (pId == null || pId.length() < 7) return false;
                    if (!pId.startsWith(prefijoTrim)) return false;
                    String mesPart = pId.substring(5, 7);
                    return meses.contains(mesPart);
                })
                .toList();

        int totalEmpleados = (int) EmpleadoRepository.getAll().stream()
                .filter(e -> "Activo".equalsIgnoreCase(e.getEstado()))
                .count();

        double totalSueldosVes = 0;
        double totalCestaTicketVes = 0;
        double totalHorasExtras = 0;
        double totalHorasExtrasVes = 0;
        double totalOtrasAsignacionesVes = 0;
        double totalRetencionesVes = 0;

        for (ReciboNomina r : recibosTrimestre) {
            double tasa = r.getTasaBcv();
            totalSueldosVes += r.getSueldoBasePeriodoVes();
            totalCestaTicketVes += r.getCestaTicketVes();
            totalHorasExtras += r.getHorasExtras();
            
            double heVes = r.getHorasExtras() * (r.getSalarioMensualUsd() / 30.0 / 8.0) * 1.50 * tasa;
            totalHorasExtrasVes += heVes;

            double hnVes = r.getHorasNocturnas() * (r.getSalarioMensualUsd() / 30.0 / 8.0) * 0.30 * tasa;
            double feriadosVes = r.getDiasFeriados() * (r.getSalarioMensualUsd() / 30.0) * 1.50 * tasa;
            double bonosVes = r.getBonosExtrasUsd() * tasa;
            totalOtrasAsignacionesVes += hnVes + feriadosVes + bonosVes;

            totalRetencionesVes += r.getIvssVes() + r.getFaovVes();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================\n");
        sb.append("  DECLARACIÓN TRIMESTRAL DE EMPLEO, HORAS Y SALARIOS (MINPPTRASS)\n");
        sb.append("  MINISTERIO DEL PODER POPULAR PARA EL PROCESO SOCIAL DE TRABAJO (MPPPST)\n");
        sb.append("  AÑO FISCAL: ").append(anio).append("   |   TRIMESTRE: ").append(trimestre).append("\n");
        sb.append("  RIF PATRONO: ").append(ConfigManager.getRifPatrono()).append("\n");
        sb.append("  FECHA GENERACIÓN: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        sb.append("=========================================================================\n\n");

        sb.append("1. RESUMEN DE FUERZA LABORAL ACTIVA\n");
        sb.append("-------------------------------------------------------------------------\n");
        sb.append(String.format("   Número de trabajadores activos al cierre del trimestre: %d\n\n", totalEmpleados));

        sb.append("2. ESTADÍSTICAS DE SALARIOS Y COMPENSACIONES (VES)\n");
        sb.append("-------------------------------------------------------------------------\n");
        sb.append(String.format("   - Salarios Base Acumulados:             Bs. %,.2f\n", totalSueldosVes));
        sb.append(String.format("   - Cesta Ticket Socialista Acumulado:    Bs. %,.2f\n", totalCestaTicketVes));
        sb.append(String.format("   - Monto Pagado por Horas Extras:        Bs. %,.2f\n", totalHorasExtrasVes));
        sb.append(String.format("   - Otras Asignaciones (HN/Feriados/Bon):  Bs. %,.2f\n", totalOtrasAsignacionesVes));
        sb.append(String.format("   - Total Retenciones Legales (IVSS/FAOV): Bs. %,.2f\n", totalRetencionesVes));
        sb.append(String.format("   - TOTAL REMUNERACIÓN TRIMESTRAL BRUTA:  Bs. %,.2f\n\n", (totalSueldosVes + totalHorasExtrasVes + totalOtrasAsignacionesVes)));

        sb.append("3. REGISTRO DE TIEMPO Y JORNADAS TRABAJADAS\n");
        sb.append("-------------------------------------------------------------------------\n");
        sb.append(String.format("   - Total Horas Extras Laboradas:         %,.1f horas\n", totalHorasExtras));
        sb.append("   - Jornada ordinaria máxima promedio:     8 horas diarias / 40 semanales (diurna)\n");
        sb.append("=========================================================================\n");
        sb.append("   Certifico que los datos suministrados coinciden fielmente con los libros\n");
        sb.append("   auxiliares de nómina y contabilidad de la empresa.\n\n");
        sb.append("   Firma y Sello del Patrono: _____________________________________________\n");

        return sb.toString();
    }

    public static void exportarTrimestralMinPPTRASS(int anio, int trimestre, String filePath) throws IOException {
        String contenido = generarContenidoTrimestralMinPPTRASS(anio, trimestre);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(contenido);
        }
    }
}
