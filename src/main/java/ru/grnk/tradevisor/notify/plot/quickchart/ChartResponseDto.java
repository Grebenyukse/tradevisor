package ru.grnk.tradevisor.notify.plot.quickchart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для парсинга ответа от quickchart.io
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartResponseDto {
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("url")
    private String url;
}
