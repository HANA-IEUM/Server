package com.hanaieum.server.domain.member.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoResponse {
    private Long memberId;
    private String name;
}
