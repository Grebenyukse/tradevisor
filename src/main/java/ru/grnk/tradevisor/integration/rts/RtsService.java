package ru.grnk.tradevisor.integration.rts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RtsService {

    private final RtsClient rtsClient;

    public Float getGoForFutures(String ticker) {
        // логика получения размера гарантийного обеспечения для тикера на бирже ртс
        return 1.0f;
    }
}
