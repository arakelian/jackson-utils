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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang3.StringUtils;

import com.arakelian.jackson.ImmutableJacksonOptions;
import com.arakelian.jackson.JacksonOptions;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.base.Preconditions;

public class JacksonUtils {
    @FunctionalInterface
    public interface JsonGeneratorCallback {
        public void accept(JsonGenerator gen) throws IOException;
    }

    /** Default ObjectMapper **/
    private static JacksonOptions DEFAULT = ImmutableJacksonOptions.builder().build();

    public static ImmutableJacksonOptions.Builder builder() {
        return ImmutableJacksonOptions.builder();
    }

    public static ObjectMapper configure(final ObjectMapper mapper, final boolean pretty) {
        return ImmutableJacksonOptions.builder() //
                .pretty(pretty) //
                .build() //
                .configure(mapper);
    }

    public static <T> T convertValue(final Object value, final Class<T> type) {
        return mapper().convertValue(value, type);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertValueToMap(final Object value) {
        return mapper().convertValue(value, LinkedHashMap.class);
    }

    public static ObjectMapper getObjectMapper() {
        return mapper().copy();
    }

    public static ObjectWriter getObjectWriter(final boolean pretty) {
        return DEFAULT.getObjectWriter(DEFAULT.getView(), pretty);
    }

    public static ObjectWriter getObjectWriter(final Class<?> view) {
        return DEFAULT.getObjectWriter(view, false);
    }

    public static ObjectWriter getObjectWriter(final Class<?> view, final boolean pretty) {
        return DEFAULT.getObjectWriter(view, pretty);
    }

    public static <T> T readValue(final ObjectMapper mapper, final String json, final Class<T> type)
            throws IOException {
        Preconditions.checkArgument(type != null, "type must be non-null");
        return StringUtils.isEmpty(json) ? null : defaultMapper(mapper).readValue(json, type);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T readValue(final ObjectMapper mapper, final String json, final JavaType type)
            throws IOException {
        Preconditions.checkArgument(type != null, "type must be non-null");
        return StringUtils.isEmpty(json) ? null : defaultMapper(mapper).readValue(json, type);
    }

    public static <T> T readValue(final ObjectMapper mapper, final String json, final TypeReference<T> type)
            throws IOException {
        Preconditions.checkArgument(type != null, "type must be non-null");
        return StringUtils.isEmpty(json) ? null : defaultMapper(mapper).readValue(json, type);
    }

    public static <T> T readValue(final String json, final Class<T> type) throws IOException {
        return readValue(mapper(), json, type);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T readValue(final String json, final JavaType type) throws IOException {
        return readValue(null, json, type);
    }

    public static <T> T readValue(final String json, final TypeReference<T> type) throws IOException {
        return readValue(null, json, type);
    }

    public static <K, V> Map<K, V> readValueAsMap(
            final ObjectMapper mapper,
            final String json,
            final Class<K> keyType,
            final Class<V> valueType) throws IOException {
        Preconditions.checkArgument(keyType != null, "keyType must be non-null");
        Preconditions.checkArgument(valueType != null, "valueType must be non-null");

        if (StringUtils.isEmpty(json)) {
            return Collections.<K, V> emptyMap();
        }

        final MapType type = mapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, keyType, valueType);
        return defaultMapper(mapper).readValue(json, type);
    }

    public static CharSequence toCharSequence(final JsonGeneratorCallback callback)
            throws UncheckedIOException {
        return toCharSequence(callback, mapper(), true);
    }

    public static CharSequence toCharSequence(
            final JsonGeneratorCallback callback,
            final ObjectMapper mapper,
            final boolean pretty) throws UncheckedIOException {

        // default size would be 16 bytes; let's choose something more reasonable for a JSON data
        // structure
        final StringWriter out = new StringWriter(128);

        if (pretty) {
            try (final JsonGenerator gen = mapper.getFactory() //
                    .createGenerator(out) //
                    .useDefaultPrettyPrinter()) {
                callback.accept(gen);
            } catch (final IOException e) {
                // shouldn't happen when writing to a String
                throw new UncheckedIOException(e);
            }
        } else {
            try (final JsonGenerator writer = mapper.getFactory().createGenerator(out)) {
                callback.accept(writer);
            } catch (final IOException e) {
                // shouldn't happen when writing to a String
                throw new UncheckedIOException(e);
            }
        }

        // let's give caller a chance to avoid a toString
        return out.getBuffer();
    }

    public static CharSequence toJson(final Object... keyValues) {
        return toJson(null, keyValues);
    }

    public static CharSequence toJson(final ObjectMapper mapper, final Object... keyValues) {
        final int length = keyValues.length;
        Preconditions.checkArgument(
                length % 2 == 0,
                "Expected key-value pairs, but received array with odd number of entries");

        return toCharSequence(gen -> {
            gen.writeStartObject();
            for (int i = 0; i < length;) {
                final String key = Objects.toString(keyValues[i++], null);
                if (key == null) {
                    continue;
                }
                final Object value = keyValues[i++];
                if (value != null) {
                    gen.writeObjectField(key, value);
                }
            }
            gen.writeEndObject();
        }, defaultMapper(mapper), true);
    }

    public static JsonNode toJsonNode(final Object... keyValues) {
        return toJsonNode(null, keyValues);
    }

    public static JsonNode toJsonNode(final ObjectMapper mapper, final Object... keyValues) {
        final ObjectMapper m = defaultMapper(mapper);
        final CharSequence json = toJson(m, keyValues);
        try {
            return m.readValue(new CharSequenceReader(json), JsonNode.class);
        } catch (final IOException e) {
            // should not happen since we serialized JSON ourselves
            throw new UncheckedIOException(e);
        }
    }

    public static String toString(final JsonGeneratorCallback callback) throws UncheckedIOException {
        final CharSequence csq = toCharSequence(callback);
        return csq != null ? csq.toString() : null;
    }

    public static String toString(
            final JsonGeneratorCallback callback,
            final ObjectMapper mapper,
            final boolean pretty) throws UncheckedIOException {
        final CharSequence csq = toCharSequence(callback, mapper, pretty);
        return csq != null ? csq.toString() : null;
    }

    public static String toString(final Object value, final boolean pretty) throws IOException {
        return toString(value, getObjectWriter(pretty));
    }

    public static String toString(final Object value, final ObjectWriter writer)
            throws JsonProcessingException {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        final ObjectWriter w = defaultWriter(writer);
        return w.writeValueAsString(value);
    }

    public static String toStringSafe(final Object value, final boolean pretty) throws UncheckedIOException {
        try {
            return toString(value, pretty);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void traverse(
            final JsonNode node,
            final Predicate<String> fieldPredicate,
            final Consumer<JsonNode> consumer) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }

        if (node.isArray()) {
            for (int i = 0, size = node.size(); i < size; i++) {
                final JsonNode item = node.get(i);
                traverse(item, null, consumer);
            }
            return;
        }

        if (node.isObject()) {
            final ObjectNode obj = (ObjectNode) node;
            for (final Iterator<String> it = obj.fieldNames(); it.hasNext();) {
                final String name = it.next();
                if (fieldPredicate == null || fieldPredicate.test(name)) {
                    final JsonNode child = obj.get(name);
                    traverse(child, null, consumer);
                }
            }
            return;
        }

        if (!node.isPojo()) {
            consumer.accept(node);
        }
    }

    public static ObjectMapper withView(final ObjectMapper mapper, final Class<?> view) {
        mapper.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setConfig(mapper.getSerializationConfig().withView(view));
        mapper.setConfig(mapper.getDeserializationConfig().withView(view));
        return mapper;
    }

    private static ObjectMapper defaultMapper(final ObjectMapper mapper) {
        // method is private so that we do not expose ObjectMapper
        return mapper != null ? mapper : DEFAULT.getObjectMapper();
    }

    private static ObjectWriter defaultWriter(final ObjectWriter writer) {
        return writer != null ? writer : DEFAULT.getObjectWriterWithPrettyPrinter();
    }

    private static ObjectMapper mapper() {
        // method is private so that we do not expose ObjectMapper
        return DEFAULT.getObjectMapper();
    }

    private JacksonUtils() {
        // utility class
    }
}
