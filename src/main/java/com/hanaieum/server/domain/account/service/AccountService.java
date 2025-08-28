package com.hanaieum.server.domain.account.service;

import com.hanaieum.server.domain.account.dto.MainAccountResponse;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.member.entity.Member;

public interface AccountService {

    // === 범용 계좌 생성 메서드 ===
    Long createAccount(Member member, String accountName, String bankName, AccountType accountType, Long balance, String password);

    Long createAccount(Long memberId, String accountName, String bankName, AccountType accountType, Long balance, String password);

    // === 주계좌 생성 메서드 ===
    Long createMainAccount(Member member); // 연계 실행, 회원가입 -> 계좌 생성(동일 트랜잭션)

    // === 머니박스 계좌 생성 메서드 ===
    // Long createMoneyBoxAccount(Long memberId, String accountName, String nickname); // 독립 실행용 (API 직접 호출)
    Long createMoneyBoxAccount(Member member, String accountName, String nickname); // 연계 실행용 (버킷리스트→머니박스 연계)

    // === 계좌 조회 메서드 ===
    MainAccountResponse getMainAccount(Member member);

}