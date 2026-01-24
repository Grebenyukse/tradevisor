package ru.grnk.tradevisor.integration.rts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharsetInfo {
    @JsonProperty("name")
    private String name;
}
