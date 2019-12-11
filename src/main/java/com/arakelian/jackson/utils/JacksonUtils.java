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
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.arakelian.jackson.model.Jackson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JacksonUtils {
    /** Default ObjectMapper **/
    private static Jackson JACKSON = Jackson.of();

    public static CharSequence buildJson(final Object... keyValues) {
        return JACKSON.buildJson(keyValues);
    }

    public static JsonNode buildJsonNode(final Object... keyValues) {
        return JACKSON.buildJsonNode(keyValues);
    }

    public static <T> T convertValue(final Object value, final Class<T> type) {
        return JACKSON.convertValue(value, type);
    }

    public static Map<String, Object> convertValueToMap(final Object value) {
        return JACKSON.convertValueToMap(value);
    }

    public static ObjectMapper getObjectMapper() {
        // always return copy because ObjectMapper is mutable
        return JACKSON.getObjectMapper();
    }

    public static ObjectWriter getObjectWriter(final boolean pretty) {
        return JACKSON.getObjectWriter(JACKSON.getView(), pretty);
    }

    public static ObjectWriter getObjectWriter(final Class<?> view) {
        return JACKSON.getObjectWriter(view, false);
    }

    public static ObjectWriter getObjectWriter(final Class<?> view, final boolean pretty) {
        return JACKSON.getObjectWriter(view, pretty);
    }

    public static <T> T readValue(final String json, final Class<T> valueType) throws IOException {
        return JACKSON.readValue(json, valueType);
    }

    public static <T> T readValue(final String json, final JavaType valueType) throws IOException {
        return JACKSON.readValue(json, valueType);
    }

    public static <T> T readValue(final String json, final TypeReference<T> valueType) throws IOException {
        return JACKSON.readValue(json, valueType);
    }

    public static <K, V> Map<K, V> readValueAsMap(
            final String json,
            final Class<K> keyType,
            final Class<V> valueType) throws IOException {
        return JACKSON.readValueAsMap(json, keyType, valueType);
    }

    public static CharSequence toCharSequence(final boolean pretty, final JsonGeneratorCallback callback)
            throws UncheckedIOException {
        return JACKSON.toCharSequence(pretty, callback);
    }

    public static String toString(final boolean pretty, final JsonGeneratorCallback callback)
            throws UncheckedIOException {
        return Objects.toString(JACKSON.toCharSequence(pretty, callback), null);
    }

    public static String toString(final Object value, final boolean pretty) throws UncheckedIOException {
        return JACKSON.toString(value, pretty);
    }

    public static String toStringSafe(final Object value, final boolean pretty) throws UncheckedIOException {
        return JACKSON.toString(value, pretty);
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
        return Jackson.from(mapper) //
                .view(view) //
                .build() //
                .getObjectMapper();
    }

    private JacksonUtils() {
        // utility class
    }
}
