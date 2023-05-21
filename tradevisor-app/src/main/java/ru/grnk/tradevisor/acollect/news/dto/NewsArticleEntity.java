package ru.grnk.tradevisor.acollect.news.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class NewsArticleEntity {
    private UUID id;
    private String title;
    private String description;
    private String url;
    private Instant publishDate;
    private String source;
}
