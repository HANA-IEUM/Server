package com.hanaieum.server.domain.account.service;

import com.hanaieum.server.domain.account.dto.MainAccountResponse;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.bucketList.entity.BucketList;

import java.math.BigDecimal;

public interface AccountService {

    // === 범용 계좌 생성 메서드 ===
    Long createAccount(Member member, String accountName, String bankName, AccountType accountType, BigDecimal balance, String password);

    // === 주계좌 생성 메서드 ===
    Long createMainAccount(Member member); // 연계 실행, 회원가입 -> 계좌 생성(동일 트랜잭션)
    
    // === 머니박스 계좌 생성 메서드 ===
    Account createMoneyBoxAccount(Member member, String boxName, String password); // 연계 실행용 (버킷리스트→머니박스 연계)
    
    // === 버킷리스트 연동 머니박스 생성 메서드 ===
    Account createMoneyBoxForBucketList(BucketList bucketList, Member member, String boxName);
    Account createMoneyBoxForBucketList(BucketList bucketList, Member member, String boxName, 
                                        Boolean enableAutoTransfer, BigDecimal monthlyAmount, Integer transferDay);

    // === 계좌 조회 메서드 ===
    MainAccountResponse getMainAccount(Member member);
    Long getMainAccountIdByMemberId(Long memberId);
    Account findMainAccountByMember(Member member); // 추가: Member 객체로 주계좌 조회
    Account findById(Long accountId);
    Account findByIdWithLock(Long accountId);

    // === 계좌 도메인 검증 메서드 ===
    void validateAccountOwnership(Long accountId, Long memberId);
    void validateAccountPassword(Long accountId, String password);
    void validateSufficientBalance(Long accountId, BigDecimal amount);
    
    // === 계좌 잔액 조작 메서드 ===
    void debitBalance(Long accountId, BigDecimal amount);
    void creditBalance(Long accountId, BigDecimal amount);
    
    // === 머니박스 개수 조회 메서드 ===
    long getMoneyBoxCountByMember(Member member);
    
    // === 계좌 저장 메서드 ===
    Account save(Account account);

}