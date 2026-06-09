package com.nomina.model;

/**
 * Representa el recibo de nómina calculado para un empleado en un período específico.
 */
public class ReciboNomina {
    private String periodoId; // Ejemplo: "2026-06-Q1" (1ra Quincena de Junio 2026)
    private String cedula;
    private String nombreCompleto;
    private double salarioMensualUsd;
    private double tasaBcv;
    private double sueldoBasePeriodoUsd;
    private double sueldoBasePeriodoVes;
    private double cestaTicketVes;
    private double ivssVes;
    private double faovVes;
    private double netoVes;
    private double netoUsd;

    // Nuevos campos de variables de periodo (Fase 5)
    private double horasExtras;
    private double horasNocturnas;
    private double diasFeriados;
    private double bonosExtrasUsd;
    private double diasNoTrabajados;
    private double adelantoVes;
    private double adelantoUsd;

    // Campos de deducción de mercancía
    private double deduccionMercanciaUsd;
    private double deduccionMercanciaVes;

    public ReciboNomina() {
    }

    public ReciboNomina(String periodoId, String cedula, String nombreCompleto, double salarioMensualUsd,
                        double tasaBcv, double sueldoBasePeriodoUsd, double sueldoBasePeriodoVes,
                        double cestaTicketVes, double ivssVes, double faovVes, double netoVes, double netoUsd) {
        this(periodoId, cedula, nombreCompleto, salarioMensualUsd, tasaBcv, sueldoBasePeriodoUsd, sueldoBasePeriodoVes,
             cestaTicketVes, ivssVes, faovVes, netoVes, netoUsd, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0);
    }

    public ReciboNomina(String periodoId, String cedula, String nombreCompleto, double salarioMensualUsd,
                        double tasaBcv, double sueldoBasePeriodoUsd, double sueldoBasePeriodoVes,
                        double cestaTicketVes, double ivssVes, double faovVes, double netoVes, double netoUsd,
                        double horasExtras, double horasNocturnas, double diasFeriados, double bonosExtrasUsd,
                        double diasNoTrabajados, double adelantoVes, double adelantoUsd) {
        this(periodoId, cedula, nombreCompleto, salarioMensualUsd, tasaBcv, sueldoBasePeriodoUsd, sueldoBasePeriodoVes,
             cestaTicketVes, ivssVes, faovVes, netoVes, netoUsd, horasExtras, horasNocturnas, diasFeriados, bonosExtrasUsd,
             diasNoTrabajados, adelantoVes, adelantoUsd, 0.0, 0.0);
    }

    public ReciboNomina(String periodoId, String cedula, String nombreCompleto, double salarioMensualUsd,
                        double tasaBcv, double sueldoBasePeriodoUsd, double sueldoBasePeriodoVes,
                        double cestaTicketVes, double ivssVes, double faovVes, double netoVes, double netoUsd,
                        double horasExtras, double horasNocturnas, double diasFeriados, double bonosExtrasUsd,
                        double diasNoTrabajados, double adelantoVes, double adelantoUsd,
                        double deduccionMercanciaUsd, double deduccionMercanciaVes) {
        this.periodoId = periodoId;
        this.cedula = cedula;
        this.nombreCompleto = nombreCompleto;
        this.salarioMensualUsd = salarioMensualUsd;
        this.tasaBcv = tasaBcv;
        this.sueldoBasePeriodoUsd = sueldoBasePeriodoUsd;
        this.sueldoBasePeriodoVes = sueldoBasePeriodoVes;
        this.cestaTicketVes = cestaTicketVes;
        this.ivssVes = ivssVes;
        this.faovVes = faovVes;
        this.netoVes = netoVes;
        this.netoUsd = netoUsd;
        this.horasExtras = horasExtras;
        this.horasNocturnas = horasNocturnas;
        this.diasFeriados = diasFeriados;
        this.bonosExtrasUsd = bonosExtrasUsd;
        this.diasNoTrabajados = diasNoTrabajados;
        this.adelantoVes = adelantoVes;
        this.adelantoUsd = adelantoUsd;
        this.deduccionMercanciaUsd = deduccionMercanciaUsd;
        this.deduccionMercanciaVes = deduccionMercanciaVes;
    }

    // --- GETTERS & SETTERS ---

    public String getPeriodoId() {
        return periodoId;
    }

    public void setPeriodoId(String periodoId) {
        this.periodoId = periodoId;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public double getSalarioMensualUsd() {
        return salarioMensualUsd;
    }

    public void setSalarioMensualUsd(double salarioMensualUsd) {
        this.salarioMensualUsd = salarioMensualUsd;
    }

    public double getTasaBcv() {
        return tasaBcv;
    }

    public void setTasaBcv(double tasaBcv) {
        this.tasaBcv = tasaBcv;
    }

    public double getSueldoBasePeriodoUsd() {
        return sueldoBasePeriodoUsd;
    }

    public void setSueldoBasePeriodoUsd(double sueldoBasePeriodoUsd) {
        this.sueldoBasePeriodoUsd = sueldoBasePeriodoUsd;
    }

    public double getSueldoBasePeriodoVes() {
        return sueldoBasePeriodoVes;
    }

    public void setSueldoBasePeriodoVes(double sueldoBasePeriodoVes) {
        this.sueldoBasePeriodoVes = sueldoBasePeriodoVes;
    }

    public double getCestaTicketVes() {
        return cestaTicketVes;
    }

    public void setCestaTicketVes(double cestaTicketVes) {
        this.cestaTicketVes = cestaTicketVes;
    }

    public double getIvssVes() {
        return ivssVes;
    }

    public void setIvssVes(double ivssVes) {
        this.ivssVes = ivssVes;
    }

    public double getFaovVes() {
        return faovVes;
    }

    public void setFaovVes(double faovVes) {
        this.faovVes = faovVes;
    }

    public double getNetoVes() {
        return netoVes;
    }

    public void setNetoVes(double netoVes) {
        this.netoVes = netoVes;
    }

    public double getNetoUsd() {
        return netoUsd;
    }

    public void setNetoUsd(double netoUsd) {
        this.netoUsd = netoUsd;
    }

    public double getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(double horasExtras) {
        this.horasExtras = horasExtras;
    }

    public double getHorasNocturnas() {
        return horasNocturnas;
    }

    public void setHorasNocturnas(double horasNocturnas) {
        this.horasNocturnas = horasNocturnas;
    }

    public double getDiasFeriados() {
        return diasFeriados;
    }

    public void setDiasFeriados(double diasFeriados) {
        this.diasFeriados = diasFeriados;
    }

    public double getBonosExtrasUsd() {
        return bonosExtrasUsd;
    }

    public void setBonosExtrasUsd(double bonosExtrasUsd) {
        this.bonosExtrasUsd = bonosExtrasUsd;
    }

    public double getDiasNoTrabajados() {
        return diasNoTrabajados;
    }

    public void setDiasNoTrabajados(double diasNoTrabajados) {
        this.diasNoTrabajados = diasNoTrabajados;
    }

    public double getAdelantoVes() {
        return adelantoVes;
    }

    public void setAdelantoVes(double adelantoVes) {
        this.adelantoVes = adelantoVes;
    }

    public double getAdelantoUsd() {
        return adelantoUsd;
    }

    public void setAdelantoUsd(double adelantoUsd) {
        this.adelantoUsd = adelantoUsd;
    }

    public double getDeduccionMercanciaUsd() {
        return deduccionMercanciaUsd;
    }

    public void setDeduccionMercanciaUsd(double deduccionMercanciaUsd) {
        this.deduccionMercanciaUsd = deduccionMercanciaUsd;
    }

    public double getDeduccionMercanciaVes() {
        return deduccionMercanciaVes;
    }

    public void setDeduccionMercanciaVes(double deduccionMercanciaVes) {
        this.deduccionMercanciaVes = deduccionMercanciaVes;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%.2f,%.4f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                periodoId, cedula, nombreCompleto, salarioMensualUsd, tasaBcv, sueldoBasePeriodoUsd,
                sueldoBasePeriodoVes, cestaTicketVes, ivssVes, faovVes, netoVes, netoUsd,
                horasExtras, horasNocturnas, diasFeriados, bonosExtrasUsd, diasNoTrabajados, adelantoVes, adelantoUsd,
                deduccionMercanciaUsd, deduccionMercanciaVes);
    }
}
