package ru.grnk.tradevisor.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class ObjectMapperUtils {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static <T> T readValue(String body, Class<T> valueType) {
        return objectMapper.readValue(body, valueType);
    }
}
