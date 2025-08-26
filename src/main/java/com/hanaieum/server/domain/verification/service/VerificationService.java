package com.hanaieum.server.domain.verification.service;

import com.hanaieum.server.domain.verification.dto.VerificationConfirmRequest;

public interface VerificationService {
    void sendVerificationCode(String to);

    void confirmVerificationCode(VerificationConfirmRequest request);
}
