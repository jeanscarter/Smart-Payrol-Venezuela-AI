package com.nomina.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nomina.config.ConfigManager;
import com.nomina.model.ReciboNomina;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Servicio encargado de generar los recibos de pago en formato PDF utilizando OpenPDF.
 * Produce archivos PDF profesionales, estructurados y adaptados al estándar multi-divisa.
 */
public final class PdfService {

    private PdfService() {
    }

    /**
     * Genera los recibos de nómina en la carpeta destino seleccionada.
     *
     * @param recibos Lista de recibos del periodo
     * @param destino Carpeta donde se guardarán los archivos
     * @throws IOException Si ocurre un error de escritura o generación
     */
    public static void generarRecibos(List<ReciboNomina> recibos, File destino) throws IOException {
        if (!destino.exists()) {
            destino.mkdirs();
        }

        String rifPatrono = ConfigManager.getRifPatrono();

        for (ReciboNomina r : recibos) {
            String fileName = String.format("Recibo_%s_%s.pdf", r.getCedula(), r.getPeriodoId());
            File file = new File(destino, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                generarPdfIndividual(r, rifPatrono, fos);
            } catch (DocumentException e) {
                throw new IOException("Error al generar el PDF para " + r.getNombreCompleto() + ": " + e.getMessage(), e);
            }
        }
    }

    private static void generarPdfIndividual(ReciboNomina r, String rifPatrono, FileOutputStream fos) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, fos);
        document.open();

        // Fuentes
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0x25, 0x63, 0xEB));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0x64, 0x74, 0x8B));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0x0F, 0x17, 0x2A));
        Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0x25, 0x63, 0xEB));
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(0x64, 0x74, 0x8B));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0x0F, 0x17, 0x2A));
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(0x64, 0x74, 0x8B));
        Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(0x0F, 0x17, 0x2A));
        Font tableBodyEarningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(0x10, 0xB9, 0x81));
        Font tableBodyDeductionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(0xEF, 0x44, 0x44));
        Font totalLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0x0F, 0x17, 0x2A));
        Font totalValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0x0F, 0x17, 0x2A));
        Font totalNetVesFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0x10, 0xB9, 0x81));
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(0x94, 0xA3, 0xB8));

        // 1. Encabezado de la empresa
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.addElement(new Paragraph("NÓMINA INTELIGENTE C.A.", titleFont));
        companyCell.addElement(new Paragraph("RIF: " + rifPatrono, subtitleFont));
        companyCell.addElement(new Paragraph("Caracas, Venezuela", subtitleFont));
        headerTable.addCell(companyCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph receiptTitle = new Paragraph("RECIBO DE PAGO", headerFont);
        receiptTitle.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(receiptTitle);

        // Traducir periodo a algo más legible
        String periodoText = r.getPeriodoId();
        if (periodoText.contains("-Q1")) {
            periodoText = "1ra Quincena - " + periodoText.substring(0, 7);
        } else if (periodoText.contains("-Q2")) {
            periodoText = "2da Quincena - " + periodoText.substring(0, 7);
        } else if (periodoText.contains("-M")) {
            periodoText = "Mensual Completo - " + periodoText.substring(0, 7);
        }
        Paragraph periodBadge = new Paragraph(periodoText, badgeFont);
        periodBadge.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(periodBadge);
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph(" ")); // Espaciador

        // Línea divisoria
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidth(2);
        lineCell.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        lineTable.addCell(lineCell);
        document.add(lineTable);
        document.add(new Paragraph(" ")); // Espaciador

        // 2. Cuadrícula de Información del Empleado
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{25, 25, 25, 25});
        infoTable.getDefaultCell().setBackgroundColor(new Color(0xF8, 0xFA, 0xFC));
        infoTable.getDefaultCell().setBorderColor(new Color(0xF1, 0xF5, 0xF9));
        infoTable.getDefaultCell().setPadding(8);

        // Fila 1
        infoTable.addCell(createInfoCell("TRABAJADOR", r.getNombreCompleto(), labelFont, valueFont, 2));
        infoTable.addCell(createInfoCell("CÉDULA DE IDENTIDAD", r.getCedula(), labelFont, valueFont, 2));

        // Fila 2
        infoTable.addCell(createInfoCell("TASA DE CAMBIO BCV", String.format("Bs. %,.4f", r.getTasaBcv()), labelFont, valueFont, 2));
        String fechaGen = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        infoTable.addCell(createInfoCell("FECHA GENERACIÓN", fechaGen, labelFont, valueFont, 2));

        document.add(infoTable);
        document.add(new Paragraph(" ")); // Espaciador

        // 3. Tabla de Conceptos (Asignaciones y Deducciones)
        PdfPTable conceptTable = new PdfPTable(4);
        conceptTable.setWidthPercentage(100);
        conceptTable.setWidths(new float[]{45, 15, 20, 20});

        // Cabecera
        conceptTable.addCell(createHeaderCell("CONCEPTO", tableHeaderFont));
        conceptTable.addCell(createHeaderCell("TIPO", tableHeaderFont));
        conceptTable.addCell(createHeaderCell("MONTO USD", tableHeaderFont));
        conceptTable.addCell(createHeaderCell("MONTO VES", tableHeaderFont));

        // Sueldo Base del periodo
        addConceptRow(conceptTable, "Sueldo Base del Periodo", "Asignación", r.getSueldoBasePeriodoUsd(), r.getSueldoBasePeriodoVes(), tableBodyFont, tableBodyEarningFont);

        // Cesta ticket
        double CTUsd = r.getTasaBcv() > 0 ? r.getCestaTicketVes() / r.getTasaBcv() : 0.0;
        addConceptRow(conceptTable, "Cesta Ticket Socialista", "Asignación", CTUsd, r.getCestaTicketVes(), tableBodyFont, tableBodyEarningFont);

        // Horas Extras (si aplica)
        if (r.getHorasExtras() > 0) {
            double heUsd = r.getHorasExtras() * (r.getSalarioMensualUsd() / 30.0 / 8.0) * 1.50;
            double heVes = heUsd * r.getTasaBcv();
            addConceptRow(conceptTable, String.format("Horas Extras (%.1f hrs)", r.getHorasExtras()), "Asignación", heUsd, heVes, tableBodyFont, tableBodyEarningFont);
        }

        // Horas Nocturnas (si aplica)
        if (r.getHorasNocturnas() > 0) {
            double hnUsd = r.getHorasNocturnas() * (r.getSalarioMensualUsd() / 30.0 / 8.0) * 0.30;
            double hnVes = hnUsd * r.getTasaBcv();
            addConceptRow(conceptTable, String.format("Recargo Nocturno (%.1f hrs)", r.getHorasNocturnas()), "Asignación", hnUsd, hnVes, tableBodyFont, tableBodyEarningFont);
        }

        // Feriados (si aplica)
        if (r.getDiasFeriados() > 0) {
            double feriadosUsd = r.getDiasFeriados() * (r.getSalarioMensualUsd() / 30.0) * 1.50;
            double feriadosVes = feriadosUsd * r.getTasaBcv();
            addConceptRow(conceptTable, String.format("Días Feriados Trabajados (%.0f días)", r.getDiasFeriados()), "Asignación", feriadosUsd, feriadosVes, tableBodyFont, tableBodyEarningFont);
        }

        // Bonos Extras (si aplica)
        if (r.getBonosExtrasUsd() > 0) {
            addConceptRow(conceptTable, "Bonificaciones Especiales", "Asignación", r.getBonosExtrasUsd(), r.getBonosExtrasUsd() * r.getTasaBcv(), tableBodyFont, tableBodyEarningFont);
        }

        // Días No Trabajados (si aplica)
        if (r.getDiasNoTrabajados() > 0) {
            double noTrabajadosUsd = r.getDiasNoTrabajados() * (r.getSalarioMensualUsd() / 30.0);
            double noTrabajadosVes = noTrabajadosUsd * r.getTasaBcv();
            addConceptRow(conceptTable, String.format("Deducción Días No Trabajados (%.0f días)", r.getDiasNoTrabajados()), "Deducción", -noTrabajadosUsd, -noTrabajadosVes, tableBodyFont, tableBodyDeductionFont);
        }

        // Adelantos USD/VES (si aplica)
        if (r.getAdelantoUsd() > 0) {
            addConceptRow(conceptTable, "Deducción Adelanto de Quincena (USD)", "Deducción", -r.getAdelantoUsd(), -r.getAdelantoUsd() * r.getTasaBcv(), tableBodyFont, tableBodyDeductionFont);
        }
        if (r.getAdelantoVes() > 0) {
            double adelantoUsd = r.getTasaBcv() > 0 ? r.getAdelantoVes() / r.getTasaBcv() : 0.0;
            addConceptRow(conceptTable, "Deducción Adelanto de Quincena (VES)", "Deducción", -adelantoUsd, -r.getAdelantoVes(), tableBodyFont, tableBodyDeductionFont);
        }

        // Retenciones IVSS y FAOV
        double ivssUsd = r.getTasaBcv() > 0 ? r.getIvssVes() / r.getTasaBcv() : 0.0;
        double faovUsd = r.getTasaBcv() > 0 ? r.getFaovVes() / r.getTasaBcv() : 0.0;
        addConceptRow(conceptTable, "Retención Seguro Social (IVSS 4%)", "Deducción", -ivssUsd, -r.getIvssVes(), tableBodyFont, tableBodyDeductionFont);
        addConceptRow(conceptTable, "Retención Viv. y Hábitat (FAOV 1%)", "Deducción", -faovUsd, -r.getFaovVes(), tableBodyFont, tableBodyDeductionFont);

        document.add(conceptTable);
        document.add(new Paragraph(" ")); // Espaciador

        // 4. Panel de Totales
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setWidths(new float[]{60, 40});

        PdfPCell spacerCell = new PdfPCell();
        spacerCell.setBorder(Rectangle.NO_BORDER);
        totalsTable.addCell(spacerCell);

        PdfPCell totalsBox = new PdfPCell();
        totalsBox.setBackgroundColor(new Color(0xF8, 0xFA, 0xFC));
        totalsBox.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        totalsBox.setPadding(12);

        // Sumas de asignaciones y deducciones
        double totalAsignacionesVes = r.getSueldoBasePeriodoVes() + r.getCestaTicketVes();
        if (r.getHorasExtras() > 0) totalAsignacionesVes += r.getHorasExtras() * (r.getSalarioMensualUsd() / 30.0 / 8.0) * 1.50 * r.getTasaBcv();
        if (r.getHorasNocturnas() > 0) totalAsignacionesVes += r.getHorasNocturnas() * (r.getSalarioMensualUsd() / 30.0 / 8.0) * 0.30 * r.getTasaBcv();
        if (r.getDiasFeriados() > 0) totalAsignacionesVes += r.getDiasFeriados() * (r.getSalarioMensualUsd() / 30.0) * 1.50 * r.getTasaBcv();
        if (r.getBonosExtrasUsd() > 0) totalAsignacionesVes += r.getBonosExtrasUsd() * r.getTasaBcv();

        double totalDeduccionesVes = r.getIvssVes() + r.getFaovVes();
        if (r.getDiasNoTrabajados() > 0) totalDeduccionesVes += r.getDiasNoTrabajados() * (r.getSalarioMensualUsd() / 30.0) * r.getTasaBcv();
        if (r.getAdelantoUsd() > 0) totalDeduccionesVes += r.getAdelantoUsd() * r.getTasaBcv();
        if (r.getAdelantoVes() > 0) totalDeduccionesVes += r.getAdelantoVes();

        totalsBox.addElement(createTotalParagraph("Total Asignaciones:", String.format("Bs. %,.2f", totalAsignacionesVes), totalLabelFont, totalValueFont));
        totalsBox.addElement(createTotalParagraph("Total Deducciones:", String.format("Bs. %,.2f", totalDeduccionesVes), totalLabelFont, totalValueFont));
        totalsBox.addElement(createTotalParagraph("Neto a Recibir (USD):", String.format("$%,.2f", r.getNetoUsd()), totalLabelFont, totalValueFont));
        
        Paragraph netVesPara = new Paragraph();
        netVesPara.add(new Phrase("Neto a Recibir: ", totalLabelFont));
        netVesPara.add(new Phrase(String.format("Bs. %,.2f", r.getNetoVes()), totalNetVesFont));
        netVesPara.setAlignment(Element.ALIGN_RIGHT);
        totalsBox.addElement(netVesPara);

        totalsTable.addCell(totalsBox);
        document.add(totalsTable);
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // 5. Firmas
        PdfPTable signaturesTable = new PdfPTable(2);
        signaturesTable.setWidthPercentage(100);
        signaturesTable.setWidths(new float[]{50, 50});

        PdfPCell sig1 = createSignatureCell("Firma del Trabajador", valueFont);
        PdfPCell sig2 = createSignatureCell("Por la Empresa (Nómina Inteligente C.A.)", valueFont);

        signaturesTable.addCell(sig1);
        signaturesTable.addCell(sig2);

        document.add(signaturesTable);
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // 6. Nota Legal
        Paragraph footerNote = new Paragraph("Este recibo de pago constituye constancia de las cantidades devengadas y deducidas conforme a la LOTTT vigente en la República Bolivariana de Venezuela.", footerFont);
        footerNote.setAlignment(Element.ALIGN_CENTER);
        document.add(footerNote);

        document.close();
    }

    private static PdfPCell createInfoCell(String label, String value, Font labelFont, Font valueFont, int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(colspan);
        cell.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        cell.setBackgroundColor(new Color(0xF8, 0xFA, 0xFC));
        cell.setPadding(6);
        
        Paragraph lPara = new Paragraph(label, labelFont);
        Paragraph vPara = new Paragraph(value, valueFont);
        cell.addElement(lPara);
        cell.addElement(vPara);
        
        return cell;
    }

    private static PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(0xF8, 0xFA, 0xFC));
        cell.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private static void addConceptRow(PdfPTable table, String concept, String type, double amtUsd, double amtVes, Font bodyFont, Font typeFont) {
        table.addCell(createCell(concept, bodyFont, Element.ALIGN_LEFT));
        table.addCell(createCell(type, typeFont, Element.ALIGN_LEFT));
        table.addCell(createCell(String.format("$%,.2f", amtUsd), bodyFont, Element.ALIGN_RIGHT));
        table.addCell(createCell(String.format("Bs. %,.2f", amtVes), bodyFont, Element.ALIGN_RIGHT));
    }

    private static PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(new Color(0xF1, 0xF5, 0xF9));
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private static Paragraph createTotalParagraph(String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label + " ", labelFont));
        p.add(new Phrase(value, valueFont));
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static PdfPCell createSignatureCell(String title, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(30);

        Paragraph line = new Paragraph("________________________________________", font);
        line.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);

        Paragraph label = new Paragraph(title, font);
        label.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(label);

        return cell;
    }
}
