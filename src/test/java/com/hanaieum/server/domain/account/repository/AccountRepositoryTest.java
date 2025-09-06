package com.hanaieum.server.domain.account.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.member.entity.Gender;
import com.hanaieum.server.domain.member.entity.Member;
import com.hanaieum.server.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@DisplayName("AccountRepository 테스트")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("계좌번호 중복 확인")
    void existsByNumber() {
        // Given
        Member member = createAndSaveMember();
        Account account = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);

        // When
        boolean exists = accountRepository.existsByNumber("12345678901234");
        boolean notExists = accountRepository.existsByNumber("99999999999999");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("회원과 계좌타입으로 계좌 조회 - 삭제된 계좌는 조회되지 않음")
    void findMainAccountTypeAndDeletedFalse_IgnoreDeletedAccounts() {
        // Given
        Member member = createAndSaveMember();
        Account account = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        account.setDeleted(true);
        accountRepository.save(account);

        // When
        Optional<Account> foundAccount = accountRepository
                .findByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN);

        // Then
        assertThat(foundAccount).isEmpty();
    }

    @Test
    @DisplayName("회원과 계좌타입으로 모든 계좌 조회")
    void findAllByMemberAndAccountTypeAndDeletedFalse() {
        // Given
        Member member = createAndSaveMember();
        Account moneyBox1 = createAndSaveAccount(member, "12345678901", AccountType.MONEY_BOX);
        Account moneyBox2 = createAndSaveAccount(member, "12345678902", AccountType.MONEY_BOX);
        Account moneyBox3 = createAndSaveAccount(member, "12345678903", AccountType.MONEY_BOX);
        
        // 삭제된 머니박스 추가
        Account deletedMoneyBox = createAndSaveAccount(member, "12345678904", AccountType.MONEY_BOX);
        deletedMoneyBox.setDeleted(true);
        accountRepository.save(deletedMoneyBox);

        // When
        List<Account> moneyBoxes = accountRepository
                .findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);

        // Then
        assertThat(moneyBoxes).hasSize(3);
        assertThat(moneyBoxes).extracting(Account::getId)
                .containsExactlyInAnyOrder(moneyBox1.getId(), moneyBox2.getId(), moneyBox3.getId());
        assertThat(moneyBoxes).noneMatch(Account::isDeleted);
    }

    @Test
    @DisplayName("ID로 계좌 조회 - 삭제되지 않은 계좌만")
    void findByIdAndDeletedFalse() {
        // Given
        Member member = createAndSaveMember();
        Account activeAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        Account deletedAccount = createAndSaveAccount(member, "12345678901235", AccountType.MAIN);
        deletedAccount.setDeleted(true);
        accountRepository.save(deletedAccount);

        // When
        Optional<Account> foundActiveAccount = accountRepository.findByIdAndDeletedFalse(activeAccount.getId());
        Optional<Account> foundDeletedAccount = accountRepository.findByIdAndDeletedFalse(deletedAccount.getId());

        // Then
        assertThat(foundActiveAccount).isPresent();
        assertThat(foundActiveAccount.get().getId()).isEqualTo(activeAccount.getId());
        assertThat(foundActiveAccount.get().isDeleted()).isFalse();

        assertThat(foundDeletedAccount).isEmpty();
    }

    @Test
    @DisplayName("Pessimistic Lock으로 계좌 조회")
    void findByIdAndDeletedFalseWithLock() {
        // Given
        Member member = createAndSaveMember();
        Account account = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);

        // When
        Optional<Account> foundAccount = accountRepository.findByIdAndDeletedFalseWithLock(account.getId());

        // Then
        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getId()).isEqualTo(account.getId());
        assertThat(foundAccount.get().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("회원의 특정 타입 계좌 개수 조회")
    void countByMemberAndAccountTypeAndDeletedFalse() {
        // Given
        Member member = createAndSaveMember();
        createAndSaveAccount(member, "12345678901", AccountType.MONEY_BOX);
        createAndSaveAccount(member, "12345678902", AccountType.MONEY_BOX);
        createAndSaveAccount(member, "12345678903", AccountType.MONEY_BOX);
        
        // 삭제된 머니박스
        Account deletedMoneyBox = createAndSaveAccount(member, "12345678904", AccountType.MONEY_BOX);
        deletedMoneyBox.setDeleted(true);
        accountRepository.save(deletedMoneyBox);
        
        // 주계좌
        createAndSaveAccount(member, "12345678901234", AccountType.MAIN);

        // When
        long moneyBoxCount = accountRepository.countByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
        long mainAccountCount = accountRepository.countByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN);

        // Then
        assertThat(moneyBoxCount).isEqualTo(3L); // 삭제된 것 제외
        assertThat(mainAccountCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("다른 회원의 계좌는 조회되지 않음")
    void findByMemberAndAccountType_DifferentMember() {
        // Given
        Member member1 = createAndSaveMember("01012345678", "회원1");
        Member member2 = createAndSaveMember("01087654321", "회원2");
        
        Account member1Account = createAndSaveAccount(member1, "12345678901234", AccountType.MAIN);
        Account member2Account = createAndSaveAccount(member2, "12345678901235", AccountType.MAIN);

        // When
        Optional<Account> foundForMember1 = accountRepository
                .findByMemberAndAccountTypeAndDeletedFalse(member1, AccountType.MAIN);
        Optional<Account> foundForMember2 = accountRepository
                .findByMemberAndAccountTypeAndDeletedFalse(member2, AccountType.MAIN);

        // Then
        assertThat(foundForMember1).isPresent();
        assertThat(foundForMember1.get().getId()).isEqualTo(member1Account.getId());
        assertThat(foundForMember1.get().getMember().getId()).isEqualTo(member1.getId());

        assertThat(foundForMember2).isPresent();
        assertThat(foundForMember2.get().getId()).isEqualTo(member2Account.getId());
        assertThat(foundForMember2.get().getMember().getId()).isEqualTo(member2.getId());
    }

    @Test
    @DisplayName("주계좌 도메인 테스트 - 회원당 하나만 존재")
    void mainAccountDomainRules() {
        // Given
        Member member = createAndSaveMember();
        
        // When - 주계좌 생성
        Account mainAccount = createAndSaveAccount(member, "12345678901234", AccountType.MAIN);
        
        // Then - 주계좌는 하나만 조회됨
        Optional<Account> foundAccount = accountRepository
                .findByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MAIN);
        
        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getId()).isEqualTo(mainAccount.getId());
        assertThat(foundAccount.get().getAccountType()).isEqualTo(AccountType.MAIN);
        assertThat(foundAccount.get().getNumber()).hasSize(14); // 주계좌는 14자리
        assertThat(foundAccount.get().getBoxName()).isNull(); // 주계좌는 boxName 없음
    }
    
    @Test
    @DisplayName("머니박스 도메인 테스트 - 회원당 여러개 가능")
    void moneyBoxDomainRules() {
        // Given
        Member member = createAndSaveMember();
        
        // When - 여러 머니박스 생성
        Account moneyBox1 = createAndSaveMoneyBox(member, "11111111111", "여행 머니박스");
        Account moneyBox2 = createAndSaveMoneyBox(member, "22222222222", "쇼핑 머니박스");
        Account moneyBox3 = createAndSaveMoneyBox(member, "33333333333", "생활비 머니박스");
        
        // Then - 머니박스는 여러개 조회됨
        List<Account> moneyBoxes = accountRepository
                .findAllByMemberAndAccountTypeAndDeletedFalse(member, AccountType.MONEY_BOX);
        
        assertThat(moneyBoxes).hasSize(3);
        assertThat(moneyBoxes).allMatch(account -> account.getAccountType() == AccountType.MONEY_BOX);
        assertThat(moneyBoxes).allMatch(account -> account.getNumber().length() == 11); // 머니박스는 11자리
        assertThat(moneyBoxes).allMatch(account -> account.getBoxName() != null); // 머니박스는 boxName 필수
        assertThat(moneyBoxes).extracting(Account::getBoxName)
                .containsExactlyInAnyOrder("여행 머니박스", "쇼핑 머니박스", "생활비 머니박스");
    }
    

    // Helper methods
    private Member createAndSaveMember() {
        return createAndSaveMember("01012345678", "테스트유저");
    }

    private Member createAndSaveMember(String phoneNumber, String name) {
        Member member = Member.builder()
                .phoneNumber(phoneNumber)
                .name(name)
                .password("encoded_password")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.M)
                .monthlyLivingCost(1000000)
                .mainAccountLinked(true)
                .hideGroupPrompt(false)
                .build();
        return memberRepository.save(member);
    }

    private Account createAndSaveAccount(Member member, String accountNumber, AccountType accountType) {
        Account account = Account.builder()
                .member(member)
                .number(accountNumber)
                .name(accountType == AccountType.MAIN ? "주거래하나 통장" : "하나머니박스")
                .bankName("하나은행")
                .password("encoded_password")
                .balance(accountType == AccountType.MAIN ? new BigDecimal("70000000") : BigDecimal.ZERO)
                .accountType(accountType)
                .boxName(accountType == AccountType.MONEY_BOX ? "테스트 머니박스" : null)
                .deleted(false)
                .build();
        return accountRepository.save(account);
    }

    private Account createAndSaveMoneyBox(Member member, String accountNumber, String boxName) {
        Account account = Account.builder()
                .member(member)
                .number(accountNumber)
                .name("하나머니박스")
                .bankName("하나은행")
                .password("encoded_password")
                .balance(BigDecimal.ZERO)
                .accountType(AccountType.MONEY_BOX)
                .boxName(boxName)
                .deleted(false)
                .build();
        return accountRepository.save(account);
    }
}