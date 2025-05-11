package ru.grnk.tradevisor.collect.events.news.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NewsApiResponse {
    private List<NewsApiArticle> articles;
    private int totalResults;
}
