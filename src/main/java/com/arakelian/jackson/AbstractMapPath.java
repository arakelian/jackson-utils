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

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import com.arakelian.jackson.model.GeoPoint;
import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Base class for navigating nested {@link Map} structures using slash-separated or dot-separated
 * paths. Provides typed accessors for retrieving values at a given path, with support for default
 * values and automatic type conversion via Jackson's {@link ObjectMapper}.
 */
public abstract class AbstractMapPath implements Serializable {
    public static final char PATH_SEPARATOR = '/';

    /** ObjectMapper that should be used for deserialization. **/
    @SuppressWarnings("immutables")
    private transient ObjectMapper mapper;

    /**
     * Traverses the property map along the given path and applies the function to the value found
     * at the terminal segment.
     *
     * @param <R>          the return type
     * @param path         slash or dot separated path to navigate
     * @param function     function to apply to the resolved value
     * @param defaultValue value to return when the path cannot be resolved
     * @return the result of applying the function, or the default value
     */
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

    /**
     * Returns the first element at the given path, converted to the specified type. If the value is
     * a collection, the first element is returned.
     *
     * @param <T>   the target type
     * @param path  path to navigate
     * @param clazz target type class
     * @return the converted value, or {@code null} if not found
     */
    public <T> T first(final String path, final Class<T> clazz) {
        return first(path, clazz, null);
    }

    /**
     * Returns the first element at the given path, converted to the specified type, or the default
     * value if not found.
     *
     * @param <T>          the target type
     * @param path         path to navigate
     * @param clazz        target type class
     * @param defaultValue value to return when the path cannot be resolved
     * @return the converted value, or the default value
     */
    public <T> T first(final String path, final Class<T> clazz, final T defaultValue) {
        Preconditions.checkArgument(clazz != null, "clazz must be non-null");
        final T result = find(path, value -> {
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Collection<?> c) {
                if (c.size() == 0) {
                    return defaultValue;
                }
                return getObjectMapper().convertValue(c.iterator().next(), clazz);
            }
            return getObjectMapper().convertValue(value, clazz);
        }, defaultValue);
        return result;
    }

    /** Returns the first {@link Double} value at the given path, or {@code null} if not found. */
    public Double firstDouble(final String path) {
        return firstDouble(path, null);
    }

    /** Returns the first {@link Double} value at the given path, or the default value. */
    public Double firstDouble(final String path, final Double defaultValue) {
        return first(path, Double.class, defaultValue);
    }

    /** Returns the first {@link Integer} value at the given path, or {@code null} if not found. */
    public Integer firstInt(final String path) {
        return firstInt(path, null);
    }

    /** Returns the first {@link Integer} value at the given path, or the default value. */
    public Integer firstInt(final String path, final Integer defaultValue) {
        return first(path, Integer.class, defaultValue);
    }

    /** Returns the first {@link Long} value at the given path, or {@code null} if not found. */
    public Long firstLong(final String path) {
        return firstLong(path, null);
    }

    /** Returns the first {@link Long} value at the given path, or the default value. */
    public Long firstLong(final String path, final Long defaultValue) {
        return first(path, Long.class, defaultValue);
    }

    /** Returns the first {@link String} value at the given path, or {@code null} if not found. */
    public String firstString(final String path) {
        return firstString(path, null);
    }

    /** Returns the first {@link String} value at the given path, or the default value. */
    public String firstString(final String path, final String defaultValue) {
        return first(path, String.class, defaultValue);
    }

    /**
     * Returns the value at the given path, converted to the specified type.
     *
     * @param <T>   the target type
     * @param path  path to navigate
     * @param clazz target type class
     * @return the converted value, or {@code null} if not found
     */
    public <T> T get(final String path, final Class<T> clazz) {
        return get(path, clazz, null);
    }

    /**
     * Returns the value at the given path, converted to the specified type, or the default value.
     *
     * @param <T>          the target type
     * @param path         path to navigate
     * @param clazz        target type class
     * @param defaultValue value to return when the path cannot be resolved
     * @return the converted value, or the default value
     */
    public <T> T get(final String path, final Class<T> clazz, final T defaultValue) {
        Preconditions.checkArgument(clazz != null, "clazz must be non-null");
        final T result = find(path, value -> {
            return value != null ? getObjectMapper().convertValue(value, clazz) : defaultValue;
        }, defaultValue);
        return result;
    }

    /** Returns the {@link Double} value at the given path. */
    public Double getDouble(final String path) {
        return get(path, Double.class);
    }

    /** Returns the {@link Float} value at the given path. */
    public Float getFloat(final String path) {
        return get(path, Float.class);
    }

    /** Returns the {@link GeoPoint} value at the given path. */
    public GeoPoint getGeoPoint(final String path) {
        return get(path, GeoPoint.class);
    }

    /** Returns the {@link Integer} value at the given path. */
    public Integer getInt(final String path) {
        return get(path, Integer.class);
    }

    /** Returns the {@link List} value at the given path. */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final String path) {
        final List list = get(path, List.class);
        return list;
    }

    /** Returns the {@link Long} value at the given path. */
    public Long getLong(final String path) {
        return get(path, Long.class);
    }

    /** Returns the {@link Map} value at the given path. */
    public Map getMap(final String path) {
        return get(path, Map.class);
    }

    /** Returns the raw {@link Object} value at the given path. */
    public Object getObject(final String path) {
        return get(path, Object.class);
    }

    /** Returns the {@link ObjectMapper} used for type conversions. */
    @JsonIgnore
    @Value.Lazy
    public ObjectMapper getObjectMapper() {
        if (mapper == null) {
            mapper = JacksonUtils.getObjectMapper();
        }
        return mapper;
    }

    /** Returns the underlying property map. */
    @JsonAnyGetter
    @Value.Default
    public Map<Object, Object> getProperties() {
        return ImmutableMap.of();
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

    /** Returns the {@link String} value at the given path. */
    public String getString(final String path) {
        return get(path, String.class);
    }

    /** Returns the {@link ZonedDateTime} value at the given path. */
    public ZonedDateTime getZonedDateTime(final String path) {
        return get(path, ZonedDateTime.class);
    }

    /** Returns {@code true} if a non-null value exists at the given path. */
    public boolean hasProperty(final String path) {
        return find(path, value -> value != null, false);
    }

    /** Sets the {@link ObjectMapper} to use for type conversions. */
    public void setObjectMapper(final ObjectMapper mapper) {
        this.mapper = mapper;
    }
}
