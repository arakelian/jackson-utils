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
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import com.arakelian.jackson.MapPath.MapPathSerializer;
import com.arakelian.jackson.model.GeoPoint;
import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

@Value.Immutable
@JsonSerialize(using = MapPathSerializer.class, as = ImmutableMapPath.class)
@JsonDeserialize(builder = ImmutableMapPath.Builder.class)
public abstract class MapPath implements Serializable {
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

    public static final char PATH_SEPARATOR = '/';

    private static final MapPath EMPTY = ImmutableMapPath.builder().build();

    public static MapPath of() {
        return EMPTY;
    }

    public static MapPath of(final Map<?, ?> map) {
        return of(map, null);
    }

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

    public static MapPath of(final String json) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return MapPath.of();
        }
        return of(json, null);
    }

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

    /** ObjectMapper that should be used for deserialization. **/
    @SuppressWarnings("immutables")
    private transient ObjectMapper mapper;

    public <R> R find(final String path, final Function<Object, R> function, final R defaultValue) {
        if (getProperties().size() == 0 || StringUtils.isEmpty(path)) {
            return defaultValue;
        }

        // starting point
        Map map = this.getProperties();

        // traverse path
        final int length = path.length();
        for (int start = length > 0 && path.charAt(0) == PATH_SEPARATOR ? 1 : 0; start < length; start++) {
            final String segment = getSegment(path, start, map);
            start += segment.length();
            final boolean lastSegment = start >= length;

            final Object value = map.get(segment);
            if (lastSegment) {
                return function.apply(value);
            }
            if (value == null) {
                return defaultValue;
            }
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("Expected \"" + path.substring(0, start) + "\" of path \""
                        + path + "\" to resolve to Map but was " + value.getClass().getSimpleName());
            }

            map = Map.class.cast(value);
        }
        return defaultValue;
    }

    public <T> T get(final String path, final Class<T> clazz) {
        return get(path, clazz, null);
    }

    public <T> T get(final String path, final Class<T> clazz, final T defaultValue) {
        Preconditions.checkArgument(clazz != null, "clazz must be non-null");
        final T result = find(path, value -> {
            return value != null ? getObjectMapper().convertValue(value, clazz) : defaultValue;
        }, defaultValue);
        return result;
    }

    public Double getDouble(final String path) {
        return get(path, Double.class);
    }

    public Float getFloat(final String path) {
        return get(path, Float.class);
    }

    public GeoPoint getGeoPoint(final String path) {
        return get(path, GeoPoint.class);
    }

    public Integer getInt(final String path) {
        return get(path, Integer.class);
    }

    public List getList(final String path) {
        return get(path, List.class);
    }

    public Long getLong(final String path) {
        return get(path, Long.class);
    }

    public Map getMap(final String path) {
        return get(path, Map.class);
    }

    public MapPath getMapPath(final String path) {
        return MapPath.of(getMap(path), getObjectMapper());
    }

    public Object getObject(final String path) {
        return get(path, Object.class);
    }

    @JsonIgnore
    @Value.Lazy
    public ObjectMapper getObjectMapper() {
        if (mapper == null) {
            mapper = JacksonUtils.getObjectMapper();
        }
        return mapper;
    }

    @JsonAnyGetter
    @Value.Default
    public Map<Object, Object> getProperties() {
        return ImmutableMap.of();
    }

    public String getString(final String path) {
        return get(path, String.class);
    }

    public ZonedDateTime getZonedDateTime(final String path) {
        return get(path, ZonedDateTime.class);
    }

    public boolean hasProperty(final String path) {
        return find(path, value -> value != null, false);
    }

    public void setObjectMapper(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    protected String getSegment(final String path, final int start, final Map map) {
        final int length = path.length();

        for (int i = start; i < length; i++) {
            final char ch = path.charAt(i);
            if (ch == '/' || ch == '.') {
                final String segment = path.substring(start, i);
                if (map.containsKey(segment)) {
                    return segment;
                }
            }
        }

        return path.substring(start);
    }
}
