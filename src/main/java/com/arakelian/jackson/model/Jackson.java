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

package com.arakelian.jackson.model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import com.arakelian.core.feature.Nullable;
import com.arakelian.jackson.databind.EnumUppercaseDeserializerModifier;
import com.arakelian.jackson.databind.TrimWhitespaceDeserializer;
import com.arakelian.jackson.databind.ZonedDateTimeDeserializer;
import com.arakelian.jackson.databind.ZonedDateTimeSerializer;
import com.arakelian.jackson.utils.JsonGeneratorCallback;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParser;
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
import com.google.common.collect.ImmutableSet;

@Value.Immutable
public abstract class Jackson {
    /** Format dates as ISO string: "yyyy-MM-dd'T'HH:mm:ss.SSSZ" **/
    public static final SimpleModule DATE_MODULE = new SimpleModule() //
            .addSerializer(new ZonedDateTimeSerializer())
            .addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());

    /** Allow enumerations to be upper or lowercase **/
    public static final SimpleModule ENUM_MODULE = new SimpleModule()
            .setDeserializerModifier(new EnumUppercaseDeserializerModifier());

    /** Trim leading and trailing whitespace, which includes tabs and newlines **/
    public static final SimpleModule TRIM_MODULE = new SimpleModule()
            .addDeserializer(String.class, TrimWhitespaceDeserializer.SINGLETON);

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = defaultBuilder().build() //
            .getObjectMapper();

    @SuppressWarnings(value = { "deprecation", "immutables:incompat" })
    public static ImmutableJackson.Builder defaultBuilder() {
        return ImmutableJackson.builder() //
                // general configuration
                .locale(Locale.getDefault()) //
                .pretty(true) //
                .serializationInclusion(JsonInclude.Include.NON_EMPTY) //

                // modules
                .findAndRegisterModules(true) //
                .registerTrimModule(true) //
                .registerDateModule(true) //
                .registerEnumModule(true) //

                // avoid unnecessary exception if module registered automatically and manually
                .putMapperFeatures(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS, true) //

                // collections are often immutable and not suitable for updates
                .putMapperFeatures(MapperFeature.USE_GETTERS_AS_SETTERS, false) //

                // forgiving parser
                .putParserFeatures(JsonParser.Feature.ALLOW_COMMENTS, true) //
                .putParserFeatures(JsonParser.Feature.ALLOW_YAML_COMMENTS, true) //
                .putParserFeatures(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true) //
                .putParserFeatures(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true) //
                .putParserFeatures(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true) //

                // serialization options
                .putSerializationFeatures(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false) //
                .putSerializationFeatures(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false) //
                .putSerializationFeatures(SerializationFeature.FAIL_ON_EMPTY_BEANS, false) //

                // strict rules regarding data integrity
                .putDeserializationFeatures(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true) //
                .putDeserializationFeatures(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true) //
                .putDeserializationFeatures(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true) //
                .putDeserializationFeatures(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true) //

                // we don't want big decimals output in scientific notation
                .putGeneratorFeatures(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true) //
        ;
    }

    public static ImmutableJackson.Builder from(final ObjectMapper mapper) {
        return ImmutableJackson.builder().defaultMapper(mapper);
    }

    public static ImmutableJackson.Builder fromDefault() {
        return from(DEFAULT_OBJECT_MAPPER);
    }

    public static Jackson of() {
        return fromDefault().build();
    }

    public static Jackson of(final ObjectMapper mapper) {
        return from(mapper).build();
    }

    public CharSequence buildJson(final Object... keyValues) {
        final int length = keyValues.length;
        Preconditions.checkArgument(
                length % 2 == 0,
                "Expected key-value pairs, but received array with odd number of entries");

        return toCharSequence(true, gen -> {
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
        });
    }

    public JsonNode buildJsonNode(final Object... keyValues) {
        final CharSequence json = buildJson(keyValues);
        try {
            return readValue(new CharSequenceReader(json), JsonNode.class);
        } catch (final IOException e) {
            // should not happen since we serialized JSON ourselves
            throw new UncheckedIOException(e);
        }
    }

    public <T> T convertValue(final Object value, final Class<T> valueType) {
        return getObjectMapper().convertValue(value, valueType);
    }

    public Map<String, Object> convertValueToMap(final Object value) {
        return convertValueToMap(value, String.class, Object.class);
    }

    public <K, V> Map<K, V> convertValueToMap(
            final Object value,
            final Class<K> keyType,
            final Class<V> valueType) {
        final MapType type = mapType(LinkedHashMap.class, keyType, valueType);
        return getObjectMapper().convertValue(value, type);
    }

    public JsonGenerator createGenerator(final Writer writer, final boolean pretty) throws IOException {
        final JsonGenerator generator = getObjectMapper().getFactory().createGenerator(writer);
        if (pretty) {
            return generator.useDefaultPrettyPrinter();
        } else {
            return generator;
        }
    }

    private ObjectWriter createObjectWriter(final Class<?> view, final boolean pretty) {
        final ObjectMapper mapper = getObjectMapper();

        if (view != null) {
            if (pretty) {
                return mapper.writerWithView(view).withDefaultPrettyPrinter();
            } else {
                return mapper.writerWithView(view).withoutFeatures(SerializationFeature.INDENT_OUTPUT);
            }
        } else {
            if (pretty) {
                return mapper.writerWithDefaultPrettyPrinter();
            } else {
                return mapper.writer().withoutFeatures(SerializationFeature.INDENT_OUTPUT);
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        // use reference equality
        return super.equals(obj);
    }

    public ImmutableJackson.Builder from() {
        return from(getObjectMapper());
    }

    @Nullable
    @Value.Auxiliary
    public abstract ObjectMapper getDefaultMapper();

    @Value.Auxiliary
    public abstract Map<DeserializationFeature, Boolean> getDeserializationFeatures();

    @Value.Auxiliary
    public abstract Map<Feature, Boolean> getGeneratorFeatures();

    @Nullable
    public abstract Locale getLocale();

    public abstract Map<MapperFeature, Boolean> getMapperFeatures();

    @Value.Default
    public Set<Module> getModules() {
        final ImmutableSet.Builder<Module> modules = ImmutableSet.<Module> builder();
        if (isRegisterTrimModule()) {
            modules.add(TRIM_MODULE);
        }
        if (isRegisterEnumModule()) {
            modules.add(ENUM_MODULE);
        }
        if (isRegisterDateModule()) {
            modules.add(DATE_MODULE);
        }
        return modules.build();
    }

    @SuppressWarnings("deprecation")
    @Value.Lazy
    public ObjectMapper getObjectMapper() {
        final ObjectMapper mapper;

        final ObjectMapper defaultMapper = getDefaultMapper();
        if (defaultMapper != null) {
            mapper = defaultMapper;
        } else {
            mapper = new ObjectMapper();
        }

        // set locale
        final Locale locale = getLocale();
        if (locale != null) {
            mapper.setLocale(locale);
        }

        final Map<Feature, Boolean> features = getGeneratorFeatures();
        for (final Feature feature : features.keySet()) {
            mapper.configure(feature, features.get(feature).booleanValue());
        }

        final Map<SerializationFeature, Boolean> serializationFeatures = getSerializationFeatures();
        for (final SerializationFeature feature : serializationFeatures.keySet()) {
            mapper.configure(feature, serializationFeatures.get(feature).booleanValue());
        }

        final Boolean pretty = isPretty();
        if (pretty != null) {
            mapper.configure(SerializationFeature.INDENT_OUTPUT, pretty.booleanValue());
        }

        final Map<DeserializationFeature, Boolean> deserializationFeatures = getDeserializationFeatures();
        for (final DeserializationFeature feature : deserializationFeatures.keySet()) {
            mapper.configure(feature, deserializationFeatures.get(feature).booleanValue());
        }

        final Map<JsonParser.Feature, Boolean> parserFeatures = getParserFeatures();
        for (final JsonParser.Feature feature : parserFeatures.keySet()) {
            mapper.configure(feature, parserFeatures.get(feature).booleanValue());
        }

        final Map<MapperFeature, Boolean> mapperFeatures = getMapperFeatures();
        for (final MapperFeature feature : mapperFeatures.keySet()) {
            mapper.configure(feature, mapperFeatures.get(feature).booleanValue());
        }

        // only register modules after mapper features have been configured; there is at least one
        // mapper feature that allows us to ignore duplicate module registration errors
        if (isFindAndRegisterModules()) {
            mapper.findAndRegisterModules();
        }

        for (final Module module : getModules()) {
            mapper.registerModule(module);
        }

        final Class<?> view = getView();
        if (view != null) {
            mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, isDefaultViewInclusion());
            mapper.setConfig(mapper.getSerializationConfig().withView(view));
            mapper.setConfig(mapper.getDeserializationConfig().withView(view));
        }

        final Include serializationInclusion = getSerializationInclusion();
        if (serializationInclusion != null) {
            mapper.setSerializationInclusion(serializationInclusion);
        }

        return mapper;
    }

    @Value.Lazy
    public ObjectWriter getObjectWriter() {
        return createObjectWriter(getView(), false);
    }

    public ObjectWriter getObjectWriter(final Class<?> view, final boolean pretty) {
        if (view == getView()) {
            if (pretty) {
                return getObjectWriterWithPrettyPrinter();
            } else {
                return getObjectWriter();
            }
        }
        return createObjectWriter(view, pretty);
    }

    @Value.Lazy
    public ObjectWriter getObjectWriterWithPrettyPrinter() {
        return createObjectWriter(getView(), true);
    }

    public abstract Map<JsonParser.Feature, Boolean> getParserFeatures();

    public abstract Map<SerializationFeature, Boolean> getSerializationFeatures();

    @Nullable
    public abstract JsonInclude.Include getSerializationInclusion();

    @Nullable
    public abstract Class<?> getView();

    @Override
    public int hashCode() {
        // use default hash code
        return super.hashCode();
    }

    @Value.Default
    public boolean isDefaultViewInclusion() {
        return true;
    }

    @Value.Default
    public boolean isFindAndRegisterModules() {
        return false;
    }

    @Nullable
    public abstract Boolean isPretty();

    @Value.Default
    public boolean isRegisterDateModule() {
        return true;
    }

    @Value.Default
    public boolean isRegisterEnumModule() {
        return true;
    }

    @Value.Default
    public boolean isRegisterTrimModule() {
        return true;
    }

    public <K, V> MapType mapType(
            final Class<? extends Map> mapClass,
            final Class<K> keyType,
            final Class<V> valueType) {
        Preconditions.checkArgument(keyType != null, "keyType must be non-null");
        Preconditions.checkArgument(valueType != null, "valueType must be non-null");
        final MapType type = getObjectMapper().getTypeFactory()
                .constructMapType(mapClass, keyType, valueType);
        return type;
    }

    public <T> T readValue(final Reader src, final Class<T> valueType) throws IOException {
        return getObjectMapper().readValue(src, valueType);
    }

    public <T> T readValue(final String json, final Class<T> type) throws IOException {
        return StringUtils.isEmpty(json) ? null : getObjectMapper().readValue(json, type);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T> T readValue(final String json, final JavaType valueType) throws IOException {
        return StringUtils.isEmpty(json) ? null : getObjectMapper().readValue(json, valueType);
    }

    public <T> T readValue(final String json, final TypeReference<T> valueType) throws IOException {
        return StringUtils.isEmpty(json) ? null : getObjectMapper().readValue(json, valueType);
    }

    public <K, V> Map<K, V> readValueAsMap(
            final String json,
            final Class<K> keyType,
            final Class<V> valueType) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return Collections.<K, V> emptyMap();
        }

        final MapType type = mapType(LinkedHashMap.class, keyType, valueType);
        return getObjectMapper().readValue(json, type);
    }

    public CharSequence toCharSequence(final boolean pretty, final JsonGeneratorCallback callback)
            throws UncheckedIOException {
        final StringWriter out = new StringWriter(128);
        try (final JsonGenerator gen = createGenerator(out, pretty)) {
            callback.accept(gen);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        // let's give caller a chance to avoid a toString
        return out.getBuffer();
    }

    public CharSequence toCharSequence(final JsonGeneratorCallback callback) {
        return toCharSequence(true, callback);
    }

    public String toString(final Object value) throws UncheckedIOException {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        try {
            return getObjectMapper().writeValueAsString(value);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String toString(final Object value, final boolean pretty) throws UncheckedIOException {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        try {
            final ObjectWriter w = pretty ? getObjectWriterWithPrettyPrinter() : getObjectWriter();
            return w.writeValueAsString(value);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
