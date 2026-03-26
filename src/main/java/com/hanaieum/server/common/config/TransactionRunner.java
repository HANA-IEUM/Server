package com.hanaieum.server.common.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionRunner {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T runInNewTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T runInTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runInNewTransaction(Runnable runnable) {
        runnable.run();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void runInTransaction(Runnable runnable) {
        runnable.run();
    }
}
