package com.nomina.service;

import com.nomina.config.ConfigManager;
import com.nomina.model.Empleado;
import com.nomina.model.ReciboNomina;

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
     * Método básico por compatibilidad.
     */
    public static List<ReciboNomina> calcularPeriodo(String periodoId, List<Empleado> empleados, double tasaBcv) {
        List<ReciboNomina> recibos = new ArrayList<>();
        for (Empleado emp : empleados) {
            if (!"Activo".equalsIgnoreCase(emp.getEstado())) {
                continue;
            }
            recibos.add(calcularReciboDetallado(periodoId, emp, tasaBcv, 0, 0, 0, 0, 0, 0, 0));
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
            double adelantoVes, double adelantoUsd) {

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
        double deduccionesTotalesUsd = deduccionesLegalesUsd + deduccionNoTrabajadosUsd + adelantoUsd;

        // Cesta ticket quincenal o mensual en USD
        double cestaTicketFijaUsd = ConfigManager.getCestaTicketFijaUsd();
        if (periodoId != null && periodoId.endsWith("-M")) {
            cestaTicketFijaUsd = ConfigManager.getCestaTicket(); // si es mensual completo se usa el total
            // Si el total está guardado en VES, se convierte a USD, pero cestaTicketFijaUsd es USD fijo
            // Si la cestaTicket mensual en VES es por ejemplo 1407.00, podemos calcular la tasa.
            // Para mantener consistencia con cestaTicketFijaUsd, si es mensual es el doble de la fija quincenal.
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
                adelantoUsd
        );
    }
}
