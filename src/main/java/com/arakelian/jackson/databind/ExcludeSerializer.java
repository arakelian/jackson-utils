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

package com.arakelian.jackson.databind;

import java.io.IOException;
import java.util.Set;

import com.arakelian.jackson.FilteringJsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class ExcludeSerializer<T> extends JsonSerializer<T> {
    private final Class<T> handledType;
    private final Set<String> excludes;
    private final JsonSerializer<Object> delegate;

    public ExcludeSerializer(final Class<T> handledType, final String... excludes) {
        this(handledType, null, excludes);
    }

    public ExcludeSerializer(
            final Class<T> handledType,
            final JsonSerializer<Object> delegate,
            final String... excludes) {
        this.handledType = Preconditions.checkNotNull(handledType);
        this.delegate = delegate;
        this.excludes = ImmutableSet.copyOf(excludes);
    }

    @Override
    public Class<T> handledType() {
        return handledType;
    }

    @Override
    public void serialize(final T value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        final FilteringJsonGenerator filtering = new FilteringJsonGenerator(gen, null, excludes);
        if (value == null || delegate == null) {
            serializers.defaultSerializeValue(value, filtering);
        } else {
            delegate.serialize(value, filtering, serializers);
        }
    }
}
