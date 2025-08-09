package ru.grnk.tradevisor.integration.finam;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.grnk.tradevisor.common.properties.TrvFinamProperties;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GET_TOKEN_URI = "/sessions";
    private static final String GET_TOKEN_DETAILS_URI = "/sessions/details";
    private static final String GET_ACCOUNT_URI = "/accounts/{account_id}";
    private static final String GET_TRADES_URI = "/accounts/{account_id}/trades";

     private final RestTemplate restTemplate;
     private final TrvFinamProperties finamProperties;
     private final AtomicReference<String> token  = new AtomicReference<>();
     private final AtomicReference<String> accountId  = new AtomicReference<>();

//    private String getFinamToken() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<FinamTokenRq> request = new HttpEntity<>(
//                new FinamTokenRq(finamProperties.secret()), headers);
//        var res = restTemplate.postForEntity(finamProperties.url() + GET_TOKEN_URI, request, FinamTokenRs.class);
//        var tokenValue = Objects.requireNonNull(res.getBody()).token();
//        token.set(tokenValue);
//        return tokenValue;
//    }

//    public String getTokenDetails() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<FinamTokenDetailsRq> request = new HttpEntity<>(
//                new FinamTokenDetailsRq(token.get()), headers);
//        var res = restTemplate.postForEntity(finamProperties.url() + GET_TOKEN_DETAILS_URI, request, FinamTokenDetailsRs.class);
//        var accountIdValue = Objects.requireNonNull(res.getBody()).account_ids().stream().findFirst().orElseThrow();
//        accountId.set(accountIdValue);
//        return accountIdValue;
//    }

//    public FinamGetAccountRs getAccount() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", token.get());
//        var res = restTemplate.getForEntity(
//                finamProperties.url() + GET_ACCOUNT_URI.replace("{account_id}", accountId.get()),
//                FinamGetAccountRs.class,
//                headers
//                );
//        return res.getBody();
//    }

//    public FinamGetTradesRs() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", token.get());
//        var res = restTemplate.getForEntity(
//                finamProperties.url() + GET_TRADES_URI.replace("{account_id}", accountId.get()),
//                FinamGetTradesRs.class,
//                headers
//        );
//        return res.body();
//    }
     private AtomicReference<String> token  = new AtomicReference<>();

//    private String getFinamToken() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<TrvFinamGetTokenRqDTO> request = new HttpEntity<>(
//                new TrvFinamGetTokenRqDTO(finamProperties.secret()), headers);
//        var res = restTemplate.postForEntity(finamProperties.url() + GET_TOKEN_URI, request, TrvFinamGetTokenRespDTO.class);
//        var tokenValue = Objects.requireNonNull(res.getBody()).token();
//        token.set(tokenValue);
//        return tokenValue;
//    }
//
//    public String getTokenDetails() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<TrvFinamGetTokenRqDTO> request = new HttpEntity<>(
//                new TrvFinamGetTokenRqDTO(finamProperties.secret()), headers);
//        var res = restTemplate.postForEntity(finamProperties.url() + GET_TOKEN_URI, request, TrvFinamGetTokenRespDTO.class);
//        var tokenValue = Objects.requireNonNull(res.getBody()).token();
//        token.set(tokenValue);
//        return tokenValue;
//    }
}
