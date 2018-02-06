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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.arakelian.jackson.model.GeoPoint;
import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class MapPath {
    public static final char PATH_SEPARATOR = '/';

    private static final MapPath EMPTY = new MapPath(ImmutableMap.of());

    public static MapPath of() {
        return EMPTY;
    }

    public static MapPath of(final Map map) {
        if (map == null || map.size() == 0) {
            return MapPath.of();
        }
        return new MapPath(map);
    }

    public static MapPath of(final Map map, final ObjectMapper mapper) {
        if (map == null || map.size() == 0) {
            return MapPath.of();
        }
        return new MapPath(map, mapper);
    }

    public static MapPath of(final String json) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return MapPath.of();
        }
        return new MapPath(json);
    }

    public static MapPath of(final String json, final ObjectMapper mapper) throws IOException {
        if (StringUtils.isEmpty(json)) {
            return MapPath.of();
        }
        return new MapPath(json, mapper);
    }

    private final Map map;

    private final ObjectMapper mapper;

    private MapPath(final Map map) {
        this(map, map != null && map.size() != 0 ? JacksonUtils.getObjectMapper() : null);
    }

    private MapPath(final Map map, final ObjectMapper mapper) {
        Preconditions
                .checkArgument(map == null || map.size() == 0 || mapper != null, "mapper must be non-null");
        this.map = map;
        this.mapper = mapper;
    }

    private MapPath(final String json) throws IOException {
        this(json, JacksonUtils.getObjectMapper());
    }

    private MapPath(final String json, final ObjectMapper mapper) throws IOException {
        this(JacksonUtils.readValue(json, Map.class), mapper);
    }

    public <R> R find(final String path, final Function<Object, R> function, final R defaultValue) {
        if (map == null || map.size() == 0 || StringUtils.isEmpty(path)) {
            return defaultValue;
        }

        // starting point
        Map map = this.map;

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
            return value != null ? mapper.convertValue(value, clazz) : defaultValue;
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

    public Object getObject(final String path) {
        return get(path, Object.class);
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
