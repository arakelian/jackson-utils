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

/**
 * Static utility methods that delegate to the default {@link Jackson} instance for common JSON
 * operations like reading, writing, and converting values.
 */
public class JacksonUtils {
    /** Default ObjectMapper **/
    private static Jackson JACKSON = Jackson.of();

    /**
     * Builds a JSON string from the given key-value pairs.
     *
     * @param keyValues alternating keys and values
     * @return a JSON string representation of the key-value pairs
     */
    public static CharSequence buildJson(final Object... keyValues) {
        return JACKSON.buildJson(keyValues);
    }

    /**
     * Builds a {@link JsonNode} from the given key-value pairs.
     *
     * @param keyValues alternating keys and values
     * @return a {@link JsonNode} representation of the key-value pairs
     */
    public static JsonNode buildJsonNode(final Object... keyValues) {
        return JACKSON.buildJsonNode(keyValues);
    }

    /**
     * Converts the given value to the specified type using Jackson data binding.
     *
     * @param <T> the target type
     * @param value the value to convert
     * @param type the target class
     * @return the converted value
     */
    public static <T> T convertValue(final Object value, final Class<T> type) {
        return JACKSON.convertValue(value, type);
    }

    /**
     * Converts the given value to a {@code Map<String, Object>}.
     *
     * @param value the value to convert
     * @return the value as a map
     */
    public static Map<String, Object> convertValueToMap(final Object value) {
        return JACKSON.convertValueToMap(value);
    }

    /**
     * Returns the default {@link ObjectMapper}.
     *
     * @return a copy of the default {@link ObjectMapper}
     */
    public static ObjectMapper getObjectMapper() {
        // always return copy because ObjectMapper is mutable
        return JACKSON.getObjectMapper();
    }

    /**
     * Returns an {@link ObjectWriter} with the specified pretty printing option.
     *
     * @param pretty {@code true} to enable pretty printing
     * @return an {@link ObjectWriter} instance
     */
    public static ObjectWriter getObjectWriter(final boolean pretty) {
        return JACKSON.getObjectWriter(JACKSON.getView(), pretty);
    }

    /**
     * Returns an {@link ObjectWriter} configured with the given JSON view.
     *
     * @param view the JSON view class
     * @return an {@link ObjectWriter} instance
     */
    public static ObjectWriter getObjectWriter(final Class<?> view) {
        return JACKSON.getObjectWriter(view, false);
    }

    /**
     * Returns an {@link ObjectWriter} configured with the given JSON view and pretty printing option.
     *
     * @param view the JSON view class
     * @param pretty {@code true} to enable pretty printing
     * @return an {@link ObjectWriter} instance
     */
    public static ObjectWriter getObjectWriter(final Class<?> view, final boolean pretty) {
        return JACKSON.getObjectWriter(view, pretty);
    }

    /**
     * Deserializes the given JSON string into an instance of the specified class.
     *
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param valueType the target class
     * @return the deserialized value
     * @throws IOException if the JSON cannot be parsed
     */
    public static <T> T readValue(final String json, final Class<T> valueType) throws IOException {
        return JACKSON.readValue(json, valueType);
    }

    /**
     * Deserializes the given JSON string into an instance of the specified {@link JavaType}.
     *
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param valueType the target {@link JavaType}
     * @return the deserialized value
     * @throws IOException if the JSON cannot be parsed
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T readValue(final String json, final JavaType valueType) throws IOException {
        return JACKSON.readValue(json, valueType);
    }

    /**
     * Deserializes the given JSON string into an instance of the specified {@link TypeReference}.
     *
     * @param <T> the target type
     * @param json the JSON string to deserialize
     * @param valueType the target {@link TypeReference}
     * @return the deserialized value
     * @throws IOException if the JSON cannot be parsed
     */
    public static <T> T readValue(final String json, final TypeReference<T> valueType) throws IOException {
        return JACKSON.readValue(json, valueType);
    }

    /**
     * Deserializes the given JSON string into a map with the specified key and value types.
     *
     * @param <K> the map key type
     * @param <V> the map value type
     * @param json the JSON string to deserialize
     * @param keyType the class of the map keys
     * @param valueType the class of the map values
     * @return the deserialized map
     * @throws IOException if the JSON cannot be parsed
     */
    public static <K, V> Map<K, V> readValueAsMap(
            final String json,
            final Class<K> keyType,
            final Class<V> valueType) throws IOException {
        return JACKSON.readValueAsMap(json, keyType, valueType);
    }

    /**
     * Generates JSON content via the callback and returns it as a {@link CharSequence}.
     *
     * @param pretty {@code true} to enable pretty printing
     * @param callback the callback that writes JSON content
     * @return the generated JSON as a {@link CharSequence}
     * @throws UncheckedIOException if an I/O error occurs during generation
     */
    public static CharSequence toCharSequence(final boolean pretty, final JsonGeneratorCallback callback)
            throws UncheckedIOException {
        return JACKSON.toCharSequence(pretty, callback);
    }

    /**
     * Generates JSON content via the callback and returns it as a {@link String}.
     *
     * @param pretty {@code true} to enable pretty printing
     * @param callback the callback that writes JSON content
     * @return the generated JSON as a {@link String}
     * @throws UncheckedIOException if an I/O error occurs during generation
     */
    public static String toString(final boolean pretty, final JsonGeneratorCallback callback)
            throws UncheckedIOException {
        return Objects.toString(JACKSON.toCharSequence(pretty, callback), null);
    }

    /**
     * Serializes the given value to a JSON string, optionally with pretty printing.
     *
     * @param value the value to serialize
     * @param pretty {@code true} to enable pretty printing
     * @return the JSON string representation
     * @throws UncheckedIOException if a serialization error occurs
     */
    public static String toString(final Object value, final boolean pretty) throws UncheckedIOException {
        return JACKSON.toString(value, pretty);
    }

    /**
     * Serializes the given value to a JSON string, suppressing serialization errors.
     *
     * @param value the value to serialize
     * @param pretty {@code true} to enable pretty printing
     * @return the JSON string representation
     * @throws UncheckedIOException if a serialization error occurs
     */
    public static String toStringSafe(final Object value, final boolean pretty) throws UncheckedIOException {
        return JACKSON.toString(value, pretty);
    }

    /**
     * Recursively traverses a {@link JsonNode} tree, invoking the consumer on matching leaf nodes.
     *
     * @param node the root node to traverse
     * @param fieldPredicate a predicate to filter object field names, or {@code null} to accept all
     * @param consumer the consumer invoked on each matching leaf node
     */
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

    /**
     * Returns a new {@link ObjectMapper} configured with the specified JSON view.
     *
     * @param mapper the source {@link ObjectMapper}
     * @param view the JSON view class
     * @return a new {@link ObjectMapper} configured with the given view
     */
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
