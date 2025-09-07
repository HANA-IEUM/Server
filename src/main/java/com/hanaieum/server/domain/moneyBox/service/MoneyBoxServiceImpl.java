package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.account.repository.AccountRepository;
import com.hanaieum.server.domain.account.service.AccountService;
import com.hanaieum.server.domain.autoTransfer.entity.AutoTransferSchedule;
import com.hanaieum.server.domain.autoTransfer.service.AutoTransferScheduleService;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxInfoResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxUpdateRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MoneyBoxServiceImpl implements MoneyBoxService {

    private final AccountRepository accountRepository;
    private final AutoTransferScheduleService autoTransferScheduleService;
    private final AccountService accountService;

    @Override
    @Transactional
    public MoneyBoxUpdateResponse updateMoneyBox(Member member, Long accountId, MoneyBoxUpdateRequest request) {
        
        // 계좌 조회 및 검증
        Account account = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 계좌 소유권 검증
        if (!account.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
        
        // MONEY_BOX 타입 계좌인지 검증
        if (account.getAccountType() != AccountType.MONEY_BOX) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        
        // 주계좌 비밀번호 검증 (머니박스 수정 시 무조건 필요)
        validateAccountPassword(member, request.getAccountPassword());
        
        // 머니박스 별명 업데이트
        account.setBoxName(request.getBoxName());
        Account savedAccount = accountRepository.save(account);
        
        // 자동이체 설정 처리 (AutoTransferScheduleService 사용)
        updateAutoTransferSettings(account, member, request);
        
        log.info("머니박스 수정 완료: accountId={}, newBoxName={}, autoTransferEnabled={}, monthlyAmount={}, transferDay={}", 
                accountId, request.getBoxName(), request.getAutoTransferEnabled(), request.getMonthlyAmount(), request.getTransferDay());
        
        // 수정 후 현재 상태 조회하여 응답 생성
        return createUpdateResponse(savedAccount, member);
    }

    @Override
    public List<MoneyBoxResponse> getMyMoneyBoxList(Member member) {
        // 사용자의 모든 MONEY_BOX 타입 계좌 조회
        List<Account> moneyBoxAccounts = accountService.findMoneyBoxes(member);
        
        log.info("머니박스 목록 조회 완료: memberId={}, count={}", member.getId(), moneyBoxAccounts.size());
        
        return moneyBoxAccounts.stream()
                .map(MoneyBoxResponse::of)
                .toList();
    }

    @Override
    public MoneyBoxInfoResponse getMoneyBoxInfo(Member member, Long boxId) {
        // 머니박스 계좌 조회 및 검증
        Account account = accountRepository.findByIdAndDeletedFalse(boxId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 계좌 소유권 검증
        if (!account.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
        
        // MONEY_BOX 타입 계좌인지 검증
        if (account.getAccountType() != AccountType.MONEY_BOX) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        
        // 자동이체 스케줄 조회 (현재 + 미래)
        Account mainAccount = accountService.findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        Optional<AutoTransferSchedule> currentSchedule = autoTransferScheduleService
                .getCurrentSchedule(mainAccount, account);
        List<AutoTransferSchedule> futureSchedules = autoTransferScheduleService
                .getFutureSchedules(mainAccount, account);
        
        log.info("머니박스 요약 조회 완료: boxId={}, hasCurrentSchedule={}, hasFutureSchedule={}", 
                boxId, currentSchedule.isPresent(), !futureSchedules.isEmpty());
        
        return MoneyBoxInfoResponse.of(account, currentSchedule.orElse(null), futureSchedules);
    }

    @Override
    public MoneyBoxUpdateResponse getMoneyBoxForEdit(Member member, Long boxId) {
        // 머니박스 계좌 조회 및 검증
        Account account = accountRepository.findByIdAndDeletedFalse(boxId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 계좌 소유권 검증
        if (!account.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.ACCOUNT_ACCESS_DENIED);
        }
        
        // MONEY_BOX 타입 계좌인지 검증
        if (account.getAccountType() != AccountType.MONEY_BOX) {
            throw new CustomException(ErrorCode.INVALID_ACCOUNT_TYPE);
        }
        
        log.info("머니박스 수정 폼 데이터 조회 완료: boxId={}", boxId);
        
        return createUpdateResponse(account, member);
    }

    /**
     * 자동이체 설정 업데이트 (AutoTransferScheduleService 위임)
     */
    private void updateAutoTransferSettings(Account moneyBoxAccount, Member member, MoneyBoxUpdateRequest request) {
        // 사용자의 주계좌 조회 (출금 계좌)
        Account mainAccount = accountService.findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // AutoTransferScheduleService에 위임
        autoTransferScheduleService.updateSchedule(
                mainAccount, 
                moneyBoxAccount, 
                request.getAutoTransferEnabled(), 
                request.getMonthlyAmount(), 
                request.getTransferDay()
        );
    }
    
    /**
     * UpdateResponse 생성
     */
    private MoneyBoxUpdateResponse createUpdateResponse(Account account, Member member) {
        // 주계좌 조회
        Account mainAccount = accountService.findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // 현재 활성 스케줄과 미래 스케줄 조회
        Optional<AutoTransferSchedule> currentSchedule = autoTransferScheduleService.getCurrentSchedule(mainAccount, account);
        List<AutoTransferSchedule> futureSchedules = autoTransferScheduleService.getFutureSchedules(mainAccount, account);
        
        return MoneyBoxUpdateResponse.of(account, currentSchedule, futureSchedules);
    }
    
    /**
     * 계좌 비밀번호 검증
     */
    private void validateAccountPassword(Member member, String password) {
        // 주계좌 조회 (자동이체는 주계좌에서 출금)
        Account mainAccount = accountService.findMainAccount(member)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // AccountService를 통한 비밀번호 검증
        accountService.validateAccountPassword(mainAccount.getId(), password);
        
        log.info("머니박스 수정을 위한 비밀번호 검증 완료: memberId={}, mainAccountId={}", 
                member.getId(), mainAccount.getId());
    }
}