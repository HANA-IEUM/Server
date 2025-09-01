package com.hanaieum.server.domain.bucketList.service;

import com.hanaieum.server.domain.bucketList.entity.BucketList;
import com.hanaieum.server.domain.bucketList.entity.BucketListStatus;
import com.hanaieum.server.domain.bucketList.repository.BucketListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BucketListSchedulerService {
    
    private final BucketListRepository bucketListRepository;
    
    /**
     * 매일 오전 9시에 목표날짜가 지난 버킷리스트를 완료 상태로 변경
     * 매일 08:00에 실행
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void updateExpiredBucketListsToCompleted() {
        long startTime = System.currentTimeMillis();
        log.info("=== 버킷리스트 만료 상태 업데이트 스케줄러 시작 ===");
        log.info("실행 시간: {}", LocalDate.now());
        
        try {
            LocalDate today = LocalDate.now();
            
            // 목표날짜가 오늘 이전이고, 아직 진행중인 버킷리스트들 조회
            List<BucketList> expiredBucketLists = bucketListRepository.findExpiredInProgressBucketLists(today);
            
            log.info("조회된 만료 대상 버킷리스트 수: {}", expiredBucketLists.size());
            
            if (expiredBucketLists.isEmpty()) {
                log.info("만료된 진행중 버킷리스트가 없습니다.");
                return;
            }
            
            int updatedCount = 0;
            for (BucketList bucketList : expiredBucketLists) {
                BucketListStatus previousStatus = bucketList.getStatus();
                bucketList.setStatus(BucketListStatus.COMPLETED);
                updatedCount++;
                
                log.info("버킷리스트 상태 변경: ID={}, 제목='{}', 목표날짜={}, {} -> {}", 
                    bucketList.getId(), bucketList.getTitle(), bucketList.getTargetDate(),
                    previousStatus, BucketListStatus.COMPLETED);
            }
            
            bucketListRepository.saveAll(expiredBucketLists);
            
            long endTime = System.currentTimeMillis();
            log.info("=== 스케줄러 실행 완료 ===");
            log.info("업데이트된 버킷리스트 수: {}", updatedCount);
            log.info("실행 시간: {}ms", (endTime - startTime));
            
        } catch (Exception e) {
            log.error("스케줄러 실행 중 오류 발생", e);
            throw e;
        }
    }
    
    /**
     * 수동으로 만료된 버킷리스트 상태 업데이트 (API 호출 시점에 사용)
     */
    @Transactional
    public void updateExpiredBucketListsForMember(Long memberId) {
        log.debug("특정 회원의 만료된 버킷리스트 상태 업데이트: memberId={}", memberId);
        
        LocalDate today = LocalDate.now();
        
        List<BucketList> expiredBucketLists = bucketListRepository.findExpiredInProgressBucketListsByMember(memberId, today);
        
        if (expiredBucketLists.isEmpty()) {
            return;
        }
        
        for (BucketList bucketList : expiredBucketLists) {
            bucketList.setStatus(BucketListStatus.COMPLETED);
        }
        
        bucketListRepository.saveAll(expiredBucketLists);
        
        log.debug("회원의 만료된 버킷리스트 상태 업데이트 완료: memberId={}, 업데이트 수={}", 
            memberId, expiredBucketLists.size());
    }
    
    /**
     * 테스트용 수동 실행 메서드
     */
    @Transactional
    public String runSchedulerManually() {
        log.info("=== 스케줄러 수동 실행 요청 ===");
        
        long startTime = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        
        // 만료된 버킷리스트 조회
        List<BucketList> expiredBucketLists = bucketListRepository.findExpiredInProgressBucketLists(today);
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("실행 시간: %s\n", today));
        result.append(String.format("조회된 만료 대상 버킷리스트 수: %d\n", expiredBucketLists.size()));
        
        if (expiredBucketLists.isEmpty()) {
            result.append("만료된 진행중 버킷리스트가 없습니다.\n");
            return result.toString();
        }
        
        int updatedCount = 0;
        for (BucketList bucketList : expiredBucketLists) {
            BucketListStatus previousStatus = bucketList.getStatus();
            bucketList.setStatus(BucketListStatus.COMPLETED);
            updatedCount++;
            
            String updateInfo = String.format("ID=%d, 제목='%s', 목표날짜=%s, %s -> %s\n",
                bucketList.getId(), bucketList.getTitle(), bucketList.getTargetDate(),
                previousStatus, BucketListStatus.COMPLETED);
            result.append(updateInfo);
            
            log.info("버킷리스트 상태 변경: {}", updateInfo.trim());
        }
        
        bucketListRepository.saveAll(expiredBucketLists);
        
        long endTime = System.currentTimeMillis();
        result.append(String.format("업데이트된 버킷리스트 수: %d\n", updatedCount));
        result.append(String.format("실행 시간: %dms\n", (endTime - startTime)));
        
        log.info("수동 스케줄러 실행 완료: {}개 업데이트", updatedCount);
        
        return result.toString();
    }
    
    /**
     * 스케줄러 상태 확인 메서드
     */
    public String getSchedulerStatus() {
        LocalDate today = LocalDate.now();
        
        // 만료 대상 버킷리스트 수 조회
        List<BucketList> expiredBucketLists = bucketListRepository.findExpiredInProgressBucketLists(today);
        
        StringBuilder status = new StringBuilder();
        status.append(String.format("현재 시간: %s\n", today));
        status.append(String.format("만료 대상 버킷리스트 수: %d\n", expiredBucketLists.size()));
        
        if (!expiredBucketLists.isEmpty()) {
            status.append("\n만료 대상 목록:\n");
            for (BucketList bucketList : expiredBucketLists) {
                status.append(String.format("- ID=%d, 제목='%s', 목표날짜=%s, 상태=%s\n",
                    bucketList.getId(), bucketList.getTitle(), 
                    bucketList.getTargetDate(), bucketList.getStatus()));
            }
        }
        
        return status.toString();
    }
}
