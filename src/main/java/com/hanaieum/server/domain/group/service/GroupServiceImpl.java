package com.hanaieum.server.domain.group.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.group.dto.GroupCreateRequest;
import com.hanaieum.server.domain.group.dto.GroupJoinRequest;
import com.hanaieum.server.domain.group.dto.GroupResponse;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.group.repository.GroupRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    // 그룹 생성
    @Override
    public GroupResponse createGroup(GroupCreateRequest groupCreateRequest, Long memberId) {

        // 멤버가 존재하는지 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 멤버가 이미 그룹에 속해 있는지 확인
        if (member.getGroup() != null) {
            throw new CustomException(ErrorCode.MEMBER_ALREADY_JOIN_GROUP);
        }

        // 초대 코드 생성
        String inviteCode = generateInviteCode();

        // Group 엔티티 생성
        Group newGroup = Group.builder()
                .groupName(groupCreateRequest.getGroupName())
                .inviteCode(inviteCode)
                .isActive(true)
                .build();

        Group savedGroup = groupRepository.save(newGroup);

        member.setGroup(savedGroup); // member의 소속 그룹 업데이트
        member.setHideGroupPrompt(true); // 그룹프롬프트 다시 안보이도록
        memberRepository.save(member);

        savedGroup.getMembers().add(member); // group 객체의 members 리스트에 추가

        log.info("그룹 생성 완료 - 그룹명: {}, 그룹 ID: {}", savedGroup.getGroupName(),savedGroup.getId());

        return GroupResponse.from(savedGroup);
    }

    // 그룹 참여
    @Override
    public void joinGroup(GroupJoinRequest groupJoinRequest, Long memberId) {

        // 멤버 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 멤버가 이미 그룹에 속해 있는지 확인
        if (member.getGroup() != null) {
            throw new CustomException(ErrorCode.MEMBER_ALREADY_JOIN_GROUP);
        }

        // 초대 코드로 그룹 조회
        Group targetGroup = groupRepository.findByInviteCode(groupJoinRequest.getInviteCode())
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 그룹 참여 멤버의 소속 그룹 업데이트
        member.setGroup(targetGroup); // member 소속 그룹 업데이트
        member.setHideGroupPrompt(true);
        memberRepository.save(member);

        targetGroup.getMembers().add(member); // group 객체의 members 리스트에 추가

        log.info("멤버 {}가 그룹 {}에 참여했습니다.", member.getName(), targetGroup.getGroupName());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupInfo(Long memberId) {

        // 멤버 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 멤버가 속한 그룹 확인
        Group group = member.getGroup();
        if (group == null) {
            return null;
        }

        return GroupResponse.from(group);
    }

    @Override
    public String generateInviteCode() {
        final String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final int length = 8;
        SecureRandom rand = new SecureRandom();

        String code;
        do {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(charset.charAt(rand.nextInt(charset.length())));
            }
            code = sb.toString();
        } while (groupRepository.existsByInviteCode(code));

        return code;
    }

}
