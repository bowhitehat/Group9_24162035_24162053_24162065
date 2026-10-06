package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.evaluation.TopicResult;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class TopicResultExportService {

    public byte[] exportToExcel(List<TopicResult> results) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Kết quả điểm");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Mã đề tài");
            headerRow.createCell(1).setCellValue("Tên đề tài");
            headerRow.createCell(2).setCellValue("Hội đồng");
            headerRow.createCell(3).setCellValue("Điểm");
            headerRow.createCell(4).setCellValue("Trạng thái");

            int rowIdx = 1;
            for (TopicResult result : results) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(result.getTopic().getCode());
                row.createCell(1).setCellValue(result.getTopic().getTitle());
                row.createCell(2).setCellValue(result.getCouncilAssignment() != null ? result.getCouncilAssignment().getCouncil().getName() : "");
                row.createCell(3).setCellValue(result.getFinalScore() != null ? result.getFinalScore().toString() : "");
                row.createCell(4).setCellValue(result.getStatus() != null ? result.getStatus().name() : "");
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportToPdf(List<TopicResult> results) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font font = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("Ket qua diem", boldFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3f, 2.5f, 1f, 2f});

            table.addCell(new PdfPCell(new Phrase("Ma de tai", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Ten de tai", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Hoi dong", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Diem", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Trang thai", boldFont)));

            for (TopicResult result : results) {
                table.addCell(new Phrase(result.getTopic().getCode(), font));
                // Remove accents for openpdf built-in fonts (since it doesn't support vietnamese out of the box without providing a TTF)
                table.addCell(new Phrase(removeAccents(result.getTopic().getTitle()), font));
                table.addCell(new Phrase(result.getCouncilAssignment() != null ? removeAccents(result.getCouncilAssignment().getCouncil().getName()) : "", font));
                table.addCell(new Phrase(result.getFinalScore() != null ? result.getFinalScore().toString() : "", font));
                table.addCell(new Phrase(result.getStatus() != null ? result.getStatus().name() : "", font));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Lỗi khi tạo PDF", e);
        }
    }
    
    private String removeAccents(String text) {
        if (text == null) return "";
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("Đ", "D")
                .replaceAll("đ", "d");
    }
}
