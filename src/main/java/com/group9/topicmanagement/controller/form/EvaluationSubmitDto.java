package com.group9.topicmanagement.controller.form;

import com.group9.topicmanagement.domain.enums.EvaluationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Map;

public class EvaluationSubmitDto {
    @NotNull(message = "Loại phiếu chấm không được để trống")
    private EvaluationType type;
    
    private String comments;
    
    private Map<Long, @DecimalMin(value = "0.0", message = "Điểm phải từ 0 đến 10")
            @DecimalMax(value = "10.0", message = "Điểm phải từ 0 đến 10") BigDecimal> scores;
    
    private boolean submitAction;

    public EvaluationType getType() {
        return type;
    }

    public void setType(EvaluationType type) {
        this.type = type;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Map<Long, BigDecimal> getScores() {
        return scores;
    }

    public void setScores(Map<Long, BigDecimal> scores) {
        this.scores = scores;
    }

    public boolean isSubmitAction() {
        return submitAction;
    }

    public void setSubmitAction(boolean submitAction) {
        this.submitAction = submitAction;
    }
}
