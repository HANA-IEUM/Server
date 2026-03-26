package com.hanaieum.server.common;

import com.hanaieum.server.common.config.TransactionRunner;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRunnerIntegrationTest {

    @Autowired
    private TransactionRunner transactionRunner;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("runInNewTransaction은 독립적인 트랜잭션을 보장해야 한다")
    void runInNewTransaction_ShouldIsolateTransactions() {
        // Given: 두 개의 태스크 정의
        String phone1 = "01011112222";
        String phone2 = "01033334444";

        // When: 첫 번째 태스크는 성공, 두 번째 태스크는 실패(예외 발생)
        transactionRunner.runInNewTransaction(() -> {
            Member member1 = Member.builder()
                    .phoneNumber(phone1)
                    .name("성공멤버")
                    .password("1234")
                    .birthDate(LocalDate.now())
                    .gender(Gender.M)
                    .active(true)
                    .deleted(false)
                    .monthlyLivingCost(10000)
                    .build();
            memberRepository.save(member1);
        });

        assertThrows(RuntimeException.class, () -> {
            transactionRunner.runInNewTransaction(() -> {
                Member member2 = Member.builder()
                        .phoneNumber(phone2)
                        .name("실패멤버")
                        .password("1234")
                        .birthDate(LocalDate.now())
                        .gender(Gender.F)
                        .active(true)
                        .deleted(false)
                        .monthlyLivingCost(10000)
                        .build();
                memberRepository.save(member2);
                throw new RuntimeException("강제 예외 발생");
            });
        });

        // Then: 성공한 멤버는 저장되어야 하고, 실패한 멤버는 롤백되어 없어야 함
        assertThat(memberRepository.findByPhoneNumber(phone1)).isPresent();
        assertThat(memberRepository.findByPhoneNumber(phone2)).isEmpty();
    }
}
