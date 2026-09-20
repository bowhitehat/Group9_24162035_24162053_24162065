package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.EvaluationStatus;
import com.group9.topicmanagement.domain.enums.TopicResultStatus;
import com.group9.topicmanagement.domain.evaluation.Evaluation;
import com.group9.topicmanagement.domain.evaluation.EvaluationScore;
import com.group9.topicmanagement.domain.evaluation.TopicResult;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.EvaluationRepository;
import com.group9.topicmanagement.repository.EvaluationScoreRepository;
import com.group9.topicmanagement.repository.TopicRepository;
import com.group9.topicmanagement.repository.TopicResultRepository;
import com.group9.topicmanagement.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TopicResultService {
    private final TopicResultRepository resultRepository;
    private final TopicRepository topicRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final UserRepository userRepository;

    public TopicResultService(TopicResultRepository resultRepository,
                              TopicRepository topicRepository,
                              EvaluationRepository evaluationRepository,
                              EvaluationScoreRepository scoreRepository,
                              UserRepository userRepository) {
        this.resultRepository = resultRepository;
        this.topicRepository = topicRepository;
        this.evaluationRepository = evaluationRepository;
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
    }

    public TopicResult calculateResult(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đề tài"));

        List<Evaluation> evaluations = evaluationRepository.findByTopicId(topicId);
        
        BigDecimal totalScore = BigDecimal.ZERO;
        int validEvaluationCount = 0;

        for (Evaluation eval : evaluations) {
            if (eval.getStatus() == EvaluationStatus.SUBMITTED || eval.getStatus() == EvaluationStatus.LOCKED) {
                List<EvaluationScore> scores = scoreRepository.findByEvaluationId(eval.getId());
                
                BigDecimal evalTotal = BigDecimal.ZERO;
                int mandatoryCount = 0;
                
                for (EvaluationScore score : scores) {
                    if (score.getCriterion().getIsMandatory()) {
                        evalTotal = evalTotal.add(score.getScore());
                        mandatoryCount++;
                    }
                }
                
                if (mandatoryCount > 0) {
                    BigDecimal evalAvg = evalTotal.divide(BigDecimal.valueOf(mandatoryCount), 2, RoundingMode.HALF_UP);
                    totalScore = totalScore.add(evalAvg);
                    validEvaluationCount++;
                }
            }
        }

        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseGet(() -> {
                    TopicResult r = new TopicResult();
                    r.setTopic(topic);
                    r.setStatus(TopicResultStatus.PENDING_CONFIRMATION);
                    return r;
                });

        if (result.getStatus() == TopicResultStatus.PUBLISHED) {
            throw new BusinessRuleException("Kết quả đã công bố không thể tính lại");
        }

        if (validEvaluationCount > 0) {
            BigDecimal finalScore = totalScore.divide(BigDecimal.valueOf(validEvaluationCount), 2, RoundingMode.HALF_UP);
            result.setFinalScore(finalScore);
        } else {
            result.setFinalScore(null);
        }

        return resultRepository.save(result);
    }

    @PreAuthorize("hasRole('LECTURER')")
    public void confirmResult(Long topicId, String confirmerUsername) {
        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseThrow(() -> new NotFoundException("Chưa có kết quả để xác nhận"));
        
        if (result.getFinalScore() == null) {
            throw new BusinessRuleException("Không thể xác nhận khi chưa có điểm");
        }
        
        User confirmer = userRepository.findByUsernameIgnoreCase(confirmerUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
                
        result.setStatus(TopicResultStatus.CONFIRMED);
        result.setConfirmer(confirmer);
        result.setConfirmedTime(LocalDateTime.now());
        resultRepository.save(result);
    }

    @PreAuthorize("hasRole('FACULTY_MANAGER')")
    public void publishResult(Long topicId, String publisherUsername) {
        TopicResult result = resultRepository.findByTopicId(topicId)
                .orElseThrow(() -> new NotFoundException("Chưa có kết quả để công bố"));
        
        if (result.getStatus() != TopicResultStatus.CONFIRMED) {
            throw new BusinessRuleException("Phải được Chủ tịch xác nhận trước khi công bố");
        }
        
        User publisher = userRepository.findByUsernameIgnoreCase(publisherUsername)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
                
        result.setStatus(TopicResultStatus.PUBLISHED);
        result.setPublisher(publisher);
        result.setPublishedTime(LocalDateTime.now());
        resultRepository.save(result);
    }
}
