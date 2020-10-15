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

public abstract class AbstractMapPath implements Serializable {
    public static final char PATH_SEPARATOR = '/';

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

    public <T> T first(final String path, final Class<T> clazz) {
        return first(path, clazz, null);
    }

    public <T> T first(final String path, final Class<T> clazz, final T defaultValue) {
        Preconditions.checkArgument(clazz != null, "clazz must be non-null");
        final T result = find(path, value -> {
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Collection) {
                final Collection c = (Collection) value;
                if (c.size() == 0) {
                    return defaultValue;
                }
                return getObjectMapper().convertValue(c.iterator().next(), clazz);
            }
            return getObjectMapper().convertValue(value, clazz);
        }, defaultValue);
        return result;
    }

    public Double firstDouble(final String path) {
        return firstDouble(path, null);
    }

    public Double firstDouble(final String path, final Double defaultValue) {
        return first(path, Double.class, defaultValue);
    }

    public Integer firstInt(final String path) {
        return firstInt(path, null);
    }

    public Integer firstInt(final String path, final Integer defaultValue) {
        return first(path, Integer.class, defaultValue);
    }

    public Long firstLong(final String path) {
        return firstLong(path, null);
    }

    public Long firstLong(final String path, final Long defaultValue) {
        return first(path, Long.class, defaultValue);
    }

    public String firstString(final String path) {
        return firstString(path, null);
    }

    public String firstString(final String path, final String defaultValue) {
        return first(path, String.class, defaultValue);
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

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(final String path) {
        final List list = get(path, List.class);
        return list;
    }

    public Long getLong(final String path) {
        return get(path, Long.class);
    }

    public Map getMap(final String path) {
        return get(path, Map.class);
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
}
