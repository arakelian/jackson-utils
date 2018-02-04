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
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.arakelian.jackson.JacksonOptions;
import com.arakelian.jackson.JacksonProcessors;
import com.arakelian.jackson.databind.EnumUppercaseDeserializerModifier;
import com.arakelian.jackson.databind.TrimWhitespaceDeserializer;
import com.arakelian.jackson.databind.ZonedDateTimeDeserializer;
import com.arakelian.jackson.databind.ZonedDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.base.Preconditions;

public class JacksonUtils {
    @FunctionalInterface
    public interface JsonGeneratorCallback {
        public void accept(JsonGenerator gen) throws IOException;
    }

    private static ObjectMapper DEFAULT_MAPPER;

    static {
        // when parsing JSON, we want to automatically trim away and leading and trailing whitespace
        // (this includes not only spaces, but tabs and newlines as well)
        final SimpleModule trimWhitespace = new SimpleModule() //
                .addDeserializer(String.class, TrimWhitespaceDeserializer.SINGLETON);

        // change Jackson Enum deserialization so that it forces to uppercase
        final SimpleModule forceEnumsUppercase = new SimpleModule()
                .setDeserializerModifier(new EnumUppercaseDeserializerModifier());

        // handle ZonedDateTime using DateUtils
        final SimpleModule javaDates = new SimpleModule() //
                .addSerializer(new ZonedDateTimeSerializer()) //
                .addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());

        // customize Jackson
        GLOBAL_MODULES = new LinkedHashSet<>();
        addModule(trimWhitespace, forceEnumsUppercase, javaDates);
    }

    /**
     * Allow clients to override the serializer factory globally; very useful if client want to use
     * a Spring-aware serializer
     **/
    static final Set<Module> GLOBAL_MODULES;

    public static void addModule(final Module... modules) {
        for (int i = 0; i < modules.length; i++) {
            GLOBAL_MODULES.add(modules[i]);
        }
        DEFAULT_MAPPER = configure(new ObjectMapper(), false);
    }

    public static JacksonOptions builder() {
        return new JacksonOptions(DEFAULT_MAPPER, GLOBAL_MODULES.hashCode());
    }

    @SuppressWarnings("deprecation")
    public static ObjectMapper configure(final ObjectMapper mapper, final boolean pretty) {
        // keep JSON simple and readable by removing empty values from output
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // we don't want scientific notation used for big decimals
        mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

        // seems reasonable to allow caller to pass single value for an array
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // we do want to enforce some strict policies on other "bad" data
        mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
        mapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

        // we don't care if a bean has any serializable properties
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // caller can optionally request pretty formatting
        if (pretty) {
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }

        // add default modules
        mapper.findAndRegisterModules();

        // register custom modules, which take priority because they are registered last
        for (final Module module : GLOBAL_MODULES) {
            mapper.registerModule(module);
        }

        // return mapper
        return mapper;
    }

    public static <T> T convertValue(final Object value, final Class<T> type) {
        return getJsonProcessors().mapper().convertValue(value, type);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertValueToMap(final Object value) {
        return getObjectMapper().convertValue(value, LinkedHashMap.class);
    }

    public static JacksonProcessors getJsonProcessors() {
        return builder().build();
    }

    public static ObjectMapper getObjectMapper() {
        return getJsonProcessors().mapper();
    }

    public static ObjectWriter getObjectWriter(final boolean pretty) {
        return builder().pretty(pretty).build().writer();
    }

    public static <T> T readValue(final ObjectMapper mapper, final String json, final Class<T> type)
            throws IOException {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");
        Preconditions.checkArgument(type != null, "type must be non-null");
        return StringUtils.isEmpty(json) ? null : mapper.readValue(json, type);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T readValue(final ObjectMapper mapper, final String json, final JavaType type)
            throws IOException {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");
        Preconditions.checkArgument(type != null, "type must be non-null");
        return StringUtils.isEmpty(json) ? null : mapper.readValue(json, type);
    }

    public static <T> T readValue(final ObjectMapper mapper, final String json, final TypeReference<T> type)
            throws IOException {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");
        Preconditions.checkArgument(type != null, "type must be non-null");
        return StringUtils.isEmpty(json) ? null : mapper.readValue(json, type);
    }

    public static <T> T readValue(final String json, final Class<T> type) throws IOException {
        return readValue(getObjectMapper(), json, type);
    }

    public static String toJson(final Object... keyValues) {
        return toJson(getObjectMapper(), keyValues);
    }

    public static String toJson(final ObjectMapper mapper, final Object... keyValues) {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");

        final int length = keyValues.length;
        Preconditions.checkArgument(
                length % 2 == 0,
                "Expected key-value pairs, but received array with odd number of entries");

        return toString(gen -> {
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
        }, mapper, true);
    }

    public static JsonNode toJsonNode(final Object... keyValues) {
        return toJsonNode(getObjectMapper(), keyValues);
    }

    public static JsonNode toJsonNode(final ObjectMapper mapper, final Object... keyValues) {
        final String json = toJson(mapper, keyValues);
        try {
            return mapper.readValue(json, JsonNode.class);
        } catch (final IOException e) {
            // should not happen since we serialized JSON ourselves
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a serialized version of the object as a Map.
     *
     * @param value
     *            object value
     * @param locale
     *            target locale
     * @return serialized version of the object as a Map.
     */
    public static Map toMap(final Object value, final Locale locale) {
        return builder().locale(locale).build().mapper().convertValue(value, LinkedHashMap.class);
    }

    public static String toString(final JsonGeneratorCallback callback) throws UncheckedIOException {
        return toString(callback, getObjectMapper(), true);
    }

    public static String toString(
            final JsonGeneratorCallback callback,
            final ObjectMapper mapper,
            final boolean pretty) throws UncheckedIOException {

        final StringWriter out = new StringWriter();

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

        final String result = out.getBuffer().toString();
        return result;
    }

    public static String toString(final Object value, final boolean pretty) throws JsonProcessingException {
        return value == null ? StringUtils.EMPTY : builder().pretty(pretty).build().toString(value);
    }

    public static ObjectMapper withView(final ObjectMapper mapper, final Class<?> view) {
        mapper.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setConfig(mapper.getSerializationConfig().withView(view));
        mapper.setConfig(mapper.getDeserializationConfig().withView(view));
        return mapper;
    }

    private JacksonUtils() {
        // utility class
    }

    public <K, V> Map<K, V> readValueAsMap(
            final ObjectMapper mapper,
            final String json,
            final Class<K> keyType,
            final Class<V> valueType) throws IOException {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");
        Preconditions.checkArgument(keyType != null, "keyType must be non-null");
        Preconditions.checkArgument(valueType != null, "valueType must be non-null");

        if (StringUtils.isEmpty(json)) {
            return Collections.<K, V> emptyMap();
        }
        final MapType type = mapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, keyType, valueType);
        return mapper.readValue(json, type);
    }
}
