package ru.grnk.tradevisor.notify.plot.quickchart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO классы для Chart.js конфигурации
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartRequestDto {
    private int width;
    private int height;
    private String backgroundColor;
    private String format;
    private ChartDto chart;
    private int version;
}
