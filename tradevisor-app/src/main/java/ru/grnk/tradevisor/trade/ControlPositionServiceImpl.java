package ru.grnk.tradevisor.trade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.grnk.tradevisor.common.repository.SignalsRepository;
import ru.grnk.tradevisor.dbmodel.tables.pojos.Signals;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.jooq.impl.DSL.grouping;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlPositionServiceImpl implements TradeService {

    private final SignalsRepository signalsRepository;
    private final InvestApi investApi;

    @Override
    public void doWork() {

    }

    @Override
    public void loadNewSignals() {
        Map<String, List<Signals>> newSignals = signalsRepository.findPublishedSignals()
                .stream().collect(groupingBy(Signals::getInstrumentUuid));
        var expiredSignals = new ArrayList<Signals>();
        for (var s : newSignals.entrySet()) {
            if (s.getValue().size() > 1) {
                s.getValue().stream().sorted(Comparator.comparing(Signals::getCreatedAt))
                        .skip(1)
                        .forEachOrdered(expiredSignals::add);
            }
        }
        cancelExpiredSignals(expiredSignals);
    }

    @Override
    public void loadOrders() {

    }

    @Override
    public void loadPositions() {

    }

    public void cancelExpiredSignals(List<Signals> expiredSignals) {
        var expiredSignalsIds = expiredSignals.stream().map(Signals::getId).collect(toList());
        signalsRepository.cancelExpiredSignals(expiredSignalsIds);
    }

    @Override
    public void cancelExpiredOrders() {

    }

    @Override
    public void openPosition() {

    }

    @Override
    public void notifyChanges() {

    }

}
