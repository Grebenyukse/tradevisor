package ru.grnk.tradevisor.integration.testconfig;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.testutils.LoadMarketDataCsv;

@Configuration
public class DotenvTestConfig {

    @PostConstruct
    public void loadDotEnv() {
        try {
            Dotenv.configure()
                .directory("config")
                .filename(".testenv")
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .systemProperties()
                .load();
        } catch (Exception e) {
            // Handle gracefully
            System.err.println("Could not load .testenv file: " + e.getMessage());
        }
    }

    @Bean
    public LoadMarketDataCsv loadMarketDataCsv() {
        return new LoadMarketDataCsv();
    }
}
