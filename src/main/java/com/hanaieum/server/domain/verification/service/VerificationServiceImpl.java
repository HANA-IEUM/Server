package com.hanaieum.server.domain.verification.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hanaieum.server.common.config.SolapiConfig;
import com.hanaieum.server.common.exception.CustomException;
import com.hanaieum.server.common.exception.ErrorCode;
import com.hanaieum.server.domain.verification.dto.VerificationConfirmRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    private final SolapiConfig solapiConfig;
    private DefaultMessageService messageService;

    private final Cache<String, String> verificationCodes = CacheBuilder.newBuilder()
            .build();

    @PostConstruct
    private void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(
                solapiConfig.getApiKey(),
                solapiConfig.getApiSecret(),
                "https://api.solapi.com"
        );
    }

    @Override
    public void sendVerificationCode(String to) {
        String verificationCode = generateRandomCode();

        Message message = new Message();
        message.setFrom(solapiConfig.getSender());
        message.setTo(to);
        message.setText("[Hana-Ieum] 인증번호: " + verificationCode);

        try {
            messageService.sendOne(new SingleMessageSendingRequest(message));
            verificationCodes.put(to, verificationCode);
            log.info("인증 코드 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("메시지 발송 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.MESSAGE_SEND_FAILURE);
        }
    }

    @Override
    public void confirmVerificationCode(VerificationConfirmRequest request) {
        String storedCode = verificationCodes.getIfPresent(request.to());

        if (storedCode == null) {
            throw new CustomException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(request.verificationCode())) {
            throw new CustomException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        verificationCodes.invalidate(request.to());
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }
}
