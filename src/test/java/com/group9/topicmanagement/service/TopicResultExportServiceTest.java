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
        topic.setTitle("Test Topic");
        
        TopicResult result = new TopicResult();
        result.setTopic(topic);
        result.setFinalScore(new BigDecimal("8.5"));
        result.setStatus(TopicResultStatus.PUBLISHED);

        byte[] excelBytes = exportService.exportToExcel(List.of(result));
        
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void testExportPdf() throws IOException {
        Topic topic = new Topic();
        topic.setCode("T01");
        topic.setTitle("Test Topic");
        
        TopicResult result = new TopicResult();
        result.setTopic(topic);
        result.setFinalScore(new BigDecimal("8.5"));
        result.setStatus(TopicResultStatus.PUBLISHED);

        byte[] pdfBytes = exportService.exportToPdf(List.of(result));
        
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
