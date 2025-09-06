package com.hanaieum.server.domain.transaction.repository;

import com.hanaieum.server.domain.account.entity.Account;
import com.hanaieum.server.domain.transaction.entity.Transaction;
import com.hanaieum.server.domain.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") Long accountId, Pageable pageable);

    List<Transaction> findAllByAccountAndTransactionTypeAndCreatedAtBeforeOrderByCreatedAtAsc(
            Account account,
            TransactionType type,
            LocalDateTime targetDate
    );
}
