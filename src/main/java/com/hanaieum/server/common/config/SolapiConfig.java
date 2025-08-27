package com.hanaieum.server.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SolapiConfig {
    @Value("${solapi.api-key:}")
    private String apiKey;
    
    @Value("${solapi.api-secret:}")
    private String apiSecret;
    
    @Value("${solapi.sender:}")
    private String sender;
}
