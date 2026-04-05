package ru.grnk.tradevisor;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.grnk.tradevisor.common.properties.TradevisorProperties;


@SpringBootApplication(scanBasePackages = "ru.grnk.tradevisor")
@EnableConfigurationProperties({TradevisorProperties.class})
public class TradeVisorApplication {
    public static void main(String[] args) {
        // Загрузка переменных из .env
        Dotenv.configure().ignoreIfMissing().systemProperties().load();
        SpringApplication.run(TradeVisorApplication.class, args);
    }
}
