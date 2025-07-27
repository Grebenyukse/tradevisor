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

     private final RestTemplate restTemplate;
     private final TrvFinamProperties finamProperties;
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
