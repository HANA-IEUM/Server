package com.hanaieum.server.domain.account.repository;

import com.hanaieum.server.domain.account.entity.AccountBalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBalanceSnapshotRepository extends JpaRepository<AccountBalanceSnapshot, Long> {

}