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
class LineAnnotationDto {
    private String type;
    private String mode;
    private String scaleID;
    private Object value;
    private String borderColor;
    private Integer borderWidth;
    private List<Integer> borderDash;
    private AnnotationLabelDto label;
    private AnnotationArrowHeadsDto arrowHeads;
}
