package ru.grnk.tradevisor.common.repository.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.grnk.tradevisor.notify.plot.dto.ChartLineDto;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Converter(autoApply = false)
public class ChartLineListToJsonbConverter implements AttributeConverter<List<ChartLineDto>, PGobject> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(List<ChartLineDto> chartLines) {
        if (chartLines == null || chartLines.isEmpty()) {
            return null;
        }

        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");

        try {
            String json = objectMapper.writeValueAsString(chartLines);
            pgObject.setValue(json);
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting List<ChartLineDto> to JSONB", e);
        }

        return pgObject;
    }

    @Override
    public List<ChartLineDto> convertToEntityAttribute(PGobject dbData) {
        if (dbData == null || dbData.getValue() == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(dbData.getValue(), new TypeReference<List<ChartLineDto>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading JSONB to List<ChartLineDto>", e);
        }
    }
}
