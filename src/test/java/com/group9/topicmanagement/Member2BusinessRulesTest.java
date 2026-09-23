package com.group9.topicmanagement;

import com.group9.topicmanagement.domain.Department;
import com.group9.topicmanagement.domain.RegistrationPeriod;
import com.group9.topicmanagement.domain.Role;
import com.group9.topicmanagement.domain.User;
import com.group9.topicmanagement.domain.enums.PeriodStatus;
import com.group9.topicmanagement.domain.enums.PeriodType;
import com.group9.topicmanagement.domain.enums.RegistrationStatus;
import com.group9.topicmanagement.domain.enums.RoleName;
import com.group9.topicmanagement.domain.registration.ReportSubmission;
import com.group9.topicmanagement.domain.registration.TopicRegistration;
import com.group9.topicmanagement.domain.studentgroup.StudentGroup;
import com.group9.topicmanagement.domain.topic.Topic;
import com.group9.topicmanagement.domain.topic.TopicStatus;
import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.*;
import com.group9.topicmanagement.controller.form.ReportSubmissionForm;
import com.group9.topicmanagement.controller.form.TopicForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Member2BusinessRulesTest {

    @Autowired MockMvc mvc;
    @Autowired UserService userService;
    @Autowired RegistrationPeriodService periodService;
    @Autowired TopicService topicService;
    @Autowired StudentGroupService groupService;
    @Autowired TopicRegistrationService registrationService;
    @Autowired ReportSubmissionService reportService;

    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired TopicRepository topics;
    @Autowired StudentGroupRepository groups;
    @Autowired TopicRegistrationRepository registrations;
    @Autowired ReportSubmissionRepository reports;
    @Autowired EvaluationCriterionRepository criteria;

    private User lecturer1;
    private User lecturer2;
    private User lecturer3;
    private User student1;
    private User student2;
    private User student3;
    private User student4;
    private User facultyManager;
    private Department dept;
    private RegistrationPeriod period;

    @BeforeEach
    void setUp() {
        reports.deleteAll();
        registrations.deleteAll();
        groups.deleteAll();
        topics.deleteAll();
        criteria.deleteAll();
        periods.deleteAll();
        users.deleteAll();
        roles.deleteAll();
        departments.deleteAll();

        for (RoleName name : RoleName.values()) {
            Role role = new Role();
            role.setName(name);
            roles.save(role);
        }

        dept = new Department();
        dept.setCode("CNPM");
        dept.setName("Công nghệ phần mềm");
        departments.save(dept);

        lecturer1 = userService.create("lecturer1", "GV Phân Văn A", "lec1@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        lecturer2 = userService.create("lecturer2", "GV Nguyễn Văn B", "lec2@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");
        lecturer3 = userService.create("lecturer3", "GV Trần Văn C", "lec3@test.local", null, dept.getId(), Set.of(RoleName.LECTURER), "Password@123");

        student1 = userService.create("student1", "SV Nguyễn Văn 1", "sv1@test.local", null, dept.getId(), Set.of(RoleName.STUDENT), "Password@123");
        student2 = userService.create("student2", "SV Trần Văn 2", "sv2@test.local", null, dept.getId(), Set.of(RoleName.STUDENT), "Password@123");
        student3 = userService.create("student3", "SV Lê Văn 3", "sv3@test.local", null, dept.getId(), Set.of(RoleName.STUDENT), "Password@123");
        student4 = userService.create("student4", "SV Phạm Văn 4", "sv4@test.local", null, dept.getId(), Set.of(RoleName.STUDENT), "Password@123");

        facultyManager = userService.create("faculty1", "Trưởng Khoa", "faculty@test.local", null, dept.getId(), Set.of(RoleName.FACULTY_MANAGER), "Password@123");

        period = new RegistrationPeriod();
        period.setName("Đợt HK1 2026-2027");
        period.setType(PeriodType.MON_HOC);
        LocalDateTime now = LocalDateTime.now();
        period.setLecturerStart(now.minusDays(5));
        period.setLecturerEnd(now.plusDays(5));
        period.setStudentStart(now.minusDays(2));
        period.setStudentEnd(now.plusDays(10));
        period.setReportSubmissionDeadline(now.plusDays(20));
        period.setStatus(PeriodStatus.STUDENT_REGISTRATION);
        period = periods.save(period);
    }

    @Test
    void testCreateTopicAndMaxAdvisorsRule() {
        TopicForm form = new TopicForm();
        form.setCode("DT01");
        form.setTitle("Xây dựng Hệ thống QLĐT");
        form.setDepartmentId(dept.getId());
        form.setRegistrationPeriodId(period.getId());
        form.setAdvisorIds(List.of(lecturer1.getId(), lecturer2.getId()));

        Topic topic = topicService.createTopic(form, lecturer1.getUsername());
        assertThat(topic.getId()).isNotNull();
        assertThat(topic.getStatus()).isEqualTo(TopicStatus.DRAFT);
        assertThat(topic.getAdvisors()).hasSize(2);

        // Kiểm tra quá 2 GVHD bị lỗi
        TopicForm invalidForm = new TopicForm();
        invalidForm.setCode("DT02");
        invalidForm.setTitle("Xây dựng Hệ thống QLĐT 2");
        invalidForm.setDepartmentId(dept.getId());
        invalidForm.setRegistrationPeriodId(period.getId());
        invalidForm.setAdvisorIds(List.of(lecturer1.getId(), lecturer2.getId(), lecturer3.getId()));

        assertThatThrownBy(() -> topicService.createTopic(invalidForm, lecturer1.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tối đa 2 giảng viên");
    }

    @Test
    void testTopicApprovalAndPublishFlow() {
        TopicForm form = new TopicForm();
        form.setCode("DT03");
        form.setTitle("Ứng dụng AI vào Giảng dạy");
        form.setDepartmentId(dept.getId());
        form.setRegistrationPeriodId(period.getId());
        Topic topic = topicService.createTopic(form, lecturer1.getUsername());

        topicService.submitTopic(topic.getId(), lecturer1.getUsername());
        Topic submitted = topicService.getTopicById(topic.getId());
        assertThat(submitted.getStatus()).isEqualTo(TopicStatus.PENDING);

        topicService.approveTopic(topic.getId());
        Topic approved = topicService.getTopicById(topic.getId());
        assertThat(approved.getStatus()).isEqualTo(TopicStatus.APPROVED);

        topicService.publishTopic(topic.getId());
        Topic published = topicService.getTopicById(topic.getId());
        assertThat(published.getStatus()).isEqualTo(TopicStatus.PUBLISHED);
    }

    @Test
    void testStudentGroupRulesMax3AndSingleLeaderAndNoDuplicateGroupInPeriod() {
        StudentGroup group = groupService.createGroup(period.getId(), student1.getUsername());
        assertThat(group.getLeader().getUsername()).isEqualTo("student1");
        assertThat(group.getMembers()).hasSize(1);
        assertThat(group.getMembers().iterator().next().isLeader()).isTrue();

        groupService.addMember(group.getId(), student2.getUsername(), student1.getUsername());
        groupService.addMember(group.getId(), student3.getUsername(), student1.getUsername());

        // Đã đủ 3 thành viên, thêm thành viên thứ 4 sẽ thất bại
        assertThatThrownBy(() -> groupService.addMember(group.getId(), student4.getUsername(), student1.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tối đa 3 sinh viên");

        // Một sinh viên không thuộc 2 nhóm trong cùng đợt
        assertThatThrownBy(() -> groupService.createGroup(period.getId(), student2.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã thuộc một nhóm khác");
    }

    @Test
    void testOnlyLeaderCanRegisterTopicAndPreventDuplicateApprovedTopic() {
        TopicForm form = new TopicForm();
        form.setCode("DT04");
        form.setTitle("IoT trong Nông nghiệp");
        form.setDepartmentId(dept.getId());
        form.setRegistrationPeriodId(period.getId());
        Topic topic = topicService.createTopic(form, lecturer1.getUsername());
        topicService.submitTopic(topic.getId(), lecturer1.getUsername());
        topicService.approveTopic(topic.getId());
        topicService.publishTopic(topic.getId());

        StudentGroup group1 = groupService.createGroup(period.getId(), student1.getUsername());
        groupService.addMember(group1.getId(), student2.getUsername(), student1.getUsername());

        // Thành viên thường đăng ký -> Thất bại
        assertThatThrownBy(() -> registrationService.registerTopic(group1.getId(), topic.getId(), student2.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chỉ trưởng nhóm");

        // Trưởng nhóm đăng ký -> Thành công
        TopicRegistration reg1 = registrationService.registerTopic(group1.getId(), topic.getId(), student1.getUsername());
        assertThat(reg1.getStatus()).isEqualTo(RegistrationStatus.PENDING);

        // Duyệt đăng ký cho nhóm 1
        registrationService.approveRegistration(reg1.getId(), facultyManager.getUsername());

        // Nhóm 2 cố gắng đăng ký đề tài đã được duyệt cho nhóm 1 -> Thất bại
        StudentGroup group2 = groupService.createGroup(period.getId(), student3.getUsername());
        assertThatThrownBy(() -> registrationService.registerTopic(group2.getId(), topic.getId(), student3.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã được đăng ký thành công");
    }

    @Test
    void testReportSubmissionOnlyLeaderAndVersionIncrement() {
        TopicForm form = new TopicForm();
        form.setCode("DT05");
        form.setTitle("Phát triển Mobile App");
        form.setDepartmentId(dept.getId());
        form.setRegistrationPeriodId(period.getId());
        Topic topic = topicService.createTopic(form, lecturer1.getUsername());
        topicService.submitTopic(topic.getId(), lecturer1.getUsername());
        topicService.approveTopic(topic.getId());
        topicService.publishTopic(topic.getId());

        StudentGroup group = groupService.createGroup(period.getId(), student1.getUsername());
        groupService.addMember(group.getId(), student2.getUsername(), student1.getUsername());

        TopicRegistration reg = registrationService.registerTopic(group.getId(), topic.getId(), student1.getUsername());
        registrationService.approveRegistration(reg.getId(), facultyManager.getUsername());

        MockMultipartFile file1 = new MockMultipartFile("file", "baocao_v1.pdf", "application/pdf", "Content v1".getBytes());
        ReportSubmissionForm form1 = new ReportSubmissionForm();
        form1.setTopicRegistrationId(reg.getId());
        form1.setFile(file1);
        form1.setNote("Báo cáo tuần 1");

        // Thành viên thường nộp -> Lỗi
        assertThatThrownBy(() -> reportService.submitReport(form1, student2.getUsername()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Chỉ trưởng nhóm");

        // Trưởng nhóm nộp lần 1 -> version = 1
        ReportSubmission sub1 = reportService.submitReport(form1, student1.getUsername());
        assertThat(sub1.getVersion()).isEqualTo(1);

        // Trưởng nhóm nộp lần 2 -> version = 2
        MockMultipartFile file2 = new MockMultipartFile("file", "baocao_v2.pdf", "application/pdf", "Content v2".getBytes());
        ReportSubmissionForm form2 = new ReportSubmissionForm();
        form2.setTopicRegistrationId(reg.getId());
        form2.setFile(file2);
        form2.setNote("Báo cáo bổ sung");

        ReportSubmission sub2 = reportService.submitReport(form2, student1.getUsername());
        assertThat(sub2.getVersion()).isEqualTo(2);

        List<ReportSubmission> history = reportService.getSubmissionHistoryForUser(reg.getId(), student1.getUsername());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getVersion()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void testBackendSecurity403ForUnauthorizedRole() throws Exception {
        // Sinh viên truy cập trang tạo đề tài của Giảng viên -> 403 Forbidden
        mvc.perform(get("/topics/create")).andExpect(status().isForbidden());

        // Sinh viên truy cập đường dẫn duyệt đăng ký của Khoa -> 403 Forbidden
        mvc.perform(post("/registrations/approve/1")).andExpect(status().isForbidden());

        // Sinh viên truy cập đường dẫn duyệt đề tài của Khoa -> 403 Forbidden
        mvc.perform(post("/topics/approve/1")).andExpect(status().isForbidden());

        // Sinh viên truy cập đường dẫn từ chối đề tài của Khoa -> 403 Forbidden
        mvc.perform(post("/topics/reject/1")).andExpect(status().isForbidden());

        // Sinh viên truy cập đường dẫn công bố đề tài của Khoa -> 403 Forbidden
        mvc.perform(post("/topics/publish/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lecturer1", roles = "LECTURER")
    void testLecturer403ForbiddenAccessToFacultyAndStudentEndpoints() throws Exception {
        // Giảng viên truy cập tạo nhóm sinh viên -> 403 Forbidden
        mvc.perform(post("/groups/create").param("periodId", "1")).andExpect(status().isForbidden());

        // Giảng viên truy cập nộp báo cáo -> 403 Forbidden
        mvc.perform(get("/reports/submit").param("registrationId", "1")).andExpect(status().isForbidden());

        // Giảng viên truy cập duyệt đăng ký đề tài -> 403 Forbidden
        mvc.perform(post("/registrations/approve/1")).andExpect(status().isForbidden());
    }

    @Test
    void testReportDownloadEndpoint() throws Exception {
        TopicRegistration registration = createApprovedRegistration();
        ReportSubmission sub = reportService.submitReport(
                reportForm(registration.getId(), "baocao_final.pdf", "application/pdf"), student1.getUsername());

        mvc.perform(get("/reports/download/" + sub.getId())
                        .with(user(student1.getUsername()).roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void testRejectReasonIsRequired() {
        TopicForm form = topicForm("DT06", "Đề tài cần phản hồi");
        Topic topic = topicService.createTopic(form, lecturer1.getUsername());
        topicService.submitTopic(topic.getId(), lecturer1.getUsername());

        assertThatThrownBy(() -> topicService.rejectTopic(topic.getId(), "  "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    void testInvalidReportFileIsRejectedAndOtherGroupCannotRead() {
        TopicRegistration registration = createApprovedRegistration();
        ReportSubmissionForm invalidForm = reportForm(registration.getId(), "malware.exe", "application/octet-stream");

        assertThatThrownBy(() -> reportService.submitReport(invalidForm, student1.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PDF, DOC, DOCX hoặc ZIP");

        ReportSubmission submission = reportService.submitReport(
                reportForm(registration.getId(), "bao-cao.pdf", "application/pdf"), student1.getUsername());
        assertThat(submission.getVersion()).isEqualTo(1);
        assertThatThrownBy(() -> reportService.getSubmissionHistoryForUser(registration.getId(), student4.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testReportDeadlineIsEnforced() {
        TopicRegistration registration = createApprovedRegistration();
        period.setReportSubmissionDeadline(LocalDateTime.now().minusMinutes(1));
        periods.save(period);

        assertThatThrownBy(() -> reportService.submitReport(
                reportForm(registration.getId(), "tre-han.pdf", "application/pdf"), student1.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn nộp báo cáo");
    }

    @Test
    void testTopicOutsideLecturerWindowIsRejected() {
        period.setLecturerStart(LocalDateTime.now().minusDays(3));
        period.setLecturerEnd(LocalDateTime.now().minusDays(2));
        periods.save(period);

        assertThatThrownBy(() -> topicService.createTopic(topicForm("DT07", "Đề tài trễ hạn"), lecturer1.getUsername()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ngoài thời gian giảng viên");
    }

    @Test
    void testReportHistoryReturns403ForStudentFromAnotherGroup() throws Exception {
        TopicRegistration registration = createApprovedRegistration();
        reportService.submitReport(reportForm(registration.getId(), "bao-cao.pdf", "application/pdf"), student1.getUsername());

        mvc.perform(get("/reports/history/" + registration.getId())
                        .with(user(student4.getUsername()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    private TopicRegistration createApprovedRegistration() {
        Topic topic = topicService.createTopic(topicForm("DT-HELPER", "Đề tài kiểm thử báo cáo"), lecturer1.getUsername());
        topicService.submitTopic(topic.getId(), lecturer1.getUsername());
        topicService.approveTopic(topic.getId());
        topicService.publishTopic(topic.getId());
        StudentGroup group = groupService.createGroup(period.getId(), student1.getUsername());
        TopicRegistration registration = registrationService.registerTopic(group.getId(), topic.getId(), student1.getUsername());
        registrationService.approveRegistration(registration.getId(), facultyManager.getUsername());
        return registration;
    }

    private TopicForm topicForm(String code, String title) {
        TopicForm form = new TopicForm();
        form.setCode(code);
        form.setTitle(title);
        form.setDepartmentId(dept.getId());
        form.setRegistrationPeriodId(period.getId());
        return form;
    }

    private ReportSubmissionForm reportForm(Long registrationId, String fileName, String contentType) {
        ReportSubmissionForm form = new ReportSubmissionForm();
        form.setTopicRegistrationId(registrationId);
        form.setFile(new MockMultipartFile("file", fileName, contentType, "test-content".getBytes()));
        return form;
    }
}
