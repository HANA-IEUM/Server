package com.hanaieum.server.domain.bucketList.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 버킷리스트 엔티티
 * 사용자가 생성한 버킷리스트 정보를 관리합니다.
 */
@Entity
@Table(name = "bucket_lists")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BucketList extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== 기본 정보 ====================
    
    /**
     * 버킷리스트 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 버킷리스트 카테고리 (여행, 취미, 건강 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "category")
    private BucketListType type;

    /**
     * 버킷리스트 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    // ==================== 목표 정보 ====================
    
    /**
     * 목표 금액
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    /**
     * 목표 완료 날짜
     */
    @Column(nullable = false)
    private LocalDate targetDate;

    // ==================== 설정 정보 ====================
    
    /**
     * 공개 여부 (true: 공개, false: 비공개)
     */
    @Column(nullable = false)
    private boolean publicFlag;

    /**
     * 공유 여부 (true: 같이 진행, false: 혼자 진행)
     */
    @Column(nullable = false)
    private boolean shareFlag;

    /**
     * 버킷리스트 상태 (진행중, 완료 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BucketListStatus status;

    // ==================== 관리 정보 ====================
    
    /**
     * 삭제 여부 (소프트 삭제)
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * 원본 버킷리스트 ID (공동 버킷리스트인 경우)
     * null이면 원본 버킷리스트, 값이 있으면 복사본
     */
    @Column(name = "original_bucket_list_id", nullable = true)
    private Long originalBucketListId;

    // ==================== 연관 관계 ====================
    
    /**
     * 버킷리스트 참여자 목록
     */
    @OneToMany(mappedBy = "bucketList", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BucketParticipant> participants = new ArrayList<>();

    /**
     * 연동된 머니박스 계좌
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "money_box_account_id", nullable = true)
    private Account moneyBoxAccount;

    // ==================== 비즈니스 메서드 ====================
    
    /**
     * 원본 버킷리스트인지 확인
     */
    public boolean isOriginalBucketList() {
        return originalBucketListId == null;
    }

    /**
     * 공동 버킷리스트인지 확인
     */
    public boolean isSharedBucketList() {
        return shareFlag;
    }

    /**
     * 진행중인 버킷리스트인지 확인
     */
    public boolean isInProgress() {
        return status == BucketListStatus.IN_PROGRESS;
    }

    /**
     * 완료된 버킷리스트인지 확인
     */
    public boolean isCompleted() {
        return status == BucketListStatus.COMPLETED;
    }

    /**
     * 만료된 버킷리스트인지 확인
     */
    public boolean isExpired() {
        return targetDate.isBefore(LocalDate.now()) && isInProgress();
    }

    /**
     * 활성화된 참여자 수 조회
     */
    public long getActiveParticipantCount() {
        return participants.stream()
                .filter(BucketParticipant::getActive)
                .count();
    }
}
