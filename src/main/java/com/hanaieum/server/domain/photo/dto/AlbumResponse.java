package com.hanaieum.server.domain.photo.dto;

import com.hanaieum.server.domain.member.dto.MemberInfoResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumResponse {
    private List<MemberInfoResponse> members;
    private List<PhotoResponse> photos;
}
