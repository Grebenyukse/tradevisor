package ru.grnk.tradevisor.collect.prices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.grnk.tradevisor.common.repository.TickersRepository;
import ru.grnk.tradevisor.common.repository.entity.Tickers;

@Component
@Slf4j
@RequiredArgsConstructor
public class PriceLoadingErrorHandler {

    private final TickersRepository tickersRepository;

    public ErrorHandlerResult handleError(Exception e, Tickers ticker, String provider) {
        if (e.getMessage().contains("Security id doesn't exist for mic") ||
            e.getMessage().contains("NOT_FOUND: 50002")
        ) {
            tickersRepository.markTickerFailedByQuotes(ticker.getTickerCode());
            log.warn("ticker {} excluded as unknown. will not be requested next time.", ticker.getTickerCode());
            return ErrorHandlerResult.SKIP_RESULT;
        }
        if (e.getMessage().contains("RESOURCE_EXHAUSTED") ||
            e.getMessage().contains("429") ||
            e.getMessage().contains("UNAUTHENTICATED: Api token could not be verified") ||
            e.getMessage().contains("Превышен лимит запросов в минуту")) {
            log.warn("RESOURCE EXHAUSTED for provider: {}", provider);
            return ErrorHandlerResult.RETRY_RESULT;
        }
        return ErrorHandlerResult.FAIL_RESULT;
    }
    
    public static class ErrorHandlerResult {
        public enum Action { SKIP, RETRY, FAIL }
        
        private final Action action;
        
        private ErrorHandlerResult(Action action) {
            this.action = action;
        }
        
        public Action getAction() {
            return action;
        }
        
        public static final ErrorHandlerResult SKIP_RESULT = new ErrorHandlerResult(Action.SKIP);
        public static final ErrorHandlerResult RETRY_RESULT = new ErrorHandlerResult(Action.RETRY);
        public static final ErrorHandlerResult FAIL_RESULT = new ErrorHandlerResult(Action.FAIL);
    }
}
