package com.hanaieum.server.domain.member.entity;

import com.hanaieum.server.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber; // 전화번호 (로그인 아이디)

    @Column(nullable = false)
    private String password; // 간편비밀번호 (암호화 저장)

    @Column(nullable = false, length = 50)
    private String name; // 이름

    @Column(nullable = false)
    private LocalDate birthDate; // 생년월일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; // 성별 (M/F)

    @Column(name = "is_active", nullable = false)
    private boolean active = true; // 활성화 여부

    // 그룹 안내 문구 (다시 보지 않기)
    @Column(nullable = false)
    private boolean hideGroupPrompt = false;

    // 월 생활비
    @Column(nullable = false)
    private Integer monthlyLivingCost;

    // 주계좌 연결 여부
    @Column(name = "is_main_account_linked", nullable = false)
    private boolean mainAccountLinked = false;

}
