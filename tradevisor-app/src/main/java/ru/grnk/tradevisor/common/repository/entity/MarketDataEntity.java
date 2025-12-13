package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Builder
@Setter
@Getter
@Entity
@Table(name = "market_data", schema = "tradevisor")
@IdClass(MarketDataEntity.CompositeId.class)
public class MarketDataEntity implements Serializable {

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

    // Composite ID Class
    public static class CompositeId implements Serializable {
        private String tickerCode;
        private OffsetDateTime time;

        // Default constructor, equals, hashCode required for composite key
        public CompositeId() {}
        public CompositeId(String tickerCode, OffsetDateTime time) {
            this.tickerCode = tickerCode;
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CompositeId)) return false;
            CompositeId that = (CompositeId) o;
            return tickerCode.equals(that.tickerCode) && time.equals(that.time);
        }

        @Override
        public int hashCode() {
            return tickerCode.hashCode() ^ time.hashCode();
        }
    }

    // Getters and Setters

}
