package com.group9.topicmanagement.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DepartmentForm {
    @NotBlank(message = "Mã bộ môn là bắt buộc") @Size(max = 20, message = "Mã bộ môn tối đa 20 ký tự")
    private String code;
    @NotBlank(message = "Tên bộ môn là bắt buộc") @Size(max = 160, message = "Tên bộ môn tối đa 160 ký tự")
    private String name;
    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
}
