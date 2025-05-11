package ru.grnk.tradevisor.trade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ControlPositionServiceImpl  {

    public void doWork() {
        log.info("control all positions mf");
    }

}
