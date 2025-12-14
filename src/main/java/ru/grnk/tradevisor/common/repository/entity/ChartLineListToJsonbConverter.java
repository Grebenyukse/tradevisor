package ru.grnk.tradevisor.common.repository.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.grnk.tradevisor.notify.plot.dto.ChartLineDto;

import java.util.Collections;
import java.util.List;

@Converter(autoApply = false)
public class ChartLineListToJsonbConverter implements AttributeConverter<List<ChartLineDto>, String> {

    private final static ObjectMapper objectMapper;
    static {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<ChartLineDto> chartLines) {
        if (chartLines == null || chartLines.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(chartLines);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting List<ChartLineDto> to JSONB", e);
        }
    }

    @Override
    public List<ChartLineDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(dbData, new TypeReference<List<ChartLineDto>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading JSONB to List<ChartLineDto>", e);
        }
    }
}
