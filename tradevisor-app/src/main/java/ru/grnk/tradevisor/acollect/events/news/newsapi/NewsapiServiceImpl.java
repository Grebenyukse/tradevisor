package ru.grnk.tradevisor.acollect.events.news.newsapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.grnk.tradevisor.acollect.events.news.NewsCollector;
import ru.grnk.tradevisor.acollect.events.news.dto.NewsApiArticle;
import ru.grnk.tradevisor.acollect.events.news.dto.NewsApiResponse;
import ru.grnk.tradevisor.dbmodel.tables.pojos.News;
import ru.grnk.tradevisor.zcommon.properties.TradevisorProperties;
import ru.grnk.tradevisor.zcommon.repository.NewsRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static ru.grnk.tradevisor.zcommon.util.ObjectIdHasher.calcHash;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsapiServiceImpl implements NewsCollector {
    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate;
    private final TradevisorProperties trvProperties;

    @Override
    public List<NewsApiResponse> collect() {
       collectEconomicNews();
       return List.of();
    }

    @Override
    public String source() {
        return "newsapi";
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
                    .forEach(newsRepository::saveNews);
            if (totalResults > Math.max(page, 1) * pageSize) {
                collectEconomicNews(page + 1);
            }
        } catch (Exception e) {
            log.error("ошибка получения новостей",e);
        }
    }

    private News convertToNewsArticle(NewsApiArticle apiArticle) {
        return new News(
                calcHash(apiArticle.getTitle(), apiArticle.getDescription(), apiArticle.getPublishedAt()),
                apiArticle.getSource().getName(),
                apiArticle.getTitle(),
                apiArticle.getDescription(),
                apiArticle.getUrl(),
                OffsetDateTime.parse(apiArticle.getPublishedAt())
        );
    }
}
