package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BucketListServiceImplDynamicTest {

    @Mock
    BucketListRepository bucketListRepository;

    @Mock
    MemberRepository memberRepository;

    @Spy
    @InjectMocks
    BucketListServiceImpl bucketListServiceImpl;


    @TestFactory
    Stream<DynamicTest> dynamicTestsForCompletedLists() {
        BucketListRepository bucketListRepository = mock(BucketListRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        BucketListServiceImpl service = spy(new BucketListServiceImpl(bucketListRepository, memberRepository));

        Group group = new Group();
        group.setId(1L);

        Member current = new Member();
        current.setId(10L);
        current.setGroup(group);

        Member target = new Member();
        target.setId(20L);
        target.setGroup(group);

        doReturn(current).when(service).getCurrentMember();
        when(memberRepository.findById(20L)).thenReturn(Optional.of(target));

        // 공통 리스트
        BucketList pubCompleted = bucketList(101L, target, true, BucketListStatus.COMPLETED, LocalDateTime.now());
        BucketList privCompleted = bucketList(102L, target, false, BucketListStatus.COMPLETED, LocalDateTime.now());

        return Stream.of(
                DynamicTest.dynamicTest("공개 완료 리스트는 항상 접근 가능합니다.",
                        () -> {
                            when(bucketListRepository.findByGroupMemberIdAndCompleted(20L))
                                    .thenReturn(List.of(pubCompleted));
                            when(bucketListRepository.findByParticipantMemberId(current.getId()))
                                    .thenReturn(List.of());

                            List<BucketListResponse> result = service.getGroupCompletedBucketLists(20L);

                            assertThat(result).extracting("id").containsExactly(101L);
                        }),
                DynamicTest.dynamicTest("비공개 완료 리스트라도 참여자면 접근 가능합니다.",
                        () -> {
                            when(bucketListRepository.findByGroupMemberIdAndCompleted(20L))
                                    .thenReturn(List.of(privCompleted));
                            when(bucketListRepository.findByParticipantMemberId(current.getId()))
                                    .thenReturn(List.of(privCompleted));

                            List<BucketListResponse> result = service.getGroupCompletedBucketLists(20L);

                            assertThat(result).extracting("id").containsExactly(102L);
                        }),
                DynamicTest.dynamicTest("비공개 완료 리스트에 참여하지 않았으면 접근 불가능합니다.",
                        () -> {
                            when(bucketListRepository.findByGroupMemberIdAndCompleted(20L))
                                    .thenReturn(List.of(privCompleted));
                            when(bucketListRepository.findByParticipantMemberId(current.getId()))
                                    .thenReturn(List.of());

                            List<BucketListResponse> result = service.getGroupCompletedBucketLists(20L);

                            assertThat(result).isEmpty();
                        })
        );
    }

    private static BucketList bucketList(Long id, Member owner, boolean pub,
                                         BucketListStatus status, LocalDateTime createdAt) {
        BucketList b = new BucketList();
        b.setId(id);
        b.setMember(owner);
        b.setPublicFlag(pub);
        b.setStatus(status);
        b.setCreatedAt(createdAt);
        return b;
    }
}
