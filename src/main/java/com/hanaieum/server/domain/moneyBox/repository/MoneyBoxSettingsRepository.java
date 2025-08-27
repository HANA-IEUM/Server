package com.hanaieum.server.domain.moneyBox.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.moneyBox.entity.MoneyBoxSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoneyBoxSettingsRepository extends JpaRepository<MoneyBoxSettings, Long> {
    
    // 계좌별 머니박스 설정 조회
    Optional<MoneyBoxSettings> findByAccountAndDeletedFalse(Account account);
    
    // 버킷리스트별 머니박스 설정 조회
    Optional<MoneyBoxSettings> findByBucketListAndDeletedFalse(BucketList bucketList);
    
    // 특정 멤버의 모든 머니박스 설정 조회
    @Query("SELECT mbs FROM MoneyBoxSettings mbs WHERE mbs.account.member = :member AND mbs.deleted = false")
    List<MoneyBoxSettings> findByMemberAndDeletedFalse(@Param("member") Member member);
    
    // 삭제되지 않은 모든 머니박스 설정 조회
    List<MoneyBoxSettings> findByDeletedFalse();
    
    // ID로 삭제되지 않은 머니박스 설정 조회
    Optional<MoneyBoxSettings> findByIdAndDeletedFalse(Long id);
}
