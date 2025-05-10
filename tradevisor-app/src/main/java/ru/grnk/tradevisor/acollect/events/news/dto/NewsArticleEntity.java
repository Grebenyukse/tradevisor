package ru.grnk.tradevisor.acollect.events.news.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class NewsArticleEntity {
    private String id;
    private String title;
    private String description;
    private String url;
    private Instant publishDate;
    private String source;

    private String ticker;
    private Integer impact;
    private String category;
    private String country;
}