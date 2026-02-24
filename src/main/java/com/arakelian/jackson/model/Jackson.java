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

/**
 * Immutable configuration for building customized Jackson {@link ObjectMapper} instances. Provides
 * sensible defaults including whitespace trimming, case-insensitive enum deserialization, and ISO
 * date formatting.
 */
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

    /**
     * Protected constructor to prevent direct instantiation.
     */
    protected Jackson() {
    }

    /**
     * Returns a builder pre-configured with sensible defaults for Jackson serialization and deserialization.
     *
     * @return a pre-configured builder
     */
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

    /**
     * Returns a builder that wraps the given {@link ObjectMapper} as its default mapper.
     *
     * @param mapper the {@link ObjectMapper} to wrap
     * @return a builder initialized with the given mapper
     */
    public static ImmutableJackson.Builder from(final ObjectMapper mapper) {
        return ImmutableJackson.builder().defaultMapper(mapper);
    }

    /**
     * Returns a builder that wraps the default {@link ObjectMapper}.
     *
     * @return a builder initialized with the default mapper
     */
    public static ImmutableJackson.Builder fromDefault() {
        return from(DEFAULT_OBJECT_MAPPER);
    }

    /**
     * Creates a {@link Jackson} instance with the default configuration.
     *
     * @return a new {@link Jackson} instance with defaults
     */
    public static Jackson of() {
        return fromDefault().build();
    }

    /**
     * Creates a {@link Jackson} instance that wraps the given {@link ObjectMapper}.
     *
     * @param mapper the {@link ObjectMapper} to use
     * @return a new {@link Jackson} instance wrapping the given mapper
     */
    public static Jackson of(final ObjectMapper mapper) {
        return from(mapper).build();
    }

    /**
     * Builds a JSON string from the given key-value pairs.
     *
     * @param keyValues alternating keys and values
     * @return the serialized JSON string
     */
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

    /**
     * Builds a {@link JsonNode} from the given key-value pairs.
     *
     * @param keyValues alternating keys and values
     * @return the parsed {@link JsonNode}
     */
    public JsonNode buildJsonNode(final Object... keyValues) {
        final CharSequence json = buildJson(keyValues);
        try {
            return readValue(new CharSequenceReader(json), JsonNode.class);
        } catch (final IOException e) {
            // should not happen since we serialized JSON ourselves
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts the given value to the specified type using Jackson data binding.
     *
     * @param <T> the target type
     * @param value the value to convert
     * @param valueType the target class
     * @return the converted value
     */
    public <T> T convertValue(final Object value, final Class<T> valueType) {
        return getObjectMapper().convertValue(value, valueType);
    }

    /**
     * Converts the given value to a {@code Map<String, Object>}.
     *
     * @param value the value to convert
     * @return a map representation of the value
     */
    public Map<String, Object> convertValueToMap(final Object value) {
        return convertValueToMap(value, String.class, Object.class);
    }

    /**
     * Converts the given value to a map with the specified key and value types.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param value the value to convert
     * @param keyType the class of the map keys
     * @param valueType the class of the map values
     * @return a map representation of the value
     */
    public <K, V> Map<K, V> convertValueToMap(
            final Object value,
            final Class<K> keyType,
            final Class<V> valueType) {
        final MapType type = mapType(LinkedHashMap.class, keyType, valueType);
        return getObjectMapper().convertValue(value, type);
    }

    /**
     * Creates a {@link JsonGenerator} that writes to the given writer, optionally with pretty printing.
     *
     * @param writer the writer to output JSON content to
     * @param pretty whether to enable pretty printing
     * @return a configured {@link JsonGenerator}
     * @throws IOException if the generator cannot be created
     */
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

    /**
     * Returns a new builder initialized from this instance's {@link ObjectMapper}.
     *
     * @return a builder initialized with this instance's mapper
     */
    public ImmutableJackson.Builder from() {
        return from(getObjectMapper());
    }

    /**
     * Returns the default {@link ObjectMapper} to use as a base, or {@code null} if none.
     *
     * @return the default mapper, or {@code null}
     */
    @Nullable
    @Value.Auxiliary
    public abstract ObjectMapper getDefaultMapper();

    /**
     * Returns the map of deserialization features and their enabled/disabled state.
     *
     * @return the deserialization feature map
     */
    @Value.Auxiliary
    public abstract Map<DeserializationFeature, Boolean> getDeserializationFeatures();

    /**
     * Returns the map of generator features and their enabled/disabled state.
     *
     * @return the generator feature map
     */
    @Value.Auxiliary
    public abstract Map<Feature, Boolean> getGeneratorFeatures();

    /**
     * Returns the locale to use for the {@link ObjectMapper}, or {@code null} for the default.
     *
     * @return the locale, or {@code null}
     */
    @Nullable
    public abstract Locale getLocale();

    /**
     * Returns the map of mapper features and their enabled/disabled state.
     *
     * @return the mapper feature map
     */
    public abstract Map<MapperFeature, Boolean> getMapperFeatures();

    /**
     * Returns the set of Jackson modules to register, based on the current configuration flags.
     *
     * @return the set of modules
     */
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

    /**
     * Returns a fully configured {@link ObjectMapper} built from this instance's settings.
     *
     * @return the configured {@link ObjectMapper}
     */
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

    /**
     * Returns a lazily-created {@link ObjectWriter} without pretty printing.
     *
     * @return the {@link ObjectWriter}
     */
    @Value.Lazy
    public ObjectWriter getObjectWriter() {
        return createObjectWriter(getView(), false);
    }

    /**
     * Returns an {@link ObjectWriter} configured with the given view and pretty printing option.
     *
     * @param view the serialization view class, or {@code null} for no view
     * @param pretty whether to enable pretty printing
     * @return the configured {@link ObjectWriter}
     */
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

    /**
     * Returns a lazily-created {@link ObjectWriter} with pretty printing enabled.
     *
     * @return the pretty-printing {@link ObjectWriter}
     */
    @Value.Lazy
    public ObjectWriter getObjectWriterWithPrettyPrinter() {
        return createObjectWriter(getView(), true);
    }

    /**
     * Returns the map of parser features and their enabled/disabled state.
     *
     * @return the parser feature map
     */
    public abstract Map<JsonParser.Feature, Boolean> getParserFeatures();

    /**
     * Returns the map of serialization features and their enabled/disabled state.
     *
     * @return the serialization feature map
     */
    public abstract Map<SerializationFeature, Boolean> getSerializationFeatures();

    /**
     * Returns the serialization inclusion rule, or {@code null} for the default.
     *
     * @return the serialization inclusion, or {@code null}
     */
    @Nullable
    public abstract JsonInclude.Include getSerializationInclusion();

    /**
     * Returns the JSON view class used for serialization and deserialization, or {@code null} for none.
     *
     * @return the view class, or {@code null}
     */
    @Nullable
    public abstract Class<?> getView();

    @Override
    public int hashCode() {
        // use default hash code
        return super.hashCode();
    }

    /**
     * Returns whether properties without a view annotation are included in serialization.
     *
     * @return {@code true} if default view inclusion is enabled
     */
    @Value.Default
    public boolean isDefaultViewInclusion() {
        return true;
    }

    /**
     * Returns whether Jackson should automatically find and register modules via the ServiceLoader.
     *
     * @return {@code true} if modules should be auto-discovered
     */
    @Value.Default
    public boolean isFindAndRegisterModules() {
        return false;
    }

    /**
     * Returns whether pretty printing is enabled, or {@code null} if not specified.
     *
     * @return {@code true} if pretty printing is enabled, {@code false} if disabled, or {@code null}
     */
    @Nullable
    public abstract Boolean isPretty();

    /**
     * Returns whether the date module for ISO-formatted ZonedDateTime should be registered.
     *
     * @return {@code true} if the date module should be registered
     */
    @Value.Default
    public boolean isRegisterDateModule() {
        return true;
    }

    /**
     * Returns whether the enum module for case-insensitive deserialization should be registered.
     *
     * @return {@code true} if the enum module should be registered
     */
    @Value.Default
    public boolean isRegisterEnumModule() {
        return true;
    }

    /**
     * Returns whether the trim module for stripping leading and trailing whitespace should be registered.
     *
     * @return {@code true} if the trim module should be registered
     */
    @Value.Default
    public boolean isRegisterTrimModule() {
        return true;
    }

    /**
     * Constructs a {@link MapType} for the given map class, key type, and value type.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param mapClass the map implementation class
     * @param keyType the class of the map keys
     * @param valueType the class of the map values
     * @return the constructed {@link MapType}
     */
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

    /**
     * Reads a value of the given type from the specified {@link Reader}.
     *
     * @param <T> the target type
     * @param src the reader to read JSON content from
     * @param valueType the target class
     * @return the deserialized value
     * @throws IOException if reading or deserialization fails
     */
    public <T> T readValue(final Reader src, final Class<T> valueType) throws IOException {
        return getObjectMapper().readValue(src, valueType);
    }

    /**
     * Deserializes the given JSON string into an instance of the specified class.
     *
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param type the target class
     * @return the deserialized value, or {@code null} if the input is empty
     * @throws IOException if deserialization fails
     */
    public <T> T readValue(final String json, final Class<T> type) throws IOException {
        return StringUtils.isEmpty(json) ? null : getObjectMapper().readValue(json, type);
    }

    /**
     * Deserializes the given JSON string into an instance of the specified {@link JavaType}.
     *
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param valueType the target {@link JavaType}
     * @return the deserialized value, or {@code null} if the input is empty
     * @throws IOException if deserialization fails
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T> T readValue(final String json, final JavaType valueType) throws IOException {
        return StringUtils.isEmpty(json) ? null : getObjectMapper().readValue(json, valueType);
    }

    /**
     * Deserializes the given JSON string into an instance of the specified {@link TypeReference}.
     *
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param valueType the target {@link TypeReference}
     * @return the deserialized value, or {@code null} if the input is empty
     * @throws IOException if deserialization fails
     */
    public <T> T readValue(final String json, final TypeReference<T> valueType) throws IOException {
        return StringUtils.isEmpty(json) ? null : getObjectMapper().readValue(json, valueType);
    }

    /**
     * Deserializes the given JSON string into a map with the specified key and value types.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param json the JSON string to deserialize
     * @param keyType the class of the map keys
     * @param valueType the class of the map values
     * @return the deserialized map, or an empty map if the input is empty
     * @throws IOException if deserialization fails
     */
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

    /**
     * Generates JSON content via the callback and returns it as a {@link CharSequence}.
     *
     * @param pretty whether to enable pretty printing
     * @param callback the callback that writes JSON content to a generator
     * @return the generated JSON as a {@link CharSequence}
     * @throws UncheckedIOException if an I/O error occurs during generation
     */
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

    /**
     * Generates pretty-printed JSON content via the callback and returns it as a {@link CharSequence}.
     *
     * @param callback the callback that writes JSON content to a generator
     * @return the generated JSON as a {@link CharSequence}
     */
    public CharSequence toCharSequence(final JsonGeneratorCallback callback) {
        return toCharSequence(true, callback);
    }

    /**
     * Serializes the given value to a JSON string using the configured {@link ObjectMapper}.
     *
     * @param value the value to serialize
     * @return the JSON string, or an empty string if the value is {@code null}
     * @throws UncheckedIOException if serialization fails
     */
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

    /**
     * Serializes the given value to a JSON string, optionally with pretty printing.
     *
     * @param value the value to serialize
     * @param pretty whether to enable pretty printing
     * @return the JSON string, or an empty string if the value is {@code null}
     * @throws UncheckedIOException if serialization fails
     */
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
