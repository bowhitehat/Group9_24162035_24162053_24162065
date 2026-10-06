package com.group9.topicmanagement;

import com.group9.topicmanagement.model.*;
import com.group9.topicmanagement.model.announcement.*;
import com.group9.topicmanagement.model.council.*;
import com.group9.topicmanagement.model.evaluation.*;
import com.group9.topicmanagement.model.registration.*;
import com.group9.topicmanagement.model.studentgroup.*;
import com.group9.topicmanagement.model.topic.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ModelMappingContractTest {
    @Test void relationshipsHaveNamedForeignKeysAndStudentCodeIsUnique() {
        Set<String> names = new HashSet<>();
        for (Class<?> type : List.of(User.class, Announcement.class, Council.class, CouncilAssignment.class,
                CouncilMember.class, Evaluation.class, EvaluationCriterion.class, EvaluationScore.class,
                ReviewerAssignment.class, TopicResult.class, ReportSubmission.class, TopicRegistration.class,
                GroupMember.class, StudentGroup.class, Topic.class)) {
            for (var field : type.getDeclaredFields()) {
                JoinColumn column = field.getAnnotation(JoinColumn.class);
                if (column != null) checkForeignKey(column, names);
                JoinTable table = field.getAnnotation(JoinTable.class);
                if (table != null) {
                    for (JoinColumn join : table.joinColumns()) checkForeignKey(join, names);
                    for (JoinColumn join : table.inverseJoinColumns()) checkForeignKey(join, names);
                }
            }
        }
        assertThat(names).hasSizeGreaterThan(30);
        assertThat(Arrays.stream(User.class.getAnnotation(Table.class).uniqueConstraints())
                .anyMatch(c -> Arrays.asList(c.columnNames()).contains("student_code"))).isTrue();
    }
    private void checkForeignKey(JoinColumn column, Set<String> names) {
        assertThat(column.foreignKey().name()).startsWith("fk_");
        assertThat(names.add(column.foreignKey().name())).as("Unique FK name %s", column.foreignKey().name()).isTrue();
    }
}
