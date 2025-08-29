package com.hanaieum.server.domain.photo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoUploadResponse {
    private String imgUrl;
}
