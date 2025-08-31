package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxUpdateRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxUpdateResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxResponse;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxInfoResponse;

import java.util.List;

public interface MoneyBoxService {

    /**
     * 머니박스 정보 수정 (별명 + 자동이체 설정)
     */
    MoneyBoxUpdateResponse updateMoneyBox(Member member, Long accountId, MoneyBoxUpdateRequest request);

    /**
     * 머니박스 수정 폼 데이터 조회
     */
    MoneyBoxUpdateResponse getMoneyBoxForEdit(Member member, Long boxId);

    /**
     * 사용자의 모든 머니박스 목록 조회 (MONEY_BOX 타입 계좌들)
     */
    List<MoneyBoxResponse> getMyMoneyBoxList(Member member);

    /**
     * 머니박스 정보 조회 (자동이체 정보 포함)
     */
    MoneyBoxInfoResponse getMoneyBoxInfo(Member member, Long boxId);

}