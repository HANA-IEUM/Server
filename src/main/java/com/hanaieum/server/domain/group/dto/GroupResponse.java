package com.hanaieum.server.domain.group.dto;

import com.hanaieum.server.domain.member.dto.MemberInfoResponse;
import com.hanaieum.server.domain.group.entity.Group;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupResponse {
    private Long groupId;
    private String groupName;
    private String inviteCode;
    private List<MemberInfoResponse> members;

    public static GroupResponse from(Group group) {
        List<MemberInfoResponse> memberInfos = group.getMembers() != null ?
                group.getMembers().stream()
                        .map(member -> MemberInfoResponse.builder()
                                .memberId(member.getId())
                                .name(member.getName())
                                .build())
                        .collect(Collectors.toList()) :
                new ArrayList<>(); // 그룹이 비어있으면 빈 리스트

        return GroupResponse.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .inviteCode(group.getInviteCode())
                .members(memberInfos)
                .build();
    }
}
