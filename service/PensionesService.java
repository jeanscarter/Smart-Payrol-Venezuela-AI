package com.nomina.service;

import com.nomina.config.ConfigManager;
import com.nomina.model.ReciboNomina;

/**
 * Servicio para el cálculo de la Contribución Especial para la Protección de las Pensiones.
 * Según ley de Venezuela (decreto oficial), el aporte patronal se aplica sobre la base salarial de los empleados.
 */
public final class PensionesService {

    private PensionesService() {
    }

    /**
     * Calcula la contribución patronal para un recibo.
     * La base salarial para pensiones incluye el salario base del periodo + bonos extras.
     */
    public static double calcularAportePatronal(ReciboNomina recibo) {
        double baseCalculableUsd = recibo.getSueldoBasePeriodoUsd() + recibo.getBonosExtrasUsd();
        double pctAporte = ConfigManager.getPensionPatronal() / 100.0;
        return baseCalculableUsd * pctAporte;
    }

    /**
     * Calcula el aporte patronal en VES.
     */
    public static double calcularAportePatronalVes(ReciboNomina recibo) {
        return calcularAportePatronal(recibo) * recibo.getTasaBcv();
    }
}
