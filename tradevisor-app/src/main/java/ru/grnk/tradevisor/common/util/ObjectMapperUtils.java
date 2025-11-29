package ru.grnk.tradevisor.common.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Slf4j
public class ObjectMapperUtils {

    private final static ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    static {
        try {
            var maxStringLength = Integer.parseInt(System.getProperty("jackson.parser.max-string-length", "50000000"));
//            objectMapper.getFactory()
//                    .setStreamReadConstraints(
//                            StreamReadConstraints.builder()
//                                    .maxStringLength(maxStringLength)
//                                    .build()
//                    );
        } catch (Exception e) {
            log.error("ошибка конфигурации object mapper", e);
        }
    }

    @SneakyThrows
    public static <T> T readValue(String body, Class<T> valueType) {
        return objectMapper.readValue(body, valueType);
    }

    @SneakyThrows
    public static <T> T readValue(InputStream stream, Class<T> valueType) {
        return objectMapper.readValue(stream, valueType);
    }

}
