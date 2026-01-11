package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;

@Builder
@Setter
@Getter
@Entity
@Table(name = "tickers", schema = "tradevisor")
@NoArgsConstructor
@AllArgsConstructor
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

    @Override
    public String toString() {
        return "Tickers{" +
                "tickerCode='" + tickerCode + '\'' +
                ", ticker='" + ticker + '\'' +
                ", exchange='" + exchange + '\'' +
                ", spotTickerCode='" + spotTickerCode + '\'' +
                ", description='" + description + '\'' +
                ", currency='" + currency + '\'' +
                ", provider='" + provider + '\'' +
                ", status='" + status + '\'' +
                ", version=" + version +
                '}';
    }
}
