/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        final String reserializedJson = objectMapper.writeValueAsString(actual);
        LOGGER.info("Reserialized JSON:\n{}", reserializedJson);
        assertJsonEquals(expectedJson, reserializedJson);
        return actual;
    }

    public static <T> T testReadWrite(final T expected, final Class<T> clazz) throws IOException {
        return testReadWrite(null, expected, clazz);
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
