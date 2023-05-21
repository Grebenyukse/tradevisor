package ru.grnk.tradevisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.grnk.tradevisor.properties.TradevisorProperties;


@SpringBootApplication(scanBasePackages = "ru.grnk.tradevisor")
@EnableConfigurationProperties({TradevisorProperties.class})
public class TradeAdvisorApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeAdvisorApplication.class, args);
    }
}
