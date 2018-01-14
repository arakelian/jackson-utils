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

package com.arakelian.jackson;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.base.Preconditions;

public final class JsonProcessors {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonProcessors.class);

    private final ObjectMapper mapper;
    private final ObjectWriter writer;

    public JsonProcessors(final ObjectMapper mapper, final ObjectWriter writer) {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");
        Preconditions.checkArgument(writer != null, "writer must be non-null");
        this.mapper = mapper;
        this.writer = writer;
    }

    public <T> CollectionType collectionOf(final Class<T> clazz) {
        final CollectionType collectionType = mapper.getTypeFactory()
                .constructCollectionType(List.class, clazz);
        return collectionType;
    }

    public <T> T convertValue(final Object value, final Class<T> type) {
        return mapper.convertValue(value, type);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T> T convertValue(final Object value, final JavaType type) {
        return mapper.convertValue(value, type);
    }

    public String keyValuesToJson(final Object... keyValues) throws IOException {
        final StringWriter sw = new StringWriter();
        final JsonGenerator writer = mapper.getFactory().createGenerator(sw);
        try {
            writer.writeStartObject();
            for (int i = 0, size = keyValues.length; i < size;) {
                final String key = Objects.toString(keyValues[i++], "");
                final Object value = keyValues[i++];
                writer.writeObjectField(key, value);
            }
            writer.writeEndObject();
            writer.flush();
            return sw.toString();
        } finally {
            try {
                writer.close();
            } catch (final IOException e) {
                // ignore exception but output warning
                LOGGER.warn("Unable to close {}", writer.getClass().getSimpleName(), e);
            }
        }
    }

    public final ObjectMapper mapper() {
        return mapper;
    }

    public <T> T readValue(final String json, final Class<T> type) throws IOException {
        return StringUtils.isEmpty(json) ? null : mapper.readValue(json, type);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T> T readValue(final String json, final JavaType type) throws IOException {
        return StringUtils.isEmpty(json) ? null : mapper.readValue(json, type);
    }

    public <T> T readValue(final String json, final TypeReference<T> type) throws IOException {
        return StringUtils.isEmpty(json) ? null : mapper.readValue(json, type);
    }

    public <K, V> Map<K, V> readValueAsMap(
            final String json,
            final Class<K> keyType,
            final Class<V> valueType) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return Collections.<K, V> emptyMap();
        }
        final MapType type = mapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, keyType, valueType);
        return mapper.readValue(json, type);
    }

    public String toString(final Object value) throws JsonProcessingException {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        return writer.writeValueAsString(value);
    }

    public final ObjectWriter writer() {
        return writer;
    }
}
