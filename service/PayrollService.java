package com.nomina.service;

import com.nomina.config.ConfigManager;
import com.nomina.model.Empleado;
import com.nomina.model.ReciboNomina;
import com.nomina.model.FacturaMercancia;
import com.nomina.repository.MercanciaRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de realizar los cálculos matemáticos y legales de la nómina.
 * Soporta conversión bi-monetaria en tiempo real (USD a VES).
 */
public final class PayrollService {

    private PayrollService() {
    }

    /**
     * Calcula la nómina para una lista de empleados bajo un periodo y tasa BCV dados.
     */
    public static List<ReciboNomina> calcularPeriodo(String periodoId, List<Empleado> empleados, double tasaBcv) {
        List<ReciboNomina> recibos = new ArrayList<>();
        for (Empleado emp : empleados) {
            if (!"Activo".equalsIgnoreCase(emp.getEstado())) {
                continue;
            }
            double mercanciaUsd = calcularDeduccionMercancia(emp.getCedula(), periodoId);
            recibos.add(calcularReciboDetallado(periodoId, emp, tasaBcv, 0, 0, 0, 0, 0, 0, 0, mercanciaUsd));
        }
        return recibos;
    }

    /**
     * Calcula el recibo detallado para un empleado aplicando todas las asignaciones,
     * deducciones y variables del período, según la LOTTT y la configuración global.
     */
    public static ReciboNomina calcularReciboDetallado(
            String periodoId, Empleado emp, double tasaBcv,
            double horasExtras, double horasNocturnas, double diasFeriados,
            double bonosExtrasUsd, double diasNoTrabajados,
            double adelantoVes, double adelantoUsd,
            double deduccionMercanciaUsd) {

        double proporcion = ConfigManager.getProporcionPago();
        // Si el periodo es mensual completo (sufijo -M), la proporción es 1.0 (100%)
        if (periodoId != null && periodoId.endsWith("-M")) {
            proporcion = 1.0;
        }

        double salarioMensualUsd = emp.getSalarioUsd();
        double ingresoBruto = salarioMensualUsd * proporcion;

        // Cálculos de tasas por hora/día (LOTTT)
        double baseDiariaUsd = salarioMensualUsd / 30.0;
        double baseHoraUsd = baseDiariaUsd / 8.0;

        // Asignaciones por variables de periodo en USD
        double heUsd = horasExtras * baseHoraUsd * 1.50; // Surchage 50%
        double hnUsd = horasNocturnas * baseHoraUsd * 0.30; // Recargo nocturno 30%
        double feriadosUsd = diasFeriados * baseDiariaUsd * 1.50; // Recargo feriados 1.50x

        // Deducción por días no trabajados
        double deduccionNoTrabajadosUsd = diasNoTrabajados * baseDiariaUsd;

        // Base imponible en VES para retenciones obligatorias
        double baseCalculableUsd = ingresoBruto - deduccionNoTrabajadosUsd;
        if (baseCalculableUsd < 0) {
            baseCalculableUsd = 0;
        }
        double sueldoBasePeriodoVes = baseCalculableUsd * tasaBcv;

        // Retenciones obligatorias (IVSS y FAOV) en VES
        double ivssPct = ConfigManager.getIvssTrabajador() / 100.0;
        double faovPct = ConfigManager.getFaovTrabajador() / 100.0;

        double ivssVes = sueldoBasePeriodoVes * ivssPct;
        double faovVes = sueldoBasePeriodoVes * faovPct;

        double deduccionesLegalesUsd = (ivssVes + faovVes) / tasaBcv;
        double deduccionesTotalesUsd = deduccionesLegalesUsd + deduccionNoTrabajadosUsd + adelantoUsd + deduccionMercanciaUsd;

        // Cesta ticket quincenal o mensual en USD
        double cestaTicketFijaUsd = ConfigManager.getCestaTicketFijaUsd();
        if (periodoId != null && periodoId.endsWith("-M")) {
            cestaTicketFijaUsd = ConfigManager.getCestaTicketFijaUsd() * 2.0;
        }
        double cestaTicketVes = cestaTicketFijaUsd * tasaBcv;

        // Cálculo del neto USD y VES con los adelantos aplicados
        double netoUsd = (ingresoBruto + bonosExtrasUsd + heUsd + hnUsd + feriadosUsd) - deduccionesTotalesUsd - cestaTicketFijaUsd;
        double netoVes = (netoUsd * tasaBcv) + cestaTicketVes - adelantoVes;

        return new ReciboNomina(
                periodoId,
                emp.getCedula(),
                emp.getNombreCompleto(),
                salarioMensualUsd,
                tasaBcv,
                ingresoBruto,
                ingresoBruto * tasaBcv,
                cestaTicketVes,
                ivssVes,
                faovVes,
                netoVes,
                netoUsd,
                horasExtras,
                horasNocturnas,
                diasFeriados,
                bonosExtrasUsd,
                diasNoTrabajados,
                adelantoVes,
                adelantoUsd,
                deduccionMercanciaUsd,
                deduccionMercanciaUsd * tasaBcv
        );
    }

    /**
     * Calcula la deducción de mercancía acumulada para un empleado en un periodo de nómina.
     */
    public static double calcularDeduccionMercancia(String cedula, String periodoId) {
        String periodEnd = getPeriodEndDate(periodoId);
        double totalDeduccion = 0;

        List<FacturaMercancia> facturas = MercanciaRepository.obtenerPorEmpleado(cedula);
        for (FacturaMercancia f : facturas) {
            if ("PAGADA".equalsIgnoreCase(f.getEstado()) || f.isPostergada()) {
                continue;
            }

            // Verificar si el vencimiento es menor o igual al fin de periodo
            if (f.getFechaVencimiento().compareTo(periodEnd) > 0) {
                continue;
            }

            // Excluir si la factura se generó hace menos de 5 días de la fecha de cierre
            long days = getDaysBetween(f.getFechaEmision(), periodEnd);
            if (days < 5) {
                continue;
            }

            totalDeduccion += (f.getMontoTotal() - f.getMontoAbonado());
        }
        return totalDeduccion;
    }

    /**
     * Determina la fecha de fin de período (corte de nómina) a partir del ID del período.
     */
    public static String getPeriodEndDate(String periodoId) {
        try {
            String[] parts = periodoId.split("-");
            if (parts.length < 3) return "2026-06-15";
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            String type = parts[2];

            if ("Q1".equalsIgnoreCase(type)) {
                return String.format("%d-%02d-15", year, month);
            } else {
                java.time.LocalDate lastDay = java.time.LocalDate.of(year, month, 1)
                        .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                return lastDay.toString();
            }
        } catch (Exception e) {
            return "2026-06-15";
        }
    }

    /**
     * Calcula la diferencia en días entre dos fechas (formato YYYY-MM-DD).
     */
    public static long getDaysBetween(String date1, String date2) {
        try {
            java.time.LocalDate d1 = java.time.LocalDate.parse(date1);
            java.time.LocalDate d2 = java.time.LocalDate.parse(date2);
            return java.time.temporal.ChronoUnit.DAYS.between(d1, d2);
        } catch (Exception e) {
            return 0;
        }
    }
}
