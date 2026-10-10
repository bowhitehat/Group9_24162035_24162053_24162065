package com.group9.topicmanagement.service;

import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.enums.PeriodType;
import com.group9.topicmanagement.model.evaluation.EvaluationCriterion;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.EvaluationCriterionRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistrationPeriodService {
    private final RegistrationPeriodRepository periods;
    private final EvaluationCriterionRepository criteria;
    private final Clock clock;

    public RegistrationPeriodService(RegistrationPeriodRepository periods,
                                     EvaluationCriterionRepository criteria,
                                     Clock clock) {
        this.periods = periods;
        this.criteria = criteria;
        this.clock = clock;
    }

    public RegistrationPeriod get(Long id) {
        return periods.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy đợt đăng ký"));
    }

    public List<RegistrationPeriod> search(String keyword, PeriodType type, PeriodStatus status) {
        String value = keyword == null ? "" : keyword.trim();
        if (type != null && status != null) return periods.findByNameContainingIgnoreCaseAndTypeAndStatusOrderByCreatedAtDesc(value, type, status);
        if (type != null) return periods.findByNameContainingIgnoreCaseAndTypeOrderByCreatedAtDesc(value, type);
        if (status != null) return periods.findByNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(value, status);
        return value.isBlank() ? periods.findAllByOrderByCreatedAtDesc() : periods.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(value);
    }

    public List<RegistrationPeriod> findAllPeriods() {
        return search("", null, null);
    }

    public long countPeriods() { return periods.count(); }

    public boolean canEdit(RegistrationPeriod period) {
        return period.getStatus() == PeriodStatus.DRAFT;
    }

    @Transactional
    public RegistrationPeriod save(RegistrationPeriod period) {
        validate(period);
        boolean creating = period.getId() == null;
        validateFutureDates(period, creating ? null : get(period.getId()));
        RegistrationPeriod saved = periods.save(period);
        if (creating) {
            seedDefaultCriteria(saved);
        }
        return saved;
    }

    @Transactional
    public RegistrationPeriod update(Long id, RegistrationPeriod input) {
        RegistrationPeriod current = get(id);
        if (current.getStatus() != PeriodStatus.DRAFT) throw new BusinessRuleException("Chỉ được sửa đợt ở trạng thái nháp");
        validate(input);
        validateFutureDates(input, current);
        current.setName(input.getName()); current.setType(input.getType());
        current.setLecturerStart(input.getLecturerStart()); current.setLecturerEnd(input.getLecturerEnd());
        current.setStudentStart(input.getStudentStart()); current.setStudentEnd(input.getStudentEnd());
        current.setReportSubmissionDeadline(input.getReportSubmissionDeadline());
        current.setReviewDeadline(input.getReviewDeadline()); current.setCouncilDate(input.getCouncilDate());
        return current;
    }

    @Transactional
    public void transition(Long id, PeriodStatus next) {
        RegistrationPeriod period = get(id);
        if (!period.getStatus().canTransitionTo(next)) {
            throw new BusinessRuleException("Không thể chuyển từ " + period.getStatus() + " sang " + next);
        }
        period.setStatus(next);
    }

    public void requireLecturerWindow(RegistrationPeriod period) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(period.getLecturerStart()) || now.isAfter(period.getLecturerEnd())) throw new BusinessRuleException("Ngoài thời gian giảng viên đăng ký đề tài");
    }

    public void requireStudentWindow(RegistrationPeriod period) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(period.getStudentStart()) || now.isAfter(period.getStudentEnd())) throw new BusinessRuleException("Ngoài thời gian sinh viên đăng ký đề tài");
    }

    public void validate(RegistrationPeriod period) {
        if (period.getLecturerStart() == null || period.getLecturerEnd() == null || !period.getLecturerStart().isBefore(period.getLecturerEnd()))
            throw new BusinessRuleException("Thời gian giảng viên bắt đầu phải trước thời gian kết thúc");
        if (period.getStudentStart() == null || period.getStudentEnd() == null || !period.getStudentStart().isBefore(period.getStudentEnd()))
            throw new BusinessRuleException("Thời gian sinh viên bắt đầu phải trước thời gian kết thúc");
        if (period.getLecturerEnd().isAfter(period.getStudentStart()))
            throw new BusinessRuleException("Giai đoạn giảng viên phải kết thúc trước giai đoạn sinh viên");
        if ((period.getType() == PeriodType.TLCN || period.getType() == PeriodType.KLTN) && period.getReportSubmissionDeadline() == null)
            throw new BusinessRuleException("TLCN/KLTN phải có thời hạn nộp báo cáo");
        if (period.getReportSubmissionDeadline() != null && period.getReportSubmissionDeadline().isBefore(period.getStudentEnd()))
            throw new BusinessRuleException("Hạn nộp báo cáo phải sau khi kết thúc đăng ký đề tài");
        if ((period.getType() == PeriodType.TLCN || period.getType() == PeriodType.KLTN) && period.getReviewDeadline() == null)
            throw new BusinessRuleException("TLCN/KLTN phải có hạn nộp điểm phản biện");
        if (period.getType() != PeriodType.KLTN && period.getCouncilDate() != null)
            throw new BusinessRuleException("Chỉ KLTN được thiết lập ngày báo cáo hội đồng");
        if (period.getType() == PeriodType.KLTN && period.getCouncilDate() == null)
            throw new BusinessRuleException("KLTN phải có ngày báo cáo hội đồng");
        // These milestones must fall on later calendar days, regardless of their hours.
        if (period.getReportSubmissionDeadline() != null && period.getReviewDeadline() != null
                && !period.getReviewDeadline().toLocalDate().isAfter(period.getReportSubmissionDeadline().toLocalDate()))
            throw new BusinessRuleException("Hạn phản biện phải sau ngày hạn nộp báo cáo");
        if (period.getType() == PeriodType.KLTN && period.getCouncilDate() != null && period.getReviewDeadline() != null
                && !period.getCouncilDate().isAfter(period.getReviewDeadline().toLocalDate()))
            throw new BusinessRuleException("Ngày hội đồng phải sau ngày hạn phản biện");
    }

    private void validateFutureDates(RegistrationPeriod input, RegistrationPeriod original) {
        LocalDate now = LocalDate.now(clock);
        requireNotPast("Giảng viên đăng ký từ", input.getLecturerStart(), original == null ? null : original.getLecturerStart(), now);
        requireNotPast("Giảng viên đăng ký đến", input.getLecturerEnd(), original == null ? null : original.getLecturerEnd(), now);
        requireNotPast("Sinh viên đăng ký từ", input.getStudentStart(), original == null ? null : original.getStudentStart(), now);
        requireNotPast("Sinh viên đăng ký đến", input.getStudentEnd(), original == null ? null : original.getStudentEnd(), now);
        requireNotPast("Hạn nộp báo cáo", input.getReportSubmissionDeadline(), original == null ? null : original.getReportSubmissionDeadline(), now);
        requireNotPast("Hạn phản biện", input.getReviewDeadline(), original == null ? null : original.getReviewDeadline(), now);
        LocalDate councilDate = input.getCouncilDate();
        if (councilDate != null && councilDate.isBefore(now)
                && (original == null || !councilDate.equals(original.getCouncilDate()))) {
            throw new BusinessRuleException("Ngày hội đồng không được chọn ngày trong quá khứ");
        }
    }

    private void requireNotPast(String label, LocalDateTime value, LocalDateTime original, LocalDate today) {
        if (value == null) return;
        LocalDate date = value.toLocalDate();
        // Past-date validation deliberately ignores hours and minutes.
        if (date.isBefore(today) && (original == null || !date.equals(original.toLocalDate()))) {
            throw new BusinessRuleException(label + " không được chọn ngày trong quá khứ");
        }
    }

    private void seedDefaultCriteria(RegistrationPeriod period) {
        if (!criteria.findByRegistrationPeriodIdOrderByDisplayOrderAsc(period.getId()).isEmpty()) {
            return;
        }

        String[][] defaults = {
                {"Nội dung", "Mức độ đầy đủ, chính xác và phù hợp của nội dung đề tài."},
                {"Kỹ thuật", "Giải pháp kỹ thuật, kiến trúc và chất lượng triển khai."},
                {"Sản phẩm", "Mức độ hoàn thiện và khả năng vận hành của sản phẩm."},
                {"Báo cáo", "Chất lượng tài liệu, cấu trúc và cách trình bày báo cáo."},
                {"Trình bày/phản biện", "Khả năng trình bày, trả lời câu hỏi và bảo vệ kết quả."}
        };

        for (int index = 0; index < defaults.length; index++) {
            EvaluationCriterion criterion = new EvaluationCriterion();
            criterion.setName(defaults[index][0]);
            criterion.setDescription(defaults[index][1]);
            criterion.setRegistrationPeriod(period);
            criterion.setDisplayOrder(index + 1);
            criterion.setIsMandatory(true);
            criterion.setIsActive(true);
            criteria.save(criterion);
        }
    }
}
