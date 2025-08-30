package com.hanaieum.server.domain.account.dto;

import com.hanaieum.server.domain.account.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainAccountResponse {
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private String bankName;
    private BigDecimal balance;
    private String accountType;
    private boolean mainAccountLinked;
    
    public static MainAccountResponse of(Account account, boolean mainAccountLinked) {
        return MainAccountResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getNumber())
                .accountName(account.getName())
                .bankName(account.getBankName())
                .balance(account.getBalance())
                .accountType(account.getAccountType().name())
                .mainAccountLinked(mainAccountLinked)
                .build();
    }
}