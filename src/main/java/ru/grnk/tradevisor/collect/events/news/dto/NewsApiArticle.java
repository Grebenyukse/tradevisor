package ru.grnk.tradevisor.collect.events.news.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsApiArticle {
    private CollectNewsSource source;
    private String title;
    private String description;
    private String url;
    private String publishedAt;
}
