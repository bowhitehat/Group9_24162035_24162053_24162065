package com.group9.topicmanagement;

import com.group9.topicmanagement.model.Department;
import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.Role;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.enums.PeriodType;
import com.group9.topicmanagement.model.enums.RoleName;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.DepartmentRepository;
import com.group9.topicmanagement.repository.EvaluationCriterionRepository;
import com.group9.topicmanagement.repository.RegistrationPeriodRepository;
import com.group9.topicmanagement.repository.RoleRepository;
import com.group9.topicmanagement.repository.UserRepository;
import com.group9.topicmanagement.service.RegistrationPeriodService;
import com.group9.topicmanagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @Autowired EvaluationCriterionRepository criteria;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        criteria.deleteAll(); periods.deleteAll(); users.deleteAll(); roles.deleteAll(); departments.deleteAll();
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

    @ParameterizedTest
    @CsvSource({"STUDENT,false", "LECTURER,false", "FACULTY_MANAGER,true", "ADMIN,true"})
    void coreAdminSidebarHeadingMatchesRole(String role, boolean visible) throws Exception {
        String html = mvc.perform(get("/dashboard")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin").roles(role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html.contains("CORE &amp; ADMIN")).isEqualTo(visible);
    }

    @Test void rejectsInvalidTimeOrder() {
        RegistrationPeriod period = validPeriod(PeriodType.MON_HOC);
        period.setLecturerEnd(period.getLecturerStart().minusHours(1));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("giảng viên");
    }

    @Test @WithMockUser(roles="ADMIN")
    void periodPagesRenderLiveValidationFields() throws Exception {
        String html = mvc.perform(get("/faculty/periods")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-period-form", "/js/period-form.js", "reportSubmissionDeadline-error", "aria-live=\"polite\"");
        RegistrationPeriod period = periodService.save(validPeriod(PeriodType.MON_HOC));
        mvc.perform(get("/faculty/periods/" + period.getId() + "/edit"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-period-form")));
        mvc.perform(get("/js/period-form.js")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles="ADMIN")
    void invalidReportDeadlineReturnsCreateFormWithoutSaving() throws Exception {
        long count = periods.count();
        String html = mvc.perform(post("/faculty/periods").with(csrf())
                        .param("name", "Đợt nhập sai").param("type", "TLCN")
                        .param("lecturerStart", "2026-08-28T15:09").param("lecturerEnd", "2026-10-02T15:09")
                        .param("studentStart", "2026-10-03T15:10").param("studentEnd", "2026-10-30T15:10")
                        .param("reportSubmissionDeadline", "2026-09-18T15:10").param("reviewDeadline", "2026-11-10T15:10"))
                .andExpect(status().isOk()).andExpect(view().name("faculty/periods"))
                .andExpect(model().attributeHasErrors("periodForm"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Hạn nộp báo cáo phải sau", "Đợt nhập sai", "2026-09-18T15:10");
        assertThat(periods.count()).isEqualTo(count);
    }

    @Test @WithMockUser(roles="ADMIN")
    void invalidEditKeepsFormValuesAndDoesNotChangeSavedSchedule() throws Exception {
        RegistrationPeriod period = periodService.save(validPeriod(PeriodType.MON_HOC));
        LocalDateTime originalEnd = periods.findById(period.getId()).orElseThrow().getStudentEnd();
        mvc.perform(post("/faculty/periods/" + period.getId() + "/edit").with(csrf())
                        .param("name", "Lịch sửa sai").param("type", "MON_HOC")
                        .param("lecturerStart", "2026-10-02T10:00").param("lecturerEnd", "2026-10-01T10:00")
                        .param("studentStart", "2026-10-03T10:00").param("studentEnd", "2026-10-30T10:00"))
                .andExpect(status().isOk()).andExpect(view().name("faculty/period-edit"))
                .andExpect(model().attributeHasErrors("periodForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lịch sửa sai")));
        assertThat(periods.findById(period.getId()).orElseThrow().getStudentEnd()).isEqualTo(originalEnd);
    }

    @Test @WithMockUser(roles="ADMIN")
    void missingRequiredPeriodFieldIsShownOnForm() throws Exception {
        mvc.perform(post("/faculty/periods").with(csrf()).param("name", "Đợt thiếu ngày").param("type", "TLCN"))
                .andExpect(status().isOk()).andExpect(view().name("faculty/periods"))
                .andExpect(model().attributeHasFieldErrors("periodForm", "lecturerStart", "lecturerEnd", "studentStart", "studentEnd"));
        assertThat(periods.count()).isZero();
    }

    @Test void thesisTypesRequireReviewDeadline() {
        RegistrationPeriod period = validPeriod(PeriodType.TLCN); period.setReviewDeadline(null);
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("TLCN/KLTN");
    }

    @Test void rejectsPastLecturerStartEvenWhenScheduleOrderIsValid() {
        RegistrationPeriod period = validPeriod(PeriodType.MON_HOC);
        period.setLecturerStart(LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giảng viên đăng ký từ").hasMessageContaining("quá khứ");
        assertThat(periods.count()).isZero();
    }

    @Test void rejectsPastOptionalReviewDeadline() {
        RegistrationPeriod period = validPeriod(PeriodType.MON_HOC);
        period.setReviewDeadline(LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Hạn phản biện").hasMessageContaining("quá khứ");
    }

    @Test void todayIsAcceptedRegardlessOfSelectedHour() {
        RegistrationPeriod period = validPeriod(PeriodType.MON_HOC);
        period.setLecturerStart(LocalDate.now().atStartOfDay());
        period.setReviewDeadline(LocalDate.now().atStartOfDay());
        assertThat(periodService.save(period).getId()).isNotNull();
    }

    @Test void rejectsPastCouncilDateBeforeReviewDeadline() {
        RegistrationPeriod period = validPeriod(PeriodType.KLTN);
        period.setCouncilDate(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ngày hội đồng").hasMessageContaining("phải sau ngày hạn phản biện");
    }

    @ParameterizedTest
    @CsvSource({"TLCN,-1", "TLCN,0", "KLTN,-1", "KLTN,0"})
    void reviewMustBeOnALaterDayThanReportEvenIfTheHourIsLater(PeriodType type, int days) {
        RegistrationPeriod period = validPeriod(type);
        LocalDate reportDay = period.getStudentEnd().toLocalDate().plusDays(2);
        period.setReportSubmissionDeadline(reportDay.atStartOfDay());
        period.setReviewDeadline(reportDay.plusDays(days).atTime(23, 59));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Hạn phản biện phải sau ngày hạn nộp báo cáo");
        assertThat(periods.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"-1", "0"})
    void councilMustBeOnALaterDayThanReview(int days) {
        RegistrationPeriod period = validPeriod(PeriodType.KLTN);
        period.setCouncilDate(period.getReviewDeadline().toLocalDate().plusDays(days));
        assertThatThrownBy(() -> periodService.save(period)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ngày hội đồng phải sau ngày hạn phản biện");
        assertThat(periods.count()).isZero();
    }

    @Test void completeKltnScheduleAcceptsNextDaysRegardlessOfHours() {
        RegistrationPeriod period = validPeriod(PeriodType.KLTN);
        LocalDate reportDay = period.getStudentEnd().toLocalDate().plusDays(2);
        period.setReportSubmissionDeadline(reportDay.atTime(23, 59));
        period.setReviewDeadline(reportDay.plusDays(1).atStartOfDay());
        period.setCouncilDate(reportDay.plusDays(2));
        assertThat(periodService.save(period).getId()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"REVIEW", "COUNCIL"})
    void invalidMilestoneEditDoesNotModifySavedPeriod(String milestone) {
        RegistrationPeriod saved = validPeriod(PeriodType.KLTN);
        saved.setReportSubmissionDeadline(saved.getStudentEnd().plusDays(2));
        saved = periodService.save(saved);
        Long id = saved.getId();
        saved = periods.findById(id).orElseThrow();
        LocalDateTime originalReport = saved.getReportSubmissionDeadline();
        LocalDateTime originalReview = saved.getReviewDeadline();
        LocalDate originalCouncil = saved.getCouncilDate();
        RegistrationPeriod input = com.group9.topicmanagement.dto.PeriodForm.from(saved).toEntity();
        if (milestone.equals("REVIEW")) input.setReviewDeadline(originalReport.minusDays(1));
        else input.setCouncilDate(originalReview.toLocalDate());
        assertThatThrownBy(() -> periodService.update(id, input)).isInstanceOf(BusinessRuleException.class);
        RegistrationPeriod unchanged = periods.findById(id).orElseThrow();
        assertThat(unchanged.getReportSubmissionDeadline()).isEqualTo(originalReport);
        assertThat(unchanged.getReviewDeadline()).isEqualTo(originalReview);
        assertThat(unchanged.getCouncilDate()).isEqualTo(originalCouncil);
    }

    @ParameterizedTest @WithMockUser(roles="ADMIN")
    @CsvSource({"REVIEW", "COUNCIL"})
    void invalidMilestonesReturnCreateFormWithoutSaving(String milestone) throws Exception {
        RegistrationPeriod input = validPeriod(PeriodType.KLTN);
        input.setReportSubmissionDeadline(input.getStudentEnd().plusDays(2));
        if (milestone.equals("REVIEW")) input.setReviewDeadline(input.getReportSubmissionDeadline().minusDays(1));
        else input.setCouncilDate(input.getReviewDeadline().toLocalDate());
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        mvc.perform(post("/faculty/periods").with(csrf()).param("name", "Lịch KLTN sai").param("type", "KLTN")
                        .param("lecturerStart", input.getLecturerStart().format(format))
                        .param("lecturerEnd", input.getLecturerEnd().format(format))
                        .param("studentStart", input.getStudentStart().format(format))
                        .param("studentEnd", input.getStudentEnd().format(format))
                        .param("reportSubmissionDeadline", input.getReportSubmissionDeadline().format(format))
                        .param("reviewDeadline", input.getReviewDeadline().format(format))
                        .param("councilDate", input.getCouncilDate().toString()))
                .andExpect(status().isOk()).andExpect(view().name("faculty/periods"))
                .andExpect(model().attributeHasErrors("periodForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(milestone.equals("REVIEW")
                        ? "Hạn phản biện phải sau ngày hạn nộp báo cáo" : "Ngày hội đồng phải sau ngày hạn phản biện")));
        assertThat(periods.count()).isZero();
    }

    @Test @WithMockUser(roles="ADMIN")
    void pastScheduleReturnsFormWithMessageAndDoesNotSave() throws Exception {
        java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime start = LocalDateTime.now().minusDays(20);
        mvc.perform(post("/faculty/periods").with(csrf()).param("name", "Đợt ngày quá khứ").param("type", "MON_HOC")
                        .param("lecturerStart", start.format(format)).param("lecturerEnd", start.plusDays(5).format(format))
                        .param("studentStart", start.plusDays(6).format(format)).param("studentEnd", start.plusDays(12).format(format)))
                .andExpect(status().isOk()).andExpect(view().name("faculty/periods"))
                .andExpect(model().attributeHasErrors("periodForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("quá khứ")));
        assertThat(periods.count()).isZero();
    }

    @Test @WithMockUser(roles="ADMIN")
    void unchangedHistoricalMinuteDatesRemainEditableButNewPastDatesAreRejected() throws Exception {
        RegistrationPeriod historical = validPeriod(PeriodType.MON_HOC);
        historical.setLecturerStart(LocalDateTime.now().minusDays(20));
        historical.setLecturerEnd(LocalDateTime.now().minusDays(10));
        historical = periods.save(historical); // Existing historical record, not a new schedule via the service.
        String html = mvc.perform(get("/faculty/periods/" + historical.getId() + "/edit"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-original-value=");
        RegistrationPeriod input = com.group9.topicmanagement.dto.PeriodForm.from(historical).toEntity();
        input.setName("Giữ lịch cũ");
        input.setLecturerStart(input.getLecturerStart().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
        input.setLecturerEnd(input.getLecturerEnd().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
        periodService.update(historical.getId(), input);
        assertThat(periods.findById(historical.getId()).orElseThrow().getName()).isEqualTo("Giữ lịch cũ");
        input.setLecturerStart(input.getLecturerStart().minusDays(1));
        Long id = historical.getId();
        assertThatThrownBy(() -> periodService.update(id, input)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quá khứ");
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

    @Test void newPeriodGetsFiveDefaultEvaluationCriteria() {
        RegistrationPeriod saved = periodService.save(validPeriod(PeriodType.MON_HOC));
        assertThat(criteria.findByRegistrationPeriodIdOrderByDisplayOrderAsc(saved.getId()))
                .extracting("name")
                .containsExactly("Nội dung", "Kỹ thuật", "Sản phẩm", "Báo cáo", "Trình bày/phản biện");
    }

    @Test @WithMockUser(username="admin", roles="ADMIN")
    void adminEditsAccountWithoutChangingPassword() throws Exception {
        var student = userService.create("editstudent", "Tên cũ", "edit@test.local", "ED001", null, Set.of(RoleName.STUDENT), "Password@123");
        String hash = student.getPasswordHash();
        mvc.perform(get("/admin/users/" + student.getId() + "/edit"))
                .andExpect(status().isOk()).andExpect(view().name("admin/user-edit"));
        mvc.perform(post("/admin/users/" + student.getId() + "/edit").with(csrf())
                .param("username", "editstudent").param("fullName", "Tên đã sửa")
                .param("email", "edit-new@test.local").param("studentCode", "ED002").param("roles", "STUDENT"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        var saved = userService.get(student.getId());
        assertThat(saved.getFullName()).isEqualTo("Tên đã sửa");
        assertThat(saved.getPasswordHash()).isEqualTo(hash);
        assertThat(saved.getStudentCode()).isEqualTo("ED002");
    }

    @Test @WithMockUser(username="admin", roles="ADMIN")
    void duplicateIdentityAndInvalidEditKeepFormAndData() throws Exception {
        var student = userService.create("other", "Sinh viên", "other@test.local", "ED001", null, Set.of(RoleName.STUDENT), "Password@123");
        mvc.perform(post("/admin/users/" + student.getId() + "/edit").with(csrf())
                .param("username", " ADMIN ").param("fullName", "Tên mới").param("email", "other@test.local").param("roles", "STUDENT"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userUpdateForm"));
        assertThat(userService.get(student.getId()).getUsername()).isEqualTo("other");
        mvc.perform(post("/admin/users/" + student.getId() + "/edit").with(csrf()).param("username", "other"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("userUpdateForm"));
    }

    @Test void studentCodeAndNormalizedEmailMustBeUnique() {
        var first = userService.create("s1", "SV1", "s1@test.local", " UNIQUE ", null, Set.of(RoleName.STUDENT), "Password@123");
        assertThatThrownBy(() -> userService.create("s2", "SV2", "s2@test.local", "unique", null, Set.of(RoleName.STUDENT), "Password@123"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("MSSV");
        assertThatThrownBy(() -> userService.create("s2", "SV2", " S1@TEST.LOCAL ", null, null, Set.of(RoleName.STUDENT), "Password@123"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Email");
        var second = userService.create("s2", "SV2", "s2@test.local", "OTHER", null, Set.of(RoleName.STUDENT), "Password@123");
        assertThatThrownBy(() -> userService.update(second.getId(), "s2", "SV2", "s2@test.local", first.getStudentCode(), null, Set.of(RoleName.STUDENT), "admin"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("MSSV");
    }

    @Test void adminCannotRemoveOwnAccessOrRenameSelf() {
        var admin = userService.getByUsername("admin");
        assertThatThrownBy(() -> userService.update(admin.getId(), "admin", "Admin", "admin@test.local", null, null, Set.of(RoleName.STUDENT), "admin"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("chính mình");
        assertThatThrownBy(() -> userService.update(admin.getId(), "renamed", "Admin", "admin@test.local", null, null, Set.of(RoleName.ADMIN), "admin"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test @WithMockUser(username="admin", roles="ADMIN")
    void departmentEditNormalizesCodeAndRejectsDuplicate() throws Exception {
        Department original = departments.findByCodeIgnoreCase("CNPM").orElseThrow();
        mvc.perform(get("/admin/departments/" + original.getId() + "/edit"))
                .andExpect(status().isOk()).andExpect(view().name("admin/department-edit"));
        mvc.perform(post("/admin/departments/" + original.getId() + "/edit").with(csrf())
                .param("code", " attt ").param("name", "An toàn thông tin").param("description", "Đã cập nhật"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(departments.findById(original.getId()).orElseThrow().getCode()).isEqualTo("ATTT");
        Department other = new Department(); other.setCode("HTTT"); other.setName("Hệ thống thông tin"); departments.save(other);
        mvc.perform(post("/admin/departments/" + original.getId() + "/edit").with(csrf()).param("code", "httt").param("name", "Tên mới"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("departmentForm"));
        assertThat(departments.findById(original.getId()).orElseThrow().getCode()).isEqualTo("ATTT");
    }

    @ParameterizedTest @CsvSource({"STUDENT", "LECTURER", "FACULTY_MANAGER"})
    void nonAdminCannotGetOrPostEditPages(String role) throws Exception {
        var actor = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles(role);
        mvc.perform(get("/admin/users/1/edit").with(actor)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/1/edit").with(actor).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/departments/1/edit").with(actor)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/departments/1/edit").with(actor).with(csrf())).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="admin", roles="ADMIN")
    void editPostRequiresCsrfAndMissingIdsReturn404() throws Exception {
        mvc.perform(post("/admin/users/1/edit")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users/999999/edit")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/departments/999999/edit")).andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username="admin", roles="STUDENT")
    void mobileNavbarIncludesReportSubmission() throws Exception {
        String html = mvc.perform(get("/dashboard")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("aria-controls=\"nav\"");
        assertThat(html.substring(0, html.indexOf("</nav>"))).contains("/reports/submit");
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
