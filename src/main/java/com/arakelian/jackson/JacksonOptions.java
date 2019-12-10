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

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.immutables.value.Value;

import com.arakelian.core.feature.Nullable;
import com.arakelian.jackson.databind.EnumUppercaseDeserializerModifier;
import com.arakelian.jackson.databind.TrimWhitespaceDeserializer;
import com.arakelian.jackson.databind.ZonedDateTimeDeserializer;
import com.arakelian.jackson.databind.ZonedDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Value.Immutable
public abstract class JacksonOptions {
    public ObjectMapper configure(final ObjectMapper mapper) {
        // set locale
        mapper.setLocale(getLocale());

        // register modules
        if (isFindAndRegisterModules()) {
            mapper.findAndRegisterModules();
        }

        for (final Module module : getModules()) {
            mapper.registerModule(module);
        }

        mapper.setSerializationInclusion(getSerializationInclusion());

        final Map<Feature, Boolean> features = getFeatures();
        for (final Feature feature : features.keySet()) {
            mapper.configure(feature, features.get(feature).booleanValue());
        }

        final Map<SerializationFeature, Boolean> serializationFeatures = getSerializationFeatures();
        for (final SerializationFeature feature : serializationFeatures.keySet()) {
            mapper.configure(feature, serializationFeatures.get(feature).booleanValue());
        }

        final Map<DeserializationFeature, Boolean> deserializationFeatures = getDeserializationFeatures();
        for (final DeserializationFeature feature : deserializationFeatures.keySet()) {
            mapper.configure(feature, deserializationFeatures.get(feature).booleanValue());
        }

        for (final MapperFeature feature : getEnabled()) {
            mapper.enable(feature);
        }

        for (final MapperFeature feature : getDisabled()) {
            mapper.disable(feature);
        }

        final Class<?> view = getView();
        if (view != null) {
            mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        }

        return mapper;
    }

    @Nullable
    public abstract ObjectMapper getDefaultMapper();

    @Value.Default
    public Map<DeserializationFeature, Boolean> getDeserializationFeatures() {
        final ImmutableMap.Builder<DeserializationFeature, Boolean> map = ImmutableMap.builder();

        // seems reasonable to allow caller to pass single value for an array
        map.put(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // we do want to enforce some strict policies on other "bad" data
        map.put(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
        map.put(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        map.put(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        return map.build();
    }

    public abstract Set<MapperFeature> getDisabled();

    public abstract Set<MapperFeature> getEnabled();

    @Value.Default
    public Map<Feature, Boolean> getFeatures() {
        final ImmutableMap.Builder<Feature, Boolean> map = ImmutableMap.builder();

        // we don't want scientific notation used for big decimals
        map.put(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        return map.build();
    }

    @Value.Default
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Value.Default
    public Set<Module> getModules() {
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

        return ImmutableSet.of(trimWhitespace, forceEnumsUppercase, javaDates);
    }

    @Value.Lazy
    public ObjectMapper getObjectMapper() {
        final ObjectMapper defaultMapper = getDefaultMapper();
        final ObjectMapper mapper = defaultMapper != null ? defaultMapper.copy() : new ObjectMapper();
        return configure(mapper);
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

    @SuppressWarnings("deprecation")
    @Value.Default
    public Map<SerializationFeature, Boolean> getSerializationFeatures() {
        final ImmutableMap.Builder<SerializationFeature, Boolean> map = ImmutableMap.builder();
        map.put(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        map.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        map.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // caller can optionally request pretty formatting
        if (isPretty()) {
            map.put(SerializationFeature.INDENT_OUTPUT, true);
        }

        return map.build();
    }

    @Value.Default
    public JsonInclude.Include getSerializationInclusion() {
        return JsonInclude.Include.NON_EMPTY;
    }

    @Nullable
    public abstract Class<?> getView();

    @Value.Default
    public boolean isFindAndRegisterModules() {
        return true;
    }

    @Value.Default
    public boolean isPretty() {
        return true;
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
}
