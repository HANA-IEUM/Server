package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;
import com.hanaieum.server.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyBoxServiceImpl implements MoneyBoxService {

    private final AccountRepository accountRepository;

    @Override
    public MoneyBoxResponse updateMoneyBoxName(Long accountId, MoneyBoxRequest request) {
        Member currentMember = getCurrentMember();
        
        // 계좌 조회 및 검증
        Account account = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 계좌 소유권 검증
        if (!account.getMember().getId().equals(currentMember.getId())) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
        
        // MONEY_BOX 타입 계좌인지 검증
        if (account.getAccountType() != AccountType.MONEY_BOX) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        
        // 머니박스 별명 업데이트
        account.setBoxName(request.getBoxName());
        account.setName(request.getBoxName()); // 계좌명도 함께 업데이트
        
        Account savedAccount = accountRepository.save(account);
        
        log.info("머니박스 별명 수정 완료: accountId={}, newBoxName={}", accountId, request.getBoxName());
        
        return MoneyBoxResponse.of(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyBoxResponse> getMyMoneyBoxList() {
        Member currentMember = getCurrentMember();
        
        // 사용자의 모든 MONEY_BOX 타입 계좌 조회
        List<Account> moneyBoxAccounts = accountRepository.findAllByMemberAndAccountTypeAndDeletedFalse(
                currentMember, AccountType.MONEY_BOX);
        
        log.info("머니박스 목록 조회 완료: memberId={}, count={}", currentMember.getId(), moneyBoxAccounts.size());
        
        return moneyBoxAccounts.stream()
                .map(MoneyBoxResponse::of)
                .toList();
    }

    private Member getCurrentMember() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userDetails.getMember();
    }
}