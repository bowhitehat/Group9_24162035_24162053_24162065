package com.group9.topicmanagement;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreAdminRequirementsTest {
    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired RegistrationPeriodService periodService;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        periods.deleteAll(); users.deleteAll(); roles.deleteAll(); departments.deleteAll();
        for (RoleName name : RoleName.values()) { Role role = new Role(); role.setName(name); roles.save(role); }
        Department department = new Department(); department.setCode("CNPM"); department.setName("Công nghệ phần mềm"); departments.save(department);
        userService.create("admin", "Quản trị", "admin@test.local", null, department.getId(), Set.of(RoleName.ADMIN), "Password@123");
    }

    @Test void loginCorrectAndIncorrect() throws Exception {
        mvc.perform(formLogin().user("admin").password("Password@123")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(formLogin().user("admin").password("wrong-password")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));
    }

    @Test @WithMockUser(username="student1", roles="STUDENT")
    void unauthorizedUserGets403() throws Exception { mvc.perform(get("/admin/users")).andExpect(status().isForbidden()); }

    @Test void passwordIsBcryptEncoded() {
        String hash = users.findByUsernameIgnoreCase("admin").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2"); assertThat(hash).doesNotContain("Password@123"); assertThat(encoder.matches("Password@123", hash)).isTrue();
    }

    @Test void rejectsInvalidTimeOrder() {
        RegistrationPeriod period = validPeriod(PeriodType.MON_HOC);
        period.setLecturerEnd(period.getLecturerStart().minusHours(1));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("giảng viên");
    }

    @Test void thesisTypesRequireReviewDeadline() {
        RegistrationPeriod period = validPeriod(PeriodType.TLCN); period.setReviewDeadline(null);
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("TLCN/KLTN");
    }

    @Test void nonKltnCannotHaveCouncilDate() {
        RegistrationPeriod period = validPeriod(PeriodType.NCKH); period.setCouncilDate(LocalDate.now().plusMonths(4));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Chỉ KLTN");
    }

    @Test void rejectsInvalidStatusTransition() {
        RegistrationPeriod saved = periodService.save(validPeriod(PeriodType.MON_HOC));
        assertThat(saved.getStatus()).isEqualTo(PeriodStatus.DRAFT);
        assertThatThrownBy(() -> periodService.transition(saved.getId(), PeriodStatus.GRADING)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Không thể chuyển");
    }

    private RegistrationPeriod validPeriod(PeriodType type) {
        RegistrationPeriod period = new RegistrationPeriod(); period.setName("Đợt kiểm thử"); period.setType(type);
        LocalDateTime start = LocalDateTime.now().plusDays(1); period.setLecturerStart(start); period.setLecturerEnd(start.plusDays(5));
        period.setStudentStart(start.plusDays(6)); period.setStudentEnd(start.plusDays(12));
        if (type == PeriodType.TLCN || type == PeriodType.KLTN) period.setReviewDeadline(start.plusDays(20));
        if (type == PeriodType.KLTN) period.setCouncilDate(start.plusDays(25).toLocalDate());
        return period;
    }
}
