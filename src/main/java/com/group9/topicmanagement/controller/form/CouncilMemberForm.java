package com.group9.topicmanagement.controller.form;

import com.group9.topicmanagement.domain.enums.CouncilMemberRole;
import jakarta.validation.constraints.NotNull;

public class CouncilMemberForm {
    @NotNull(message = "Phải chọn giảng viên")
    private Long userId;

    @NotNull(message = "Phải chọn vai trò trong hội đồng")
    private CouncilMemberRole role;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public CouncilMemberRole getRole() { return role; }
    public void setRole(CouncilMemberRole role) { this.role = role; }
}
