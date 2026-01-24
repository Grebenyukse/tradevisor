package ru.grnk.tradevisor.integration.rts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecuritiesData {
    @JsonProperty("securities")
    private List<Security> securities;
}
