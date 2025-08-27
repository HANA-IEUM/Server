package com.hanaieum.server.domain.account.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByNumber(String number);
    
    Optional<Account> findByMemberAndAccountTypeAndDeletedFalse(Member member, AccountType accountType);
}