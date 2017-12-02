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

import com.arakelian.core.utils.MoreStringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

/**
 * Modifies Jackson deserialization of {@link Enum} types so that it forces input to uppercase
 * before attempt to convert to Enumeration.
 */
public final class EnumUppercaseDeserializerModifier extends BeanDeserializerModifier {
    @Override
    public JsonDeserializer<Enum> modifyEnumDeserializer(final DeserializationConfig config,
            final JavaType type, final BeanDescription beanDesc, final JsonDeserializer<?> deserializer) {
        return new JsonDeserializer<Enum>() {
            @SuppressWarnings("unchecked")
            @Override
            public Enum deserialize(final JsonParser jp, final DeserializationContext ctxt)
                    throws IOException {
                final Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();
                final String trimmed = MoreStringUtils.trimWhitespace(jp.getValueAsString());
                if (trimmed == null || trimmed.length() == 0) {
                    return null;
                }
                try {
                    // try original value
                    return Enum.valueOf(rawClass, trimmed);
                } catch (final IllegalArgumentException e) {
                    try {
                        // try uppercase
                        return Enum.valueOf(rawClass, trimmed.toUpperCase());
                    } catch (final IllegalArgumentException e2) {
                        // try lowercase
                        return Enum.valueOf(rawClass, trimmed.toLowerCase());
                    }
                }
            }
        };
    }
}
