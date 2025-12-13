package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Setter
@Getter
@Entity
@Table(name = "tickers", schema = "tradevisor")
public class Tickers implements Serializable {

    @Id
    @Column(name = "ticker_code")
    private String tickerCode;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(name = "figi", nullable = false)
    private String figi;

    @Column(name = "description")
    private String description;

    @Column(name = "market_type")
    private String marketType;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "precision")
    private Integer precision;

    @Column(name = "lot")
    private Integer lot;

    @Column(name = "go")
    private Integer go;

    @Column(name = "expiration")
    private LocalDateTime expiration;

    @Column(name = "currency")
    private String currency;

    @Column(name = "provider")
    private String provider;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "load_priority")
    private Integer loadPriority;

    @Column(name = "trade_ticker_code")
    private String tradeTickerCode;

    // Getters and Setters

}
