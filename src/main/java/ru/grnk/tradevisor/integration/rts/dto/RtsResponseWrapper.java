package ru.grnk.tradevisor.integration.rts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RtsResponseWrapper {
    private CharsetInfo charsetinfo;
    private SecuritiesData securitiesData;
}
