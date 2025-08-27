package com.hanaieum.server.domain.transaction.repository;

import com.hanaieum.server.domain.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
