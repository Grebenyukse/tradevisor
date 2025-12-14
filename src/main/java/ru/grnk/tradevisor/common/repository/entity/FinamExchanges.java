package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Builder
@Setter
@Getter
@Entity
@Table(name = "finam_exchanges", schema = "tradevisor")
@NoArgsConstructor
@AllArgsConstructor
public class FinamExchanges implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "mic", length = 100)
    private String mic;

    @Column(name = "is_active")
    private Boolean isActive;

    // Getters and Setters

}
