package ru.grnk.tradevisor.integration.bybit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceResponse {
    private BalanceResult result;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BalanceResult {
        private List<AccountInfo> list;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountInfo {
        private List<CoinInfo> coin;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoinInfo {
        private String coin;
        private String walletBalance;
        private String availableToWithdraw;
    }
}
