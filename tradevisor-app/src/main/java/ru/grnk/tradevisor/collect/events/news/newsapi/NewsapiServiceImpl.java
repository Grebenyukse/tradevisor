package ru.grnk.tradevisor.collect.events.news.newsapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.grnk.tradevisor.collect.TrvSource;
import ru.grnk.tradevisor.collect.events.EventsRepository;
import ru.grnk.tradevisor.collect.events.dto.EventCategory;
import ru.grnk.tradevisor.collect.events.dto.TickerEvent;
import ru.grnk.tradevisor.collect.events.news.NewsCollector;
import ru.grnk.tradevisor.collect.events.news.dto.NewsApiArticle;
import ru.grnk.tradevisor.collect.events.news.dto.NewsApiResponse;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;

import java.time.OffsetDateTime;
import java.util.List;

import static ru.grnk.tradevisor.common.util.ObjectIdHasher.calcHash;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsapiServiceImpl implements NewsCollector {
    private final EventsRepository eventsRepository;
    private final RestTemplate restTemplate;
    private final TradevisorProperties trvProperties;

    @Override
    public List<NewsApiResponse> collect() {
       collectEconomicNews();
       return List.of();
    }

    public void collectEconomicNews() {
       collectEconomicNews(1);
    }
    public void collectEconomicNews(int page) {
        var apiKey = trvProperties.integration().newsapi().apiKey();
        var pageSize = trvProperties.integration().newsapi().pageSize();
        var baseUrl = trvProperties.integration().newsapi().url();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("apiKey", apiKey)
                    .queryParam("page", page)
                    .queryParam("pageSize", pageSize);
            String url = builder.toUriString();
            ResponseEntity<NewsApiResponse> response = restTemplate.getForEntity(url, NewsApiResponse.class);
            int totalResults = response.getBody().getTotalResults();
            if (response.getBody() == null) return;
            response.getBody().getArticles().stream()
                    .map(this::convertToNewsArticle)
                    .forEach(eventsRepository::saveEvent);
            if (totalResults > Math.max(page, 1) * pageSize) {
                collectEconomicNews(page + 1);
            }
        } catch (Exception e) {
            log.error("ошибка получения новостей",e);
        }
    }

    private TickerEvent convertToNewsArticle(NewsApiArticle apiArticle) {
        return TickerEvent.builder()
                .id(calcHash(apiArticle.getTitle(), apiArticle.getDescription(), apiArticle.getPublishedAt()))
                .source(TrvSource.NEWSAPI)
                .category(EventCategory.NEWS)
                .impact(null)
                .content(apiArticle.getTitle() + " " + apiArticle.getDescription() + " " + apiArticle.getUrl())
                .eventDate(OffsetDateTime.parse(apiArticle.getPublishedAt()))
                .tickerUid(null)
                .build();
    }
}
