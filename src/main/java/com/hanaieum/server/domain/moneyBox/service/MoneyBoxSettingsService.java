package com.hanaieum.server.domain.moneyBox.service;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxSettingsRequest;
import com.hanaieum.server.domain.moneyBox.dto.MoneyBoxSettingsResponse;

import java.math.BigDecimal;
import java.util.List;

public interface MoneyBoxSettingsService {

    /**
     * 버킷리스트와 연동된 머니박스 생성 (내부 서비스용)
     */
    MoneyBoxSettingsResponse createMoneyBoxForBucketList(BucketList bucketList, Member member, String boxName);

    /**
     * 머니박스 생성 (계좌 생성 + 설정 연결)
     */
    MoneyBoxSettingsResponse createMoneyBox(MoneyBoxSettingsRequest request);

    /**
     * 머니박스 설정 수정 (별명만 수정)
     */
    MoneyBoxSettingsResponse updateMoneyBoxSettings(Long settingsId, MoneyBoxSettingsRequest request);

    /**
     * 머니박스 삭제 (계좌와 설정 모두 삭제)
     */
    void deleteMoneyBoxSettings(Long settingsId);

    /**
     * 사용자의 모든 머니박스 목록 조회
     */
    List<MoneyBoxSettingsResponse> getMyMoneyBoxList();

}
