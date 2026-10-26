package com.group9.topicmanagement.dto;

import com.group9.topicmanagement.model.council.Council;
import com.group9.topicmanagement.model.council.CouncilAssignment;
import com.group9.topicmanagement.model.council.CouncilMember;
import java.util.List;

/** Request-local view data; never shared between users or cached across requests. */
public record CouncilDetail(Council council, List<CouncilMember> members,
        List<CouncilAssignment> assignments, List<String> gradingRoleConflicts,
        boolean canEditCouncil, boolean canEditStructure, boolean canActivate,
        boolean canComplete, boolean canCancel, boolean canGrade) { }
