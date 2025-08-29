package com.hanaieum.server.domain.bucketList.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketListUpdateRequest {
    
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title; // 제목
    
    private Boolean publicFlag; // 공개여부 (true: 공개, false: 비공개)
    
    private Boolean shareFlag; // 혼자/같이 진행 여부 (true: 같이, false: 혼자)
    
    private List<Long> selectedMemberIds; // 같이 진행할 그룹원 ID 목록 (shareFlag가 true일 때만 유효)
}
