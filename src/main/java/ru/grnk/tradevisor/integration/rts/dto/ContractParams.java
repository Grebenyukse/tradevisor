package ru.grnk.tradevisor.integration.rts.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ContractParams {
    private String ticker;
    private BigDecimal tickSize;      // Шаг цены (MINSTEP)
    private BigDecimal tickValue;     // Стоимость шага цены (STEPPRICE)
    private Integer lotSize;          // Размер лота (LOTSIZE)
    private BigDecimal initialMargin; // Гарантийное обеспечение

    public static ContractParams empty(String ticker) {
        return ContractParams.builder()
                .ticker(ticker)
                .tickSize(BigDecimal.ZERO)
                .tickValue(BigDecimal.ZERO)
                .lotSize(1)
                .initialMargin(BigDecimal.ZERO)
                .build();
    }

    /**
     * Полная стоимость шага цены с учетом размера лота
     */
    public BigDecimal getFullTickValue() {
        return tickValue.multiply(BigDecimal.valueOf(lotSize));
    }

    /**
     * Форматированный вывод параметров
     */
    public String format() {
        return String.format(
                "Контракт: %s%n" +
                        "Шаг цены: %s%n" +
                        "Стоимость шага цены: %s руб.%n" +
                        "Размер лота: %d%n" +
                        "Полная стоимость шага: %s руб.%n" +
                        "ГО: %s руб.",
                ticker, tickSize, tickValue, lotSize, getFullTickValue(), initialMargin
        );
    }
}
