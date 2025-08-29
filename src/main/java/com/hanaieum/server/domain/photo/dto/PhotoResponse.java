package com.hanaieum.server.domain.photo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoResponse {
    private Long photoId;
    private String name;
    private String imgUrl;
    private String caption;
    private LocalDateTime createdAt;
    private boolean mine;
}
