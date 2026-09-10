package com.group9.topicmanagement.service;

import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.exception.NotFoundException;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistrationPeriodService {
    private final RegistrationPeriodRepository periods;
    private final Clock clock;

    public RegistrationPeriodService(RegistrationPeriodRepository periods, Clock clock) {
        this.periods = periods;
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

    @Transactional
    public RegistrationPeriod save(RegistrationPeriod period) {
        validate(period);
        return periods.save(period);
    }

    @Transactional
    public RegistrationPeriod update(Long id, RegistrationPeriod input) {
        RegistrationPeriod current = get(id);
        if (current.getStatus() != PeriodStatus.DRAFT) throw new BusinessRuleException("Chỉ được sửa đợt ở trạng thái nháp");
        current.setName(input.getName()); current.setType(input.getType());
        current.setLecturerStart(input.getLecturerStart()); current.setLecturerEnd(input.getLecturerEnd());
        current.setStudentStart(input.getStudentStart()); current.setStudentEnd(input.getStudentEnd());
        current.setReviewDeadline(input.getReviewDeadline()); current.setCouncilDate(input.getCouncilDate());
        validate(current);
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
        if ((period.getType() == PeriodType.TLCN || period.getType() == PeriodType.KLTN) && period.getReviewDeadline() == null)
            throw new BusinessRuleException("TLCN/KLTN phải có hạn nộp điểm phản biện");
        if (period.getType() != PeriodType.KLTN && period.getCouncilDate() != null)
            throw new BusinessRuleException("Chỉ KLTN được thiết lập ngày báo cáo hội đồng");
        if (period.getType() == PeriodType.KLTN && period.getCouncilDate() == null)
            throw new BusinessRuleException("KLTN phải có ngày báo cáo hội đồng");
    }
}
