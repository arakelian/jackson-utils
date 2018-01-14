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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;

public final class JacksonProcessors {
    private final ObjectMapper mapper;
    private final ObjectWriter writer;

    public JacksonProcessors(final ObjectMapper mapper, final ObjectWriter writer) {
        Preconditions.checkArgument(mapper != null, "mapper must be non-null");
        Preconditions.checkArgument(writer != null, "writer must be non-null");
        this.mapper = mapper;
        this.writer = writer;
    }

    public final ObjectMapper mapper() {
        return mapper;
    }

    public String toString(final Object value) throws JsonProcessingException {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        return writer.writeValueAsString(value);
    }

    public final ObjectWriter writer() {
        return writer;
    }
}
