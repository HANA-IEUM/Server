package com.hanaieum.server.domain.group.service;

import com.hanaieum.server.domain.group.dto.GroupCreateRequest;
import com.hanaieum.server.domain.group.dto.GroupJoinRequest;
import com.hanaieum.server.domain.group.dto.GroupResponse;
import com.hanaieum.server.domain.member.entity.Member;

public interface GroupService {

    // 새로운 그룹 생성 후 생성한 멤버의 소속 그룹 업데이트
    GroupResponse createGroup(GroupCreateRequest groupCreateRequest, Long memberId);

    // 초대 코드로 그룹에 참여
    void joinGroup(GroupJoinRequest groupJoinRequest, Long memberId);

    // 그룹 및 그룹원 정보 조회
    GroupResponse getGroupInfo(Long memberId);

    // 초대코드 생성
    String generateInviteCode();

}
