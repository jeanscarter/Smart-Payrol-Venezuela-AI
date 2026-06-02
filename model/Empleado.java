package com.nomina.model;

/**
 * Modelo de datos para un empleado.
 */
public class Empleado {

    private String cedula;
    private String nombreCompleto;
    private String cargo;
    private String departamento;
    private double salarioUsd;
    private String tipoContrato;
    private String estado;
    private String fechaIngreso; // Formato YYYY-MM-DD

    public Empleado() {
    }

    public Empleado(String cedula, String nombreCompleto, String cargo, String departamento, double salarioUsd, String tipoContrato, String estado) {
        this(cedula, nombreCompleto, cargo, departamento, salarioUsd, tipoContrato, estado, "2024-01-15");
    }

    public Empleado(String cedula, String nombreCompleto, String cargo, String departamento, double salarioUsd, String tipoContrato, String estado, String fechaIngreso) {
        this.cedula = cedula;
        this.nombreCompleto = nombreCompleto;
        this.cargo = cargo;
        this.departamento = departamento;
        this.salarioUsd = salarioUsd;
        this.tipoContrato = tipoContrato;
        this.estado = estado;
        this.fechaIngreso = fechaIngreso;
    }

    // --- GETTERS & SETTERS ---

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

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public double getSalarioUsd() {
        return salarioUsd;
    }

    public void setSalarioUsd(double salarioUsd) {
        this.salarioUsd = salarioUsd;
    }

    public String getTipoContrato() {
        return tipoContrato;
    }

    public void setTipoContrato(String tipoContrato) {
        this.tipoContrato = tipoContrato;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(String fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%.2f,%s,%s,%s",
                cedula, nombreCompleto, cargo, departamento, salarioUsd, tipoContrato, estado, fechaIngreso);
    }
}
