package ru.grnk.tradevisor.notify.plot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ChartDatasetDto {
    private String label;
    private List<Double> data;
    private List<String> backgroundColor;
    private List<String> borderColor;
    private Integer borderWidth;
}
