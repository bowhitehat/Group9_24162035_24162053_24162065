package com.group9.topicmanagement;

import com.group9.topicmanagement.exception.BusinessRuleException;
import com.group9.topicmanagement.model.RegistrationPeriod;
import com.group9.topicmanagement.model.User;
import com.group9.topicmanagement.model.enums.PeriodStatus;
import com.group9.topicmanagement.model.enums.PeriodType;
import com.group9.topicmanagement.model.enums.RoleName;
import com.group9.topicmanagement.model.studentgroup.GroupMember;
import com.group9.topicmanagement.model.studentgroup.StudentGroup;
import com.group9.topicmanagement.repository.*;
import com.group9.topicmanagement.service.StudentGroupService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group9_membership_concurrency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.config.import=", "app.mail.enabled=false", "app.demo-password="
})
@ActiveProfiles("test")
class GroupMembershipConcurrencyRegressionTest {
    @DynamicPropertySource
    static void optionalIsolatedMySql(DynamicPropertyRegistry properties) {
        String url = System.getenv("GROUP9_QA_MYSQL_URL");
        if (url == null || url.isBlank()) return;
        if (!url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?/group9_qa_[a-z0-9_]+(?:\\?.*)?")) {
            throw new IllegalArgumentException("MySQL regression tests require a dedicated local group9_qa_ database");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("GROUP9_QA_MYSQL_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("GROUP9_QA_MYSQL_PASSWORD"));
    }

    @Autowired PlatformTransactionManager transactions;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired RegistrationPeriodRepository periods;
    @Autowired StudentGroupRepository groups;
    @Autowired StudentGroupService service;

    private Fixture fixture;
    record Fixture(Long periodId, Long firstGroup, Long secondGroup, String firstLeader,
                   String secondLeader, Long sharedStudentId, String sharedStudent) {}

    @BeforeEach
    void seedScopedFixture() {
        fixture = new TransactionTemplate(transactions).execute(tx -> {
            String prefix = "member" + UUID.randomUUID().toString().substring(0, 8);
            var role = roles.findByName(RoleName.STUDENT).orElseThrow();
            var department = departments.findByCodeIgnoreCase("CNPM").orElseThrow();
            var actors = java.util.stream.IntStream.range(0, 3).mapToObj(i -> {
                var user = new User(); user.setUsername(prefix + "_" + i); user.setFullName("Membership QA " + i);
                user.setEmail(prefix + i + "@example.test"); user.setPasswordHash("unused-test-hash");
                user.setDepartment(department); user.getRoles().add(role); return users.save(user);
            }).toList();
            var period = new RegistrationPeriod(); period.setName(prefix); period.setType(PeriodType.MON_HOC);
            period.setStatus(PeriodStatus.STUDENT_REGISTRATION);
            period.setLecturerStart(LocalDateTime.now().minusDays(10));
            period.setLecturerEnd(LocalDateTime.now().minusDays(5));
            period.setStudentStart(LocalDateTime.now().minusDays(1));
            period.setStudentEnd(LocalDateTime.now().plusDays(1));
            period = periods.save(period);
            Long first = seedGroup(period, actors.get(0)).getId();
            Long second = seedGroup(period, actors.get(1)).getId();
            return new Fixture(period.getId(), first, second, actors.get(0).getUsername(),
                    actors.get(1).getUsername(), actors.get(2).getId(), actors.get(2).getUsername());
        });
    }

    @Test
    void concurrentAddsToDifferentGroupsCannotEnrollTheSameStudentTwice() throws Exception {
        raceAgainstUncommittedMembership(() -> service.addMember(
                fixture.secondGroup(), fixture.sharedStudent(), fixture.secondLeader()));
        assertOnlyFirstGroupContainsStudent();
    }

    @Test
    void creatingGroupAfterWaitingForUserLockSeesTheNewMembership() throws Exception {
        raceAgainstUncommittedMembership(() -> service.createGroup(fixture.periodId(), fixture.sharedStudent()));
        assertOnlyFirstGroupContainsStudent();
        assertThat(groups.findByRegistrationPeriodIdAndLeaderId(fixture.periodId(), fixture.sharedStudentId())).isEmpty();
    }

    private StudentGroup seedGroup(RegistrationPeriod period, User leader) {
        var group = new StudentGroup(); group.setRegistrationPeriod(period); group.setLeader(leader);
        var membership = new GroupMember(); membership.setGroup(group); membership.setMember(leader);
        membership.setLeader(true); group.getMembers().add(membership); return groups.save(group);
    }

    private void raceAgainstUncommittedMembership(Runnable secondAction) throws Exception {
        var firstChecked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondAtUserLock = new CountDownLatch(1);
        var groupProxy = (Advised) groups;
        var userProxy = (Advised) users;
        MethodInterceptor pauseFirstSave = invocation -> {
            if (invocation.getMethod().getName().equals("save")
                    && Thread.currentThread().getName().equals("membership-first")) {
                firstChecked.countDown();
                if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new AssertionError("First membership edit timed out");
            }
            return invocation.proceed();
        };
        MethodInterceptor observeSecondLock = invocation -> {
            if (invocation.getMethod().getName().equals("findLockedById")
                    && Thread.currentThread().getName().equals("membership-second")) secondAtUserLock.countDown();
            return invocation.proceed();
        };
        groupProxy.addAdvice(0, pauseFirstSave); userProxy.addAdvice(0, observeSecondLock);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> {
                Thread.currentThread().setName("membership-first");
                return runMutation(() -> service.addMember(fixture.firstGroup(), fixture.sharedStudent(), fixture.firstLeader()));
            });
            assertThat(firstChecked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> second = pool.submit(() -> {
                Thread.currentThread().setName("membership-second"); return runMutation(secondAction);
            });
            assertThat(secondAtUserLock.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("OK");
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("REJECTED");
        } finally {
            releaseFirst.countDown(); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS);
            groupProxy.removeAdvice(pauseFirstSave); userProxy.removeAdvice(observeSecondLock);
        }
    }

    private String runMutation(Runnable action) {
        try { action.run(); return "OK"; }
        catch (BusinessRuleException expected) { return "REJECTED"; }
    }

    private void assertOnlyFirstGroupContainsStudent() {
        var memberships = groups.findByMemberId(fixture.sharedStudentId());
        assertThat(memberships).extracting(StudentGroup::getId).containsExactly(fixture.firstGroup());
        assertThat(groups.findById(fixture.secondGroup()).orElseThrow().getMembers())
                .noneMatch(member -> member.getMember().getId().equals(fixture.sharedStudentId()));
    }
}
