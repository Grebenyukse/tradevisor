package ru.grnk.tradevisor.integration.rts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class RtsClient {

    private final RestTemplate restTemplate;

    // реализация рест апи вызовов для взаимодействия с биржей РТС по baseurl = https://iss.moex.com/iss/

}
