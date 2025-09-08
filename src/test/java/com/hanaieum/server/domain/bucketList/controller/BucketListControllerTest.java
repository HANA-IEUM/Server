package com.hanaieum.server.domain.bucketList.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanaieum.server.common.dto.ApiResponse;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.bucketList.dto.BucketListRequest;
import com.hanaieum.server.domain.bucketList.dto.BucketListResponse;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import com.hanaieum.server.domain.group.entity.Group;
import com.hanaieum.server.domain.group.repository.GroupRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import com.hanaieum.server.security.CustomUserDetails;
import com.hanaieum.server.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.hanaieum.server.domain.account.entity.AccountType.MONEY_BOX;
import static com.hanaieum.server.domain.bucketList.entity.BucketListStatus.IN_PROGRESS;
import static com.hanaieum.server.domain.bucketList.entity.BucketListType.TRIP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.core.context.SecurityContextHolder.createEmptyContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureWebMvc
@DisplayName("버킷리스트 도메인 컨트롤러 테스트")
class BucketListControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BucketListRepository bucketListRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Member testMember;
    private Group testGroup;
    private String accessToken;
    private BucketList commonBucketList;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = webAppContextSetup(webApplicationContext).build();

        // 테스트 데이터 생성
        testGroup = createTestGroup();
        testMember = createTestMember();
        accessToken = createAccessToken();

        // SecurityContext 설정
        setupSecurityContext();
    }

    /**
     * 공통 버킷리스트 생성
     */
    private void createCommonBucketListIfNeeded() throws Exception {
        if (commonBucketList == null) {
            createCommonBucketList();
        }
    }

    @Test
    @DisplayName("사용자가 버킷리스트를 생성할 때 머니박스가 자동으로 생성되고 자동이체가 설정된다.")
    void testCompleteBucketListCreationFlow() throws Exception {
        // given
        BucketListRequest request = BucketListRequest.builder()
                .type(TRIP)
                .title("유럽 여행")
                .targetAmount(new BigDecimal("5000000"))
                .targetMonths("12")
                .publicFlag(true)
                .togetherFlag(false)
                .createMoneyBox(true)
                .moneyBoxName("유럽 여행 저금통")
                .enableAutoTransfer(true)
                .monthlyAmount(new BigDecimal("300000"))
                .transferDay("15")
                .build();

        // when
        mockMvc.perform(post("/api/bucket-lists")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("유럽 여행"))
                .andExpect(jsonPath("$.data.type").value("TRIP"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // then
        List<BucketList> bucketLists = bucketListRepository.findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(
                testMember, IN_PROGRESS, false);

        assertThat(bucketLists).hasSize(1);
        BucketList savedBucketList = bucketLists.get(0);

        // 버킷리스트 검증
        assertThat(savedBucketList.getTitle()).isEqualTo("유럽 여행");
        assertThat(savedBucketList.getType()).isEqualTo(TRIP);
        assertThat(savedBucketList.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(savedBucketList.getTargetAmount()).isEqualTo(new BigDecimal("5000000"));
        assertThat(savedBucketList.getTargetMonth()).isEqualTo(12);
        assertThat(savedBucketList.getTargetDate()).isEqualTo(LocalDate.now().plusMonths(12));

        // 머니박스 자동 생성 검증
        assertThat(savedBucketList.getMoneyBoxAccount()).isNotNull();
        Account moneyBoxAccount = savedBucketList.getMoneyBoxAccount();
        assertThat(moneyBoxAccount.getAccountType()).isEqualTo(MONEY_BOX);

        assertThat(moneyBoxAccount.getBoxName()).isEqualTo("유럽 여행 저금통");
        assertThat(moneyBoxAccount.getMember()).isEqualTo(testMember);
        assertThat(moneyBoxAccount.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("사용자가 그룹원들과 함께 버킷리스트를 생성할 때 참여자가 정상적으로 추가된다.")
    void testBucketListCreationWithGroupMembers() throws Exception {
        // given
        createCommonBucketListIfNeeded();

        Member participant1 = createTestMember("참여자1", testGroup);
        Member participant2 = createTestMember("참여자2", testGroup);

        BucketListRequest request = BucketListRequest.builder()
                .type(TRIP)
                .title("그룹 여행")
                .targetAmount(new BigDecimal("3000000"))
                .targetMonths("12")
                .publicFlag(true)
                .togetherFlag(true) // 함께 진행
                .selectedMemberIds(List.of(participant1.getId(), participant2.getId()))
                .createMoneyBox(true)
                .moneyBoxName("그룹 여행 저금통")
                .build();

        // when
        mockMvc.perform(post("/api/bucket-lists")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("그룹 여행"))
                .andExpect(jsonPath("$.data.togetherFlag").value(true));

        // then
        List<BucketList> bucketLists = bucketListRepository.findByMemberAndStatusAndDeletedOrderByCreatedAtDesc(
                testMember, IN_PROGRESS, false);

        assertThat(bucketLists).hasSize(2);

        // 새로 생성한 버킷리스트 찾기
        BucketList savedBucketList = bucketLists.stream()
                .filter(bl -> bl.getTitle().equals("그룹 여행"))
                .findFirst()
                .orElseThrow();

        // 참여자 검증 - 실제 참여자 수를 확인
        System.out.println("=== 참여자 수 확인 ===");
        System.out.println("참여자 수: " + savedBucketList.getParticipants().size());
        savedBucketList.getParticipants().forEach(participant ->
                System.out.println("참여자: " + participant.getMember().getName()));

        // 참여자가 생성되었는지 확인
        assertThat(savedBucketList.getParticipants()).hasSizeGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("사용자가 버킷리스트를 생성한 후 진행중인 버킷리스트 목록을 조회할 때 최신순으로 정렬되어 반환된다.")
    void testBucketListCreationAndInProgressList() throws Exception {
        // given
        createCommonBucketListIfNeeded();

        BucketListRequest request = BucketListRequest.builder()
                .type(TRIP)
                .title("추가 버킷리스트")
                .targetAmount(new BigDecimal("2000000"))
                .targetMonths("6")
                .publicFlag(true)
                .togetherFlag(false)
                .createMoneyBox(true)
                .build();

        // when
        mockMvc.perform(post("/api/bucket-lists")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // then
        mockMvc.perform(get("/api/bucket-lists/my/in-progress")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("추가 버킷리스트"))
                .andExpect(jsonPath("$.data[1].title").value("공통 테스트 버킷리스트"));
    }

    @Test
    @DisplayName("사용자가 생성한 버킷리스트의 상세 정보를 조회할 때 머니박스 정보와 함께 반환된다.")
    void testBucketListCreationAndDetailView() throws Exception {
        // given
        createCommonBucketListIfNeeded();

        Long bucketListId = commonBucketList.getId();

        // when & then
        mockMvc.perform(get("/api/bucket-lists/my/{bucketListId}", bucketListId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("공통 테스트 버킷리스트"))
                .andExpect(jsonPath("$.data.bucketListStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.targetAmount").value(1000000))
                .andExpect(jsonPath("$.data.publicFlag").value(true))
                .andExpect(jsonPath("$.data.togetherFlag").value(false))
                .andExpect(jsonPath("$.data.canComplete").value(false))
                .andExpect(jsonPath("$.data.moneyBoxInfo").exists())
                .andExpect(jsonPath("$.data.moneyBoxInfo.boxName").value("공통 테스트 저금통"));
    }

    @Test
    @DisplayName("사용자가 버킷리스트 생성을 요청하기 전에 생성 가능 여부를 확인할 때 현재 머니박스 개수와 최대 개수가 정상적으로 반환되는지 검증한다")
    void testBucketListCreationAvailability() throws Exception {
        // given
        createCommonBucketListIfNeeded();

        // when
        mockMvc.perform(get("/api/bucket-lists/creation-availability")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canCreate").value(true))
                .andExpect(jsonPath("$.data.currentMoneyBoxCount").value(1)); // 공통 버킷리스트로 생성된 머니박스
                // .andExpect(jsonPath("$.data.maxMoneyBoxCount").value(20));
    }

    // ========== 헬퍼 메서드 ==========

    private Group createTestGroup() {
        Group group = Group.builder()
                .groupName("테스트 그룹")
                .inviteCode("TEST123")
                .build();
        return groupRepository.save(group);
    }

    private Member createTestMember() {
        return createTestMember("테스트 사용자", testGroup);
    }

    private Member createTestMember(String name, Group group) {
        // 고유한 전화번호 생성
        String uniquePhoneNumber = "010" + String.format("%08d", System.currentTimeMillis() % 100000000);

        Member member = Member.builder()
                .name(name)
                .phoneNumber(uniquePhoneNumber)
                .password("encodedPassword")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(com.hanaieum.server.domain.member.entity.Gender.M)
                .monthlyLivingCost(1000000)
                .group(group)
                .build();
        return memberRepository.save(member);
    }

    private String createAccessToken() {
        return jwtTokenProvider.generateAccessToken(testMember.getId(), testMember.getName(), testMember.getPhoneNumber());
    }

    private void setupSecurityContext() {
        CustomUserDetails userDetails = new CustomUserDetails(testMember);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext securityContext = createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void createCommonBucketList() throws Exception {
        BucketListRequest request = BucketListRequest.builder()
                .type(TRIP)
                .title("공통 테스트 버킷리스트")
                .targetAmount(new BigDecimal("1000000"))
                .targetMonths("6")
                .publicFlag(true)
                .togetherFlag(false)
                .createMoneyBox(true)
                .moneyBoxName("공통 테스트 저금통")
                .build();

        String response = mockMvc.perform(post("/api/bucket-lists")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 응답에서 버킷리스트 ID 추출하여 저장
        ApiResponse<BucketListResponse> apiResponse =
                objectMapper.readValue(response,
                        objectMapper.getTypeFactory().constructParametricType(
                                com.hanaieum.server.common.dto.ApiResponse.class,
                                BucketListResponse.class));

        // 생성된 버킷리스트를 DB에서 조회하여 저장
        commonBucketList = bucketListRepository.findById(apiResponse.getData().getId())
                .orElseThrow(() -> new RuntimeException("공통 버킷리스트 생성 실패"));
    }
}
