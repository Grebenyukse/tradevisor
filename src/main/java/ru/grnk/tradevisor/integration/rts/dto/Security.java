package ru.grnk.tradevisor.integration.rts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Security {
    @JsonProperty("SECID")
    private String secid;
    
    @JsonProperty("MINSTEP")
    private BigDecimal minstep;
    
    @JsonProperty("STEPPRICE")
    private BigDecimal stepPrice;
    
    @JsonProperty("INITIALMARGIN")
    private BigDecimal initialMargin;
    
    @JsonProperty("LOTSIZE")
    private Integer lotSize;
}
