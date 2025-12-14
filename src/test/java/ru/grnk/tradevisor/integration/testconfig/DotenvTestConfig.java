package ru.grnk.tradevisor.integration.testconfig;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class DotenvTestConfig {
    
    @PostConstruct
    public void loadDotEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory("config")
                .filename(".testenv")
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
            
            // Set system properties from dotenv
            dotenv.entries().forEach(e -> 
                System.setProperty(e.getKey(), e.getValue())
            );
        } catch (Exception e) {
            // Handle gracefully
            System.err.println("Could not load .testenv file: " + e.getMessage());
        }
    }
}
