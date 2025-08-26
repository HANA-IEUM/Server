package com.hanaieum.server.domain.account.dto;

import com.hanaieum.server.domain.account.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainAccountResponse {
    private String accountNumber;
    private String accountName;
    private String bankName;
    private Long balance;
    private String accountType;
    
    public static MainAccountResponse of(Account account) {
        return MainAccountResponse.builder()
                .accountNumber(account.getNumber())
                .accountName(account.getName())
                .bankName(account.getBankName())
                .balance(account.getBalance())
                .accountType(account.getAccountType().name())
                .build();
    }
}