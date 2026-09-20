package com.group9.topicmanagement.domain.evaluation;
import com.group9.topicmanagement.domain.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "evaluation_scores")
public class EvaluationScore extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal score;
    @Column(length = 500)
    private String note;
    
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evaluation getEvaluation() { return evaluation; } public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }
    public EvaluationCriterion getCriterion() { return criterion; } public void setCriterion(EvaluationCriterion criterion) { this.criterion = criterion; }
    public BigDecimal getScore() { return score; } public void setScore(BigDecimal score) { this.score = score; }
    public String getNote() { return note; } public void setNote(String note) { this.note = note; }
}