package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "market_data", schema = "tradevisor")
@IdClass(MarketData.CompositeId.class)
@NoArgsConstructor
@AllArgsConstructor
public class MarketData implements Serializable {

    @Id
    @Column(name = "ticker_code")
    private String tickerCode;

    @Id
    @Column(name = "time")
    private OffsetDateTime time;

    @Column(name = "open")
    private Float open;

    @Column(name = "high")
    private Float high;

    @Column(name = "low")
    private Float low;

    @Column(name = "close")
    private Float close;

    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class CompositeId implements Serializable {
        private String tickerCode;
        private OffsetDateTime time;
    }
}
