package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Builder
@Setter
@Getter
@Entity
@Table(name = "tickers", schema = "tradevisor")
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Tickers implements Serializable {

    @Id
    @Column(name = "ticker_code")
    private String tickerCode;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "spot_ticker_code")
    private String spotTickerCode;

    @Column(name = "description")
    private String description;

    @Column(name = "currency")
    private String currency;

    @Column(name = "provider")
    private String provider;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "version")
    private Integer version;
}
