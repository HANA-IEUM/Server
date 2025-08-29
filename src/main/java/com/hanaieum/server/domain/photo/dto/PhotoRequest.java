package com.hanaieum.server.domain.photo.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoRequest {
    private String imgUrl;
    @Size(max = 30, message = "짧은 한마디를 30자 이내로 입력해주세요.")
    private String caption;
}
