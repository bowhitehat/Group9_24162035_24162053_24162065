package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.enums.TopicResultStatus;
import com.group9.topicmanagement.model.evaluation.TopicResult;
import com.group9.topicmanagement.model.topic.Topic;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicResultExportServiceTest {

    private final TopicResultExportService exportService = new TopicResultExportService();

    @Test
    void testExportExcel() throws IOException {
        Topic topic = new Topic();
        topic.setCode("T01");
        topic.setTitle("Đề tài tiếng Việt");

        TopicResult result = new TopicResult();
        result.setTopic(topic);
        result.setFinalScore(new BigDecimal("8.5"));
        result.setStatus(TopicResultStatus.PUBLISHED);

        byte[] excelBytes = exportService.exportToExcel(List.of(result));

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(excelBytes))) {
            var row = workbook.getSheetAt(0).getRow(1);
            org.assertj.core.api.Assertions.assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Đề tài tiếng Việt");
            org.assertj.core.api.Assertions.assertThat(row.getCell(3).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.NUMERIC);
            org.assertj.core.api.Assertions.assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(8.5);
        }
    }

    @Test
    void testExportPdf() throws IOException {
        Topic topic = new Topic();
        topic.setCode("T01");
        topic.setTitle("Đề tài tiếng Việt");

        TopicResult result = new TopicResult();
        result.setTopic(topic);
        result.setFinalScore(new BigDecimal("8.5"));
        result.setStatus(TopicResultStatus.PUBLISHED);

        byte[] pdfBytes = exportService.exportToPdf(List.of(result));

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        var reader = new com.lowagie.text.pdf.PdfReader(pdfBytes);
        try {
            String text = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
            org.assertj.core.api.Assertions.assertThat(text).contains("Đề tài tiếng Việt", "Kết quả điểm", "8.5");
        } finally {
            reader.close();
        }
    }
}
