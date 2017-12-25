package com.arakelian.jackson.utils;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonTestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonTestUtils.class);

    public static <T> T testReadWrite(final T expected, final Class<T> clazz) throws IOException {
        return testReadWrite(null, expected, clazz);
    }

    public static <T> T testReadWrite(ObjectMapper objectMapper, final T expected, final Class<T> clazz)
            throws JsonProcessingException, IOException, JsonParseException, JsonMappingException {
        objectMapper = configure(objectMapper);

        // serialize bean
        final String expectedJson = objectMapper.writeValueAsString(expected);
        LOGGER.info("{} serialized to JSON:\n{}", clazz.getSimpleName(), expectedJson);

        // deserialize from JSON and make sure we have object equality
        final T actual = objectMapper.readValue(expectedJson, clazz);
        assertEquals(expected, actual);

        // serialize the clone and make sure it matches original
        String reserializedJson = objectMapper.writeValueAsString(actual);
        LOGGER.info("Reserialized JSON:\n{}", reserializedJson);
        assertJsonEquals(expectedJson, reserializedJson);
        return actual;
    }

    private static ObjectMapper configure(ObjectMapper objectMapper) {
        // configure object mapper
        if (objectMapper == null) {
            objectMapper = JacksonUtils.getObjectMapper();
        }
        objectMapper = objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return objectMapper;
    }
}
