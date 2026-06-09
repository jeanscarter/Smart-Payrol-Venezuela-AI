package com.nomina.service;

import com.nomina.model.Empleado;
import com.nomina.model.FacturaMercancia;
import com.nomina.repository.EmpleadoRepository;
import com.nomina.repository.MercanciaRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de importar y parsear facturas de mercancía desde el PDF de Profit.
 */
public final class PdfImporter {

    public static class ImportResult {
        private final int successCount;
        private final int updatedCount;
        private final List<String> warnings;

        public ImportResult(int successCount, int updatedCount, List<String> warnings) {
            this.successCount = successCount;
            this.updatedCount = updatedCount;
            this.warnings = warnings;
        }

        public int getSuccessCount() { return successCount; }
        public int getUpdatedCount() { return updatedCount; }
        public List<String> getWarnings() { return warnings; }
    }

    private PdfImporter() {
    }

    public static ImportResult importFromPdf(File pdfFile) throws IOException {
        String text;
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            text = pdfStripper.getText(document);
        }

        int successCount = 0;
        int updatedCount = 0;
        List<String> warnings = new ArrayList<>();

        String[] lines = text.split("\\r?\\n");
        String currentCedula = null;
        String currentName = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Detectar cabecera de cliente: e.g. "11295201 NELSON RAFAEL DATICA URDANETA"
            if (line.matches("^\\d+\\s+[^/:;@]+$")) {
                int firstSpace = line.indexOf(' ');
                if (firstSpace > 0) {
                    currentCedula = line.substring(0, firstSpace).trim();
                    currentName = line.substring(firstSpace).trim();
                }
                continue;
            }

            // Detectar línea de factura: e.g. "FACT 03/06/2026 18/06/2026 ... 19.477,89 19.477,890000035070"
            if (line.startsWith("FACT ")) {
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 6) {
                    try {
                        String emisionPdf = tokens[1]; // DD/MM/YYYY
                        String vencimientoPdf = tokens[2]; // DD/MM/YYYY

                        // El total es el penúltimo token
                        String netTotalStr = tokens[tokens.length - 2];

                        // El último token contiene el saldo + 10 dígitos del número de factura
                        String lastToken = tokens[tokens.length - 1];
                        if (lastToken.length() > 10) {
                            String invoiceNum = lastToken.substring(lastToken.length() - 10);
                            String balanceStr = lastToken.substring(0, lastToken.length() - 10);

                            double netTotal = Double.parseDouble(netTotalStr.replace(".", "").replace(",", "."));
                            double balance = Double.parseDouble(balanceStr.replace(".", "").replace(",", "."));
                            double montoAbonado = Math.max(0.0, netTotal - balance);

                            String fechaEmision = convertPdfDateToDbDate(emisionPdf);
                            String fechaVencimiento = convertPdfDateToDbDate(vencimientoPdf);

                            // Buscar empleado en BD
                            Empleado employee = findMatchedEmployee(currentCedula);
                            if (employee != null) {
                                String dbCedula = employee.getCedula();
                                
                                // Verificar si la factura ya existe en el repositorio
                                FacturaMercancia existing = MercanciaRepository.getAll().stream()
                                        .filter(f -> f.getNumeroFactura().equals(invoiceNum))
                                        .findFirst().orElse(null);

                                String calculatedEstado = (montoAbonado <= 0) ? "PENDIENTE" : (montoAbonado >= netTotal ? "PAGADA" : "ABONANDO");

                                if (existing != null) {
                                    // Actualizar factura existente
                                    existing.setMontoTotal(netTotal);
                                    existing.setMontoAbonado(montoAbonado);
                                    existing.setFechaEmision(fechaEmision);
                                    existing.setFechaVencimiento(fechaVencimiento);
                                    existing.setEstado(calculatedEstado);
                                    
                                    MercanciaRepository.update(existing);
                                    updatedCount++;
                                } else {
                                    // Crear nueva factura
                                    FacturaMercancia newInvoice = new FacturaMercancia(
                                            0,
                                            dbCedula,
                                            invoiceNum,
                                            netTotal,
                                            montoAbonado,
                                            fechaEmision,
                                            fechaVencimiento,
                                            false, // postergada
                                            calculatedEstado,
                                            "Importado de Profit PDF"
                                    );
                                    MercanciaRepository.add(newInvoice);
                                    successCount++;
                                }
                            } else {
                                String warningMsg = currentCedula + " (" + currentName + ") - No registrado en Empleados";
                                if (!warnings.contains(warningMsg)) {
                                    warnings.add(warningMsg);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parseando linea de factura: " + line + " - " + e.getMessage());
                    }
                }
            }
        }
        return new ImportResult(successCount, updatedCount, warnings);
    }

    private static Empleado findMatchedEmployee(String pdfCedula) {
        if (pdfCedula == null) return null;
        String cleanPdf = pdfCedula.replaceAll("\\D", ""); // conservar sólo dígitos
        if (cleanPdf.isEmpty()) return null;

        for (Empleado emp : EmpleadoRepository.getAll()) {
            String cleanDb = emp.getCedula().replaceAll("\\D", "");
            if (cleanPdf.equals(cleanDb)) {
                return emp;
            }
        }
        return null;
    }

    private static String convertPdfDateToDbDate(String pdfDate) {
        try {
            String[] parts = pdfDate.split("/");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        } catch (Exception e) {
            // ignorar
        }
        return pdfDate;
    }
}
