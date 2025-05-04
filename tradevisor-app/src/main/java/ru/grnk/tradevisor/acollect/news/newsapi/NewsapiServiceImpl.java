package ru.grnk.tradevisor.acollect.news.newsapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.grnk.tradevisor.acollect.news.NewsCollector;
import ru.grnk.tradevisor.acollect.news.dto.NewsApiArticle;
import ru.grnk.tradevisor.acollect.news.dto.NewsApiResponse;
import ru.grnk.tradevisor.dbmodel.tables.pojos.News;
import ru.grnk.tradevisor.zcommon.repository.NewsRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static ru.grnk.tradevisor.zcommon.util.ObjectIdHasher.calcHash;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsapiServiceImpl implements NewsCollector {
    @Value("${collect.news.newsapi.api-key}")
    private String apiKey;

    @Value("${collect.news.newsapi.url}")
    private String baseUrl;

    @Value("${collect.news.newsapi.page-size}")
    private Integer pageSize;

    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate;

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
