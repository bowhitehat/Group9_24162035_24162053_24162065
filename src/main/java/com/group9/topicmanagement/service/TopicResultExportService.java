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
import com.lowagie.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;
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
            var headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerRow.forEach(cell -> cell.setCellStyle(headerStyle));
            var scoreStyle = workbook.createCellStyle();
            scoreStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            sheet.createFreezePane(0, 1);

            int rowIdx = 1;
            for (TopicResult result : results) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(result.getTopic().getCode());
                row.createCell(1).setCellValue(result.getTopic().getTitle());
                row.createCell(2).setCellValue(result.getCouncilAssignment() != null ? result.getCouncilAssignment().getCouncil().getName() : "");
                var scoreCell = row.createCell(3);
                if (result.getFinalScore() != null) {
                    scoreCell.setCellValue(result.getFinalScore().doubleValue());
                    scoreCell.setCellStyle(scoreStyle);
                }
                row.createCell(4).setCellValue(result.getStatus() != null ? result.getStatus().name() : "");
            }
            for (int column = 0; column < 5; column++) sheet.autoSizeColumn(column);
            sheet.setColumnWidth(1, Math.min(sheet.getColumnWidth(1), 60 * 256));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportToPdf(List<TopicResult> results) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            byte[] fontBytes;
            try (var stream = new ClassPathResource("fonts/NotoSans-Regular.ttf").getInputStream()) {
                fontBytes = stream.readAllBytes();
            }
            BaseFont baseFont = BaseFont.createFont("NotoSans-Regular.ttf", BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, fontBytes, null);
            Font font = new Font(baseFont, 10, Font.NORMAL);
            Font boldFont = new Font(baseFont, 11, Font.BOLD);

            Paragraph title = new Paragraph("Kết quả điểm", boldFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3f, 2.5f, 1f, 2f});

            table.addCell(new PdfPCell(new Phrase("Mã đề tài", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Tên đề tài", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Hội đồng", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Điểm", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Trạng thái", boldFont)));
            table.setHeaderRows(1);

            for (TopicResult result : results) {
                table.addCell(new Phrase(result.getTopic().getCode(), font));
                table.addCell(new Phrase(result.getTopic().getTitle(), font));
                table.addCell(new Phrase(result.getCouncilAssignment() != null ? result.getCouncilAssignment().getCouncil().getName() : "", font));
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

}
