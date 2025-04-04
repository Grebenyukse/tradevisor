package ru.grnk.tradevisor.zcommon.properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.grnk.tradevisor.zcommon.properties.jobs.CalculateJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.CalendarJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.ControlJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.JobsProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.NewsJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.PricesJobProperties;
import ru.grnk.tradevisor.zcommon.properties.jobs.PublishJobProperties;

@Configuration
public class PropertiesConfiguration {

    @Bean
    public JobsProperties jobsProperties(TradevisorProperties properties) {
        return properties.jobs();
    }

    @Bean
    public CalculateJobProperties calculateJobProperties(JobsProperties properties) {
        return properties.calculate();
    }

    @Bean
    public CalendarJobProperties calendarJobProperties(JobsProperties properties) {
        return properties.calendar();
    }

    @Bean
    public ControlJobProperties controlJobProperties(JobsProperties properties) {
        return properties.control();
    }

    @Bean
    public NewsJobProperties newsJobProperties(JobsProperties properties) {
        return properties.news();
    }

    @Bean
    public PricesJobProperties pricesJobProperties(JobsProperties properties) {
        return properties.prices();
    }

    @Bean
    public PublishJobProperties publishJobProperties(JobsProperties properties) {
        return properties.publish();
    }
}
