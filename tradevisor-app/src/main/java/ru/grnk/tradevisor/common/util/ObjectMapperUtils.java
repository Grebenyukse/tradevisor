package ru.grnk.tradevisor.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.InputStream;

public class ObjectMapperUtils {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static <T> T readValue(String body, Class<T> valueType) {
        return objectMapper.readValue(body, valueType);
    }

    @SneakyThrows
    public static <T> T readValue(InputStream stream, Class<T> valueType) {
        return objectMapper.readValue(stream, valueType);
    }

}
