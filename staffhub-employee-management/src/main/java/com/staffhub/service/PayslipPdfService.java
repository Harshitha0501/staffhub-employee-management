package com.staffhub.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.staffhub.model.Payroll;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders a clean, printable A4 payslip PDF for a single {@link Payroll} using OpenPDF. */
@Service
public class PayslipPdfService {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);
    private static final Color INK = new Color(0x0F, 0x17, 0x2A);
    private static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color LINE = new Color(0xE2, 0xE8, 0xF0);
    private static final Color ACCENT = new Color(0x1E, 0x29, 0x3B);
    private static final Color PANEL = new Color(0xF8, 0xFA, 0xFC);

    public byte[] build(Payroll payroll) {
        Document document = new Document(PageSize.A4, 50, 50, 54, 46);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(brandHeader(payroll));
            document.add(spacer(14));
            document.add(infoTable(payroll));
            document.add(spacer(18));
            document.add(earningsTable(payroll));
            document.add(spacer(22));
            document.add(footer());

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to build payslip PDF", ex);
        }
    }

    private Paragraph brandHeader(Payroll payroll) {
        Paragraph brand = new Paragraph();
        brand.add(new Phrase("StaffHub\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, INK)));
        brand.add(new Phrase("Payslip · " + monthLabel(payroll.getPayMonth()),
                FontFactory.getFont(FontFactory.HELVETICA, 12, MUTED)));
        return brand;
    }

    private PdfPTable infoTable(Payroll payroll) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorder(0);
        var employee = payroll.getEmployee();
        addInfo(table, "Employee", employee.getFullName());
        addInfo(table, "Employee code", employee.getEmployeeCode() == null ? "—" : employee.getEmployeeCode());
        addInfo(table, "Department", employee.getDepartment().getName());
        addInfo(table, "Role", employee.getRole());
        addInfo(table, "Pay period", monthLabel(payroll.getPayMonth()));
        addInfo(table, "Status", payroll.getStatus().name());
        return table;
    }

    private void addInfo(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPaddingBottom(10);
        cell.addElement(new Paragraph(label.toUpperCase(Locale.US), labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private PdfPTable earningsTable(Payroll payroll) {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(100);

        table.addCell(headerCell("Description"));
        table.addCell(headerCellRight("Amount"));

        BigDecimal gross = payroll.getBaseSalary().add(payroll.getBonus());
        row(table, "Base salary", payroll.getBaseSalary(), false);
        row(table, "Bonus", payroll.getBonus(), false);
        row(table, "Gross pay", gross, true);
        row(table, "Deductions", payroll.getDeductions().negate(), false);
        netRow(table, "Net pay", payroll.getNetPay());
        return table;
    }

    private void row(PdfPTable table, String label, BigDecimal amount, boolean strong) {
        Font font = FontFactory.getFont(strong ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 11, INK);
        PdfPCell left = new PdfPCell(new Phrase(label, font));
        PdfPCell right = new PdfPCell(new Phrase(MONEY.format(amount), font));
        for (PdfPCell cell : new PdfPCell[]{left, right}) {
            cell.setBorderColor(LINE);
            cell.setBorderWidthLeft(0);
            cell.setBorderWidthRight(0);
            cell.setBorderWidthTop(0);
            cell.setPadding(9);
            if (strong) cell.setBackgroundColor(PANEL);
        }
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private void netRow(PdfPTable table, String label, BigDecimal amount) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE);
        PdfPCell left = new PdfPCell(new Phrase(label, font));
        PdfPCell right = new PdfPCell(new Phrase(MONEY.format(amount), font));
        for (PdfPCell cell : new PdfPCell[]{left, right}) {
            cell.setBackgroundColor(ACCENT);
            cell.setBorder(0);
            cell.setPadding(11);
        }
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(left);
        table.addCell(right);
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(Locale.US),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED)));
        cell.setBorder(0);
        cell.setBorderWidthBottom(1);
        cell.setBorderColor(LINE);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell headerCellRight(String text) {
        PdfPCell cell = headerCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private Paragraph footer() {
        Paragraph footer = new Paragraph(
                "This is a computer-generated payslip and does not require a signature. "
                        + "Net pay = base salary + bonus - deductions.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED));
        footer.setSpacingBefore(6);
        return footer;
    }

    private Paragraph spacer(float height) {
        Paragraph spacer = new Paragraph(" ");
        spacer.setLeading(height);
        return spacer;
    }

    private String monthLabel(String payMonth) {
        try {
            return YearMonth.parse(payMonth).format(MONTH_LABEL);
        } catch (Exception ex) {
            return payMonth;
        }
    }
}
