package com.hanaieum.server.domain.account.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.account.entity.AccountType;
import com.hanaieum.server.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByNumber(String number);
    
    Optional<Account> findByMemberAndAccountTypeAndDeletedFalse(Member member, AccountType accountType);

    List<Account> findAllByMemberAndAccountTypeAndDeletedFalse(Member member, AccountType accountType);

    Optional<Account> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.deleted = false")
    Optional<Account> findByIdAndDeletedFalseWithLock(@Param("id") Long id);
    
    long countByMemberAndAccountTypeAndDeletedFalse(Member member, AccountType accountType);
}