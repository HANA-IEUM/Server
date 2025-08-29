package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;

import java.util.List;

public interface MoneyBoxService {

    /**
     * 머니박스 별명 수정 (Account ID 기준)
     */
    MoneyBoxResponse updateMoneyBoxName(Long accountId, MoneyBoxRequest request);

    /**
     * 사용자의 모든 머니박스 목록 조회 (MONEY_BOX 타입 계좌들)
     */
    List<MoneyBoxResponse> getMyMoneyBoxList();

}