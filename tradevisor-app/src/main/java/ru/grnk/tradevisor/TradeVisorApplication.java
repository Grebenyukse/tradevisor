package ru.grnk.tradevisor;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.grnk.tradevisor.zcommon.properties.TradevisorProperties;


@SpringBootApplication(scanBasePackages = "ru.grnk.tradevisor")
@EnableConfigurationProperties({TradevisorProperties.class})
public class TradeVisorApplication {
    public static void main(String[] args) {
        // Загрузка переменных из .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(TradeVisorApplication.class, args);
    }
}
