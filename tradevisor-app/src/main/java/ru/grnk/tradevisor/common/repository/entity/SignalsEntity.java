package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.grnk.tradevisor.notify.plot.dto.ChartLineDto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Setter
@Getter
@Entity
@Table(name = "signals", schema = "tradevisor")
public class SignalsEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "ticker_code")
    private String tickerCode;

    @Column(name = "direction")
    private Short direction;

    @Column(name = "price_open")
    private Float priceOpen;

    @Column(name = "stop_loss")
    private Float stopLoss;

    @Column(name = "take_profit")
    private Float takeProfit;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "strategy_props", columnDefinition = "jsonb")
    @Convert(converter = ChartLineListToJsonbConverter.class)
    private List<ChartLineDto> strategyProps;

    // Getters and Setters

}
