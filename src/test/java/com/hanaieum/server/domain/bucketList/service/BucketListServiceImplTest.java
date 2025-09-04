package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.entity.BucketListType;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class BucketListServiceImplTest {

    @Mock
    private BucketListRepository bucketListRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private BucketListServiceImpl bucketListService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    void setUp() {
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMock.close();
        bucketListRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @DisplayName("버킷리스트 생성 시 현재 로그인한 사용자 정보가 올바르게 사용된다.")
    @Test
    void createBucketList() {
        // given
        Long memberId = 1L;
        Member currentMember = Member.builder()
                .id(memberId)
                .name("테스트 사용자")
                .build();

        BucketListRequest request = BucketListRequest.builder()
                .type(BucketListType.TRIP)
                .title("테스트 버킷리스트")
                .targetAmount(new BigDecimal("1000000"))
                .targetMonths("12")
                .publicFlag(true)
                .togetherFlag(false)
                .createMoneyBox(true)
                .build();

        BucketList savedBucketList = BucketList.builder()
                .id(1L)
                .member(currentMember)
                .type(BucketListType.TRIP)
                .title("테스트 버킷리스트")
                .targetAmount(new BigDecimal("1000000"))
                .targetMonth(12)
                .status(BucketListStatus.IN_PROGRESS)
                .deleted(false)
                .build();

        setupSecurityContext(memberId);  // SecurityContext 모킹 설정

        given(memberRepository.findById(memberId)).willReturn(Optional.of(currentMember));
        given(bucketListRepository.save(any(BucketList.class)))
                .willReturn(savedBucketList);

        // when
        BucketListResponse response = bucketListService.createBucketList(request);

        // then
        assertThat(response.getTitle()).isEqualTo("테스트 버킷리스트");
        assertThat(response.getType()).isEqualTo(BucketListType.TRIP);
        assertThat(response.getStatus()).isEqualTo(BucketListStatus.IN_PROGRESS);
        assertThat(response.getMemberId()).isEqualTo(memberId);
    }

    @DisplayName("버킷리스트 생성 시 존재하지 않는 사용자로 인해 실패한다.")
    @Test
    void createBucketList_WithNonExistentUser() {
        // given
        Long memberId = 999L;
        BucketListRequest request = BucketListRequest.builder()
                .type(BucketListType.TRIP)
                .title("테스트 버킷리스트")
                .targetAmount(new BigDecimal("1000000"))
                .targetMonths("12")
                .publicFlag(true)
                .togetherFlag(false)
                .createMoneyBox(false)
                .build();

        // SecurityContext 모킹 설정
        setupSecurityContext(memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bucketListService.createBucketList(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @DisplayName("버킷리스트 생성 시 자동이체가 포함된 머니박스가 생성된다.")
    @Test
    void createBucketList_WithMoneyBoxAndAutoTransfer() {
        // given
        Long memberId = 1L;
        Member currentMember = Member.builder()
                .id(memberId)
                .name("테스트 사용자")
                .build();

        BucketListRequest request = BucketListRequest.builder()
                .type(BucketListType.TRIP)
                .title("테스트 버킷리스트")
                .targetAmount(new BigDecimal("1000000"))
                .targetMonths("12")
                .publicFlag(true)
                .togetherFlag(false)
                .createMoneyBox(true)
                .moneyBoxName("테스트 머니박스")
                .enableAutoTransfer(true)
                .monthlyAmount(new BigDecimal("100000"))
                .transferDay("15")
                .build();

        BucketList savedBucketList = BucketList.builder()
                .id(1L)
                .member(currentMember)
                .type(BucketListType.TRIP)
                .title("테스트 버킷리스트")
                .targetAmount(new BigDecimal("1000000"))
                .targetMonth(12)
                .status(BucketListStatus.IN_PROGRESS)
                .deleted(false)
                .build();

        setupSecurityContext(memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(currentMember));
        given(bucketListRepository.save(any(BucketList.class))).willReturn(savedBucketList);

        // when
        BucketListResponse response = bucketListService.createBucketList(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("테스트 버킷리스트");
        
        // 자동이체 정보와 함께 호출되었는지 검증
        verify(accountService).createMoneyBoxForBucketList(
                eq(savedBucketList),
                eq(currentMember),
                eq("테스트 머니박스"),
                eq(true),
                eq(new BigDecimal("100000")),
                eq(15)
        );
    }

    /**
     * SecurityContext 모킹을 설정하는 헬퍼 메서드
     */
    private void setupSecurityContext(Long memberId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getId()).willReturn(memberId);
    }

}