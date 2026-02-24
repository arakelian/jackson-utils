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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import com.arakelian.jackson.MapPath.MapPathSerializer;
import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Immutable implementation of {@link AbstractMapPath} that wraps a {@link Map} and provides
 * path-based navigation with typed accessors. Instances are created via the static {@code of}
 * factory methods from an existing map or a JSON string.
 */
@Value.Immutable
@JsonSerialize(using = MapPathSerializer.class, as = ImmutableMapPath.class)
@JsonDeserialize(builder = ImmutableMapPath.Builder.class)
public abstract class MapPath extends AbstractMapPath {
    /**
     * Customize serializer that prevents 'empty' MapPath objects from being serialized
     */
    public static class MapPathSerializer extends StdSerializer<MapPath> {
        public MapPathSerializer() {
            super(MapPath.class);
        }

        @Override
        public boolean isEmpty(final SerializerProvider provider, final MapPath value) {
            // we don't want to serialize 'empty' MapPath objects
            return value == null || value.getProperties().isEmpty();
        }

        @Override
        public void serialize(final MapPath value, final JsonGenerator gen, final SerializerProvider provider)
                throws IOException {
            final Map<Object, Object> props = value.getProperties();
            provider.defaultSerializeValue(props, gen);
        }
    }

    private static final class EmptyHolder {
        static final MapPath INSTANCE = ImmutableMapPath.builder().build();
    }

    /** Returns an empty {@link MapPath} with no properties. */
    public static MapPath of() {
        return EmptyHolder.INSTANCE;
    }

    /**
     * Creates a {@link MapPath} from the given map using the default {@link ObjectMapper}.
     *
     * @param map the property map, or {@code null} for an empty instance
     * @return a new {@link MapPath} wrapping the map
     */
    public static MapPath of(final Map<?, ?> map) {
        return of(map, null);
    }

    /**
     * Creates a {@link MapPath} from the given map using the specified {@link ObjectMapper}.
     *
     * @param map    the property map, or {@code null} for an empty instance
     * @param mapper the {@link ObjectMapper} to use for type conversions, or {@code null} for the default
     * @return a new {@link MapPath} wrapping the map
     */
    public static MapPath of(final Map<?, ?> map, final ObjectMapper mapper) {
        if (map == null || map.size() == 0) {
            return MapPath.of();
        }
        final ImmutableMapPath mapPath = ImmutableMapPath.builder() //
                .putAllProperties(map) //
                .build();
        mapPath.setObjectMapper(mapper);
        return mapPath;
    }

    /**
     * Creates a {@link MapPath} by parsing the given JSON string using the default {@link ObjectMapper}.
     *
     * @param json the JSON string to parse, or {@code null}/empty for an empty instance
     * @return a new {@link MapPath} backed by the parsed map
     * @throws IOException if the JSON cannot be parsed
     */
    public static MapPath of(final String json) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return MapPath.of();
        }
        return of(json, null);
    }

    /**
     * Creates a {@link MapPath} by parsing the given JSON string using the specified {@link ObjectMapper}.
     *
     * @param json   the JSON string to parse, or {@code null}/empty for an empty instance
     * @param mapper the {@link ObjectMapper} to use for parsing and type conversions, or {@code null} for the default
     * @return a new {@link MapPath} backed by the parsed map
     * @throws IOException if the JSON cannot be parsed
     */
    public static MapPath of(final String json, ObjectMapper mapper) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return MapPath.of();
        }
        if (mapper == null) {
            mapper = JacksonUtils.getObjectMapper();
        }
        final Map<?, ?> map = mapper.readValue(json, Map.class);
        final ImmutableMapPath mapPath = ImmutableMapPath.builder() //
                .putAllProperties(map) //
                .build();
        mapPath.setObjectMapper(mapper);
        return mapPath;
    }

    /**
     * Returns the nested {@link Map} at the given path wrapped as a new {@link MapPath}.
     *
     * @param path the slash or dot separated path to navigate
     * @return a {@link MapPath} wrapping the nested map, or an empty instance if the path does not resolve
     */
    public MapPath getMapPath(final String path) {
        return MapPath.of(getMap(path), getObjectMapper());
    }
}
