package com.hanaieum.server.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupCreateRequest {
    @NotBlank(message = "그룹명은 필수입니다.")
    @Size(max = 20, message = "그룹명을 20자 이내로 입력해주세요.")
    private String groupName;
}
