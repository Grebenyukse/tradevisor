package ru.grnk.tradevisor.integration.yahoofinance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceService {

    private final RestTemplate restTemplate;

    @Getter
    private String crumb;
    private List<String> cookies = new ArrayList<>();

    // Инициализация - получение crumb и cookies
    @PostConstruct
    public void initialize() {
        refreshCrumbAndCookies();
    }

    public void refreshCrumbAndCookies() {
        try {
            log.info("Refreshing crumb and cookies...");

            // Создаем новую сессию
            HttpHeaders initialHeaders = createBaseHeaders();
            HttpEntity<String> initialEntity = new HttpEntity<>(initialHeaders);

            // Первый запрос для получения начальных куков
            ResponseEntity<String> consentResponse = restTemplate.exchange(
                    "https://finance.yahoo.com",
                    HttpMethod.GET,
                    initialEntity,
                    String.class
            );

            // Сохраняем куки из первого ответа
            List<String> initialCookies = extractCookies(consentResponse.getHeaders());
            this.cookies.addAll(initialCookies);

            log.info("Initial cookies count: {}", initialCookies.size());

            // Создаем заголовки с накопленными куками
            HttpHeaders headersWithCookies = createBaseHeaders();
            if (!this.cookies.isEmpty()) {
                String cookieHeader = String.join("; ", this.cookies);
                headersWithCookies.add("Cookie", cookieHeader);
                log.info("Cookie header: {}", cookieHeader);
            }

            HttpEntity<String> entityWithCookies = new HttpEntity<>(headersWithCookies);

            // Получаем crumb
            ResponseEntity<String> crumbResponse = restTemplate.exchange(
                    "https://query1.finance.yahoo.com/v1/test/getcrumb",
                    HttpMethod.GET,
                    entityWithCookies,
                    String.class
            );

            this.crumb = crumbResponse.getBody();
            log.info("Crumb obtained successfully: {}", this.crumb);

            // Добавляем куки из ответа получения crumb
            List<String> crumbCookies = extractCookies(crumbResponse.getHeaders());
            this.cookies.addAll(crumbCookies);
            log.info("Total cookies count after crumb: {}", this.cookies.size());

        } catch (Exception e) {
            log.error("Error obtaining crumb and cookies", e);
            this.crumb = null;
            this.cookies.clear();
        }
    }

    public String getHistoricalData(String symbol, String interval, int daysBack) {
        try {
            long endTime = Instant.now().getEpochSecond();
            long startTime = Instant.now().minus(daysBack, ChronoUnit.DAYS).getEpochSecond();

            String url = String.format(
                    "https://query2.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=%s&events=div,splits,capitalGains",
                    symbol, startTime, endTime, interval
            );

            if (crumb != null && !crumb.isEmpty()) {
                url += "&crumb=" + crumb;
            }

            HttpHeaders headers = createBaseHeaders();
            if (!cookies.isEmpty()) {
                String cookieHeader = String.join("; ", cookies);
                headers.add("Cookie", cookieHeader);
                log.info("Using cookies for chart request: {}", cookieHeader);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Making chart request to: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching historical data for symbol: " + symbol, e);

            // Если ошибка авторизации, пробуем обновить crumb
            if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                log.info("Refreshing crumb and cookies due to auth error");
                refreshCrumbAndCookies();
            }

            throw e;
        }
    }

    private HttpHeaders createBaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.add("Accept-Language", "en-US,en;q=0.5");
        headers.add("Accept-Encoding", "gzip, deflate, br");
        headers.add("DNT", "1");
        headers.add("Connection", "keep-alive");
        headers.add("Upgrade-Insecure-Requests", "1");
        headers.add("Sec-Fetch-Dest", "document");
        headers.add("Sec-Fetch-Mode", "navigate");
        headers.add("Sec-Fetch-Site", "none");
        headers.add("Cache-Control", "max-age=0");
        return headers;
    }

    private List<String> extractCookies(HttpHeaders responseHeaders) {
        List<String> cookieList = new ArrayList<>();
        List<String> setCookieHeaders = responseHeaders.get("Set-Cookie");

        if (setCookieHeaders != null) {
            for (String cookie : setCookieHeaders) {
                // Берем только имя=значение часть куки
                if (cookie != null && cookie.contains("=")) {
                    String[] parts = cookie.split(";");
                    if (parts.length > 0) {
                        String cookiePart = parts[0].trim();
                        if (!cookiePart.isEmpty() && cookiePart.contains("=")) {
                            cookieList.add(cookiePart);
                            log.debug("Extracted cookie: {}", cookiePart);
                        }
                    }
                }
            }
        }

        return cookieList;
    }

    public List<String> getCookies() {
        return new ArrayList<>(cookies);
    }
}
