package ru.grnk.tradevisor.common.repository.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Setter
@Getter
@Entity
@Table(name = "events", schema = "tradevisor")
public class Events {

    @Id
    @Column(name = "hash_id")
    private String hashId;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "category")
    private String category;

    @Column(name = "source")
    private String source;

    @Column(name = "impact")
    private String impact;

    @Column(name = "instrument_uuid")
    private String instrumentUuid;

    @Column(name = "content")
    private String content;

}
