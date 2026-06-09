package com.nomina.model;

/**
 * Modelo de datos para las facturas de mercancía pendientes por cobrar a empleados.
 */
public class FacturaMercancia {
    private int id;
    private String cedulaEmpleado;
    private String numeroFactura;
    private double montoTotal;
    private double montoAbonado;
    private String fechaEmision;      // Formato YYYY-MM-DD
    private String fechaVencimiento;  // Formato YYYY-MM-DD
    private boolean postergada;
    private String estado;            // "PENDIENTE", "ABONANDO", "PAGADA"
    private String observaciones;

    public FacturaMercancia() {
    }

    public FacturaMercancia(int id, String cedulaEmpleado, String numeroFactura, double montoTotal, double montoAbonado, 
                            String fechaEmision, String fechaVencimiento, boolean postergada, String estado, String observaciones) {
        this.id = id;
        this.cedulaEmpleado = cedulaEmpleado;
        this.numeroFactura = numeroFactura;
        this.montoTotal = montoTotal;
        this.montoAbonado = montoAbonado;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.postergada = postergada;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    // --- GETTERS & SETTERS ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCedulaEmpleado() {
        return cedulaEmpleado;
    }

    public void setCedulaEmpleado(String cedulaEmpleado) {
        this.cedulaEmpleado = cedulaEmpleado;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }

    public double getMontoAbonado() {
        return montoAbonado;
    }

    public void setMontoAbonado(double montoAbonado) {
        this.montoAbonado = montoAbonado;
    }

    public double getSaldo() {
        return Math.max(0.0, montoTotal - montoAbonado);
    }

    public String getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public boolean isPostergada() {
        return postergada;
    }

    public void setPostergada(boolean postergada) {
        this.postergada = postergada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
