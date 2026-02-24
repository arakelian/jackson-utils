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
import java.time.ZonedDateTime;

import com.arakelian.core.utils.DateUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Jackson deserializer that converts date strings to {@link ZonedDateTime} using UTC normalization
 * via {@link DateUtils}.
 */
public class ZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {
    /** Constructs a new {@code ZonedDateTimeDeserializer}. */
    public ZonedDateTimeDeserializer() {
        super(ZonedDateTime.class);
    }

    /**
     * Deserializes a JSON string token into a {@link ZonedDateTime} normalized to UTC.
     *
     * @param p the JSON parser
     * @param ctxt the deserialization context
     * @return a {@link ZonedDateTime} in UTC, or throws if the token is not a string
     * @throws IOException if a low-level I/O problem occurs
     * @throws JsonProcessingException if the token is not a string value
     */
    @Override
    public ZonedDateTime deserialize(final JsonParser p, final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        final JsonToken token = p.getCurrentToken();
        if (token == JsonToken.VALUE_STRING) {
            final String str = p.getText().trim();
            return DateUtils.toZonedDateTimeUtc(str);
        }
        throw ctxt.wrongTokenException(p, ZonedDateTime.class, JsonToken.VALUE_STRING, "Expected a string");
    }
}
