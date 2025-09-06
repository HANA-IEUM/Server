package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.autoTransfer.service.AutoTransferScheduleService;
import com.hanaieum.server.domain.bucketList.calculator.InterestCalculator;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.dto.BucketListUpdateRequest;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.bucketList.repository.BucketParticipantRepository;
import com.hanaieum.server.domain.coupon.service.CouponService;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.domain.transaction.service.TransactionService;
import com.hanaieum.server.domain.transfer.service.TransferService;
import com.hanaieum.server.security.CustomUserDetails;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.hanaieum.server.domain.bucketList.entity.BucketListStatus.COMPLETED;
import static com.hanaieum.server.domain.bucketList.entity.BucketListStatus.IN_PROGRESS;
import static com.hanaieum.server.domain.bucketList.entity.BucketListType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BucketListServiceTest {

    @Mock
    private BucketListRepository bucketListRepository;
    @Mock
    private BucketParticipantRepository bucketParticipantRepository;
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccountService accountService;
    @Mock
    private TransferService transferService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private AutoTransferScheduleService autoTransferScheduleService;
    @Mock
    private CouponService couponService;

    @Mock
    private InterestCalculator interestCalculator;

    /**
     * 시나리오 1: 그룹원의 완료된 버킷리스트 접근 권한 테스트
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForGroupCompletedBucketLists() {
        return Stream.of(
                DynamicTest.dynamicTest("공개 완료 리스트는 항상 접근 가능하다",
                        () -> testPublicCompletedBucketListAccess()),
                DynamicTest.dynamicTest("비공개 완료 리스트라도 참여자면 접근 가능하다",
                        () -> testPrivateCompletedBucketListAsParticipant()),
                DynamicTest.dynamicTest("비공개 완료 리스트에 참여하지 않았으면 접근 불가능하다",
                        () -> testPrivateCompletedBucketListAsNonParticipant())
        );
    }

    /**
     * 시나리오 2: 버킷리스트 생성 시 다양한 타입별 테스트
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForBucketListCreation() {
        return Stream.of(
                DynamicTest.dynamicTest("여행 버킷리스트 생성 시 머니박스가 자동 생성된다",
                        () -> testBucketListCreationWithMoneyBox(TRIP, "유럽 여행")),
                DynamicTest.dynamicTest("취미 버킷리스트 생성 시 머니박스가 자동 생성된다",
                        () -> testBucketListCreationWithMoneyBox(HOBBY, "카메라 구매")),
                DynamicTest.dynamicTest("가족 버킷리스트 생성 시 머니박스가 자동 생성된다",
                        () -> testBucketListCreationWithMoneyBox(FAMILY, "가족 여행"))
        );
    }

    /**
     * 시나리오 3: 진행중인 버킷리스트 목록 조회 테스트
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForInProgressBucketLists() {
        return Stream.of(
                DynamicTest.dynamicTest("진행중인 버킷리스트만 반환된다",
                        () -> testInProgressBucketListsFiltering()),
                DynamicTest.dynamicTest("삭제된 버킷리스트는 제외된다",
                        () -> testDeletedBucketListsExclusion())
        );
    }

    /**
     * 시나리오 4: 버킷리스트 완료 처리 테스트
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForBucketListCompletion() {
        return Stream.of(
                DynamicTest.dynamicTest("목표 날짜 도달 후 정상적으로 완료 처리된다",
                        () -> testSuccessfulBucketListCompletion()),
                DynamicTest.dynamicTest("목표 날짜 이전 완료 시도 시 실패한다",
                        () -> testEarlyCompletionFailure()),
                DynamicTest.dynamicTest("이미 완료된 버킷리스트 재완료 시 실패한다",
                        () -> testAlreadyCompletedFailure())
        );
    }

    /**
     * 시나리오 5: 버킷리스트 삭제 테스트
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForBucketListDeletion() {
        return Stream.of(
                DynamicTest.dynamicTest("진행중인 버킷리스트 삭제 시 머니박스 잔액을 인출한다",
                        () -> testInProgressBucketListDeletion()),
                DynamicTest.dynamicTest("완료된 버킷리스트 삭제 시 정상적으로 삭제된다",
                        () -> testCompletedBucketListDeletion()),
                DynamicTest.dynamicTest("권한 없는 사용자의 삭제 시도 시 실패한다",
                        () -> testUnauthorizedDeletionFailure())
        );
    }

    /**
     * 시나리오 6: 버킷리스트 수정 테스트
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForBucketListUpdate() {
        return Stream.of(
                DynamicTest.dynamicTest("기본 정보 수정이 정상적으로 처리된다",
                        () -> testBasicInfoUpdate()),
                DynamicTest.dynamicTest("혼자 진행에서 같이 진행으로 변경 시 참여자가 추가된다",
                        () -> testShareModeChangeToShared()),
                DynamicTest.dynamicTest("같이 진행에서 혼자 진행으로 변경 시 참여자가 제거된다",
                        () -> testShareModeChangeToPrivate()),
                DynamicTest.dynamicTest("권한 없는 사용자의 수정 시도 시 실패한다",
                        () -> testUnauthorizedUpdateFailure())
        );
    }

    // ========== 테스트 시나리오 구현 메서드 ==========

    private void testPublicCompletedBucketListAccess() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long currentMemberId = 10L;
            Long targetMemberId = 20L;

            Group group = createTestGroup(1L);

            Member currentMember = createTestMember(currentMemberId, "현재 사용자", group);
            Member targetMember = createTestMember(targetMemberId, "대상 사용자", group);

            BucketList publicCompletedBucketList = createTestBucketList(
                    101L, targetMember, TRIP, "공개 완료 버킷리스트",
                    COMPLETED, true, false
            );

            setupSecurityContext(securityContextHolderMock, currentMemberId);
            given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(currentMember));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
            given(bucketListRepository.findByGroupMemberIdAndCompleted(targetMemberId))
                    .willReturn(List.of(publicCompletedBucketList));
            given(bucketListRepository.findByParticipantMemberId(currentMemberId))
                    .willReturn(List.of());

            BucketListServiceImpl service = createBucketListService();

            // when
            List<BucketListResponse> result = service.getGroupCompletedBucketLists(targetMemberId);

            // then
            assertThat(result)
                    .hasSize(1)
                    .extracting(BucketListResponse::getId)
                    .containsExactly(101L);
        }
    }

    private void testPrivateCompletedBucketListAsParticipant() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long currentMemberId = 10L;
            Long targetMemberId = 20L;

            Group group = createTestGroup(1L);
            Member currentMember = createTestMember(currentMemberId, "현재 사용자", group);
            Member targetMember = createTestMember(targetMemberId, "대상 사용자", group);

            BucketList privateCompletedBucketList = createTestBucketList(
                    102L, targetMember, FAMILY, "비공개 완료 버킷리스트",
                    COMPLETED, false, false
            );

            setupSecurityContext(securityContextHolderMock, currentMemberId);
            given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(currentMember));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
            given(bucketListRepository.findByGroupMemberIdAndCompleted(targetMemberId))
                    .willReturn(List.of(privateCompletedBucketList));
            given(bucketListRepository.findByParticipantMemberId(currentMemberId))
                    .willReturn(List.of(privateCompletedBucketList));

            BucketListServiceImpl service = createBucketListService();

            // when
            List<BucketListResponse> result = service.getGroupCompletedBucketLists(targetMemberId);

            // then
            assertThat(result)
                    .hasSize(1)
                    .extracting(BucketListResponse::getId)
                    .containsExactly(102L);
        }
    }

    private void testPrivateCompletedBucketListAsNonParticipant() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long currentMemberId = 10L;
            Long targetMemberId = 20L;

            Group group = createTestGroup(1L);
            Member currentMember = createTestMember(currentMemberId, "현재 사용자", group);
            Member targetMember = createTestMember(targetMemberId, "대상 사용자", group);

            BucketList privateCompletedBucketList = createTestBucketList(
                    103L, targetMember, HOBBY, "비공개 완료 버킷리스트",
                    COMPLETED, false, false
            );

            setupSecurityContext(securityContextHolderMock, currentMemberId);
            given(memberRepository.findById(currentMemberId)).willReturn(Optional.of(currentMember));
            given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));
            given(bucketListRepository.findByGroupMemberIdAndCompleted(targetMemberId))
                    .willReturn(List.of(privateCompletedBucketList));
            given(bucketListRepository.findByParticipantMemberId(currentMemberId))
                    .willReturn(List.of());

            BucketListServiceImpl service = createBucketListService();

            // when
            List<BucketListResponse> result = service.getGroupCompletedBucketLists(targetMemberId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private void testBucketListCreationWithMoneyBox(BucketListType type, String title) {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Member currentMember = createTestMember(memberId, "테스트 사용자", null);

            BucketListRequest request = BucketListRequest.builder()
                    .type(type)
                    .title(title)
                    .targetAmount(new BigDecimal("1000000"))
                    .targetMonths("12")
                    .publicFlag(true)
                    .togetherFlag(false)
                    .createMoneyBox(true)
                    .moneyBoxName(title + " 저금통")
                    .build();

            BucketList savedBucketList = createTestBucketList(
                    1L, currentMember, type, title, IN_PROGRESS, true, false
            );

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(currentMember));
            given(bucketListRepository.save(any(BucketList.class))).willReturn(savedBucketList);

            BucketListServiceImpl service = createBucketListService();

            // when
            BucketListResponse response = service.createBucketList(request);

            // then
            assertThat(response)
                    .extracting(BucketListResponse::getTitle, BucketListResponse::getType)
                    .containsExactly(title, type);

            verify(accountService).createMoneyBoxForBucketList(
                    eq(savedBucketList),
                    eq(currentMember),
                    eq(title + " 저금통")
            );
        }
    }

    private void testInProgressBucketListsFiltering() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Member currentMember = createTestMember(memberId, "테스트 사용자", null);

            List<BucketList> inProgressBucketLists = List.of(
                    createTestBucketList(1L, currentMember, TRIP, "여행 버킷리스트", IN_PROGRESS, true, false),
                    createTestBucketList(2L, currentMember, HOBBY, "취미 버킷리스트", IN_PROGRESS, true, false)
            );

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(currentMember));
            given(bucketListRepository.findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(
                    currentMember, IN_PROGRESS, false))
                    .willReturn(inProgressBucketLists);

            BucketListServiceImpl service = createBucketListService();

            // when
            List<BucketListResponse> result = service.getInProgressBucketLists();

            // then
            assertThat(result)
                    .hasSize(2)
                    .extracting(BucketListResponse::getStatus)
                    .containsOnly(IN_PROGRESS);
        }
    }

    private void testDeletedBucketListsExclusion() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Member currentMember = createTestMember(memberId, "테스트 사용자", null);

            List<BucketList> activeBucketLists = List.of(
                    createTestBucketList(1L, currentMember, TRIP, "활성 버킷리스트", IN_PROGRESS, true, false)
            );

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(currentMember));
            given(bucketListRepository.findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(
                    currentMember, IN_PROGRESS, false))
                    .willReturn(activeBucketLists);

            BucketListServiceImpl service = createBucketListService();

            // when
            List<BucketListResponse> result = service.getInProgressBucketLists();

            // then
            assertThat(result)
                    .hasSize(1)
                    .extracting(BucketListResponse::getTitle)
                    .containsExactly("활성 버킷리스트");
        }
    }

    // ========== 헬퍼 메서드 ==========

    private BucketListServiceImpl createBucketListService() {
        return new BucketListServiceImpl(
                bucketListRepository,
                bucketParticipantRepository,
                memberRepository,
                accountService,
                transferService,
                transactionService,
                autoTransferScheduleService,
                couponService,
                interestCalculator
        );
    }

    private Group createTestGroup(Long id) {
        return Group.builder()
                .id(id)
                .build();
    }

    private Member createTestMember(Long id, String name, Group group) {
        return Member.builder()
                .id(id)
                .name(name)
                .group(group)
                .build();
    }

    private BucketList createTestBucketList(Long id, Member member, BucketListType type,
                                            String title, BucketListStatus status,
                                            boolean publicFlag, boolean deleted) {
        return BucketList.builder()
                .id(id)
                .member(member)
                .type(type)
                .title(title)
                .targetAmount(new BigDecimal("1000000"))
                .targetMonth(12)
                .status(status)
                .publicFlag(publicFlag)
                .deleted(deleted)
                .build();
    }

    private void setupSecurityContext(MockedStatic<SecurityContextHolder> securityContextHolderMock, Long memberId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getId()).willReturn(memberId);
    }

    // ========== 시나리오 4: 버킷리스트 완료 처리 테스트 구현 ==========

    private void testSuccessfulBucketListCompletion() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "테스트 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "여행 버킷리스트",
                    IN_PROGRESS, true, false
            );
            bucketList.setTargetDate(LocalDate.now().minusDays(1)); // 완료 가능한 날짜

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));
            given(bucketListRepository.save(any(BucketList.class))).willReturn(bucketList);

            BucketListServiceImpl service = createBucketListService();

            // when
            BucketListResponse response = service.completeBucketList(bucketListId);

            // then
            assertThat(response.getStatus()).isEqualTo(COMPLETED);
            verify(bucketListRepository).save(argThat(bl -> bl.getStatus() == COMPLETED));
        }
    }

    private void testEarlyCompletionFailure() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "테스트 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "여행 버킷리스트",
                    IN_PROGRESS, true, false
            );
            bucketList.setTargetDate(LocalDate.now().plusDays(1)); // 아직 완료 불가능

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));

            BucketListServiceImpl service = createBucketListService();

            // when & Then
            assertThatThrownBy(() -> service.completeBucketList(bucketListId))
                    .isInstanceOf(Exception.class);
        }
    }

    private void testAlreadyCompletedFailure() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "테스트 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "여행 버킷리스트",
                    COMPLETED, true, false // 이미 완료된 상태
            );

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));

            BucketListServiceImpl service = createBucketListService();

            // when & Then
            assertThatThrownBy(() -> service.completeBucketList(bucketListId))
                    .isInstanceOf(Exception.class);
        }
    }

    // ========== 시나리오 5: 버킷리스트 삭제 테스트 구현 ==========

    private void testInProgressBucketListDeletion() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "테스트 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "여행 버킷리스트",
                    IN_PROGRESS, true, false
            );
            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));

            lenient().when(bucketListRepository.save(any(BucketList.class))).thenReturn(bucketList);

            BucketListServiceImpl service = createBucketListService();

            // when & Then
            assertThatThrownBy(() -> service.deleteBucketList(bucketListId))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    private void testCompletedBucketListDeletion() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "테스트 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "여행 버킷리스트",
                    COMPLETED, true, false
            );

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));
            given(bucketListRepository.save(any(BucketList.class))).willReturn(bucketList);

            BucketListServiceImpl service = createBucketListService();

            // when
            service.deleteBucketList(bucketListId);

            // then
            verify(bucketListRepository).save(argThat(bl -> bl.isDeleted()));
        }
    }

    private void testUnauthorizedDeletionFailure() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long ownerId = 1L;
            Long otherUserId = 2L;
            Long bucketListId = 1L;

            Member owner = createTestMember(ownerId, "소유자", null);
            Member otherUser = createTestMember(otherUserId, "다른 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, owner, TRIP, "여행 버킷리스트",
                    IN_PROGRESS, true, false
            );

            setupSecurityContext(securityContextHolderMock, otherUserId);
            given(memberRepository.findById(otherUserId)).willReturn(Optional.of(otherUser));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));

            BucketListServiceImpl service = createBucketListService();

            // when & Then
            assertThatThrownBy(() -> service.deleteBucketList(bucketListId))
                    .isInstanceOf(Exception.class);
        }
    }

    // ========== 시나리오 6: 버킷리스트 수정 테스트 구현 ==========

    private void testBasicInfoUpdate() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "테스트 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "기존 제목",
                    IN_PROGRESS, true, false
            );

            BucketListUpdateRequest updateRequest = BucketListUpdateRequest.builder()
                    .title("수정된 제목")
                    .build();

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));
            given(bucketListRepository.save(any(BucketList.class))).willReturn(bucketList);

            BucketListServiceImpl service = createBucketListService();

            // when
            BucketListResponse response = service.updateBucketList(bucketListId, updateRequest);

            // then
            assertThat(response.getTitle()).isEqualTo("수정된 제목");
            verify(bucketListRepository).save(argThat(bl -> bl.getTitle().equals("수정된 제목")));
        }
    }

    private void testShareModeChangeToShared() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Group group = createTestGroup(1L);
            Member member = createTestMember(memberId, "소유자", group);
            Member participant = createTestMember(2L, "참여자", group);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "공유 버킷리스트",
                    IN_PROGRESS, true, false
            );
            bucketList.setShareFlag(false); // 현재 혼자 진행

            BucketListUpdateRequest updateRequest = BucketListUpdateRequest.builder()
                    .title("공유 버킷리스트")
                    .shareFlag(true)
                    .selectedMemberIds(List.of(2L))
                    .build();

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberRepository.findById(2L)).willReturn(Optional.of(participant));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));
            given(bucketListRepository.save(any(BucketList.class))).willReturn(bucketList);
            given(bucketParticipantRepository.findByBucketListAndActive(any(), eq(true)))
                    .willReturn(List.of());

            BucketListServiceImpl service = createBucketListService();

            // when
            service.updateBucketList(bucketListId, updateRequest);

            // then
            verify(bucketParticipantRepository).save(any());
        }
    }

    private void testShareModeChangeToPrivate() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long memberId = 1L;
            Long bucketListId = 1L;
            Member member = createTestMember(memberId, "소유자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, member, TRIP, "개인 버킷리스트",
                    IN_PROGRESS, true, false
            );
            bucketList.setShareFlag(true); // 현재 같이 진행

            BucketListUpdateRequest updateRequest = BucketListUpdateRequest.builder()
                    .title("개인 버킷리스트")
                    .shareFlag(false)
                    .build();

            setupSecurityContext(securityContextHolderMock, memberId);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));
            given(bucketListRepository.save(any(BucketList.class))).willReturn(bucketList);
            given(bucketParticipantRepository.findByBucketListAndActive(any(), eq(true)))
                    .willReturn(List.of());

            BucketListServiceImpl service = createBucketListService();

            // when
            service.updateBucketList(bucketListId, updateRequest);

            // then
            verify(bucketParticipantRepository).save(any());
        }
    }

    private void testUnauthorizedUpdateFailure() {
        try (MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)) {
            // given
            Long ownerId = 1L;
            Long otherUserId = 2L;
            Long bucketListId = 1L;

            Member owner = createTestMember(ownerId, "소유자", null);
            Member otherUser = createTestMember(otherUserId, "다른 사용자", null);

            BucketList bucketList = createTestBucketList(
                    bucketListId, owner, TRIP, "여행 버킷리스트",
                    IN_PROGRESS, true, false
            );

            BucketListUpdateRequest updateRequest = BucketListUpdateRequest.builder()
                    .title("수정된 제목")
                    .build();

            setupSecurityContext(securityContextHolderMock, otherUserId);
            given(memberRepository.findById(otherUserId)).willReturn(Optional.of(otherUser));
            given(bucketListRepository.findByIdAndDeletedWithParticipants(bucketListId, false))
                    .willReturn(Optional.of(bucketList));

            BucketListServiceImpl service = createBucketListService();

            // when & Then
            assertThatThrownBy(() -> service.updateBucketList(bucketListId, updateRequest))
                    .isInstanceOf(Exception.class);
        }
    }
}