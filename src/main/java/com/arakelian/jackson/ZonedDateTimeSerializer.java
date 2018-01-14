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

import com.arakelian.core.utils.DateUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {
    public ZonedDateTimeSerializer() {
        super(ZonedDateTime.class);
    }

    /**
     * Serializes the given <code>ZonedDateTime</code> in a format that is readable by commonly use
     * libraries.
     *
     * @see <a href=
     *      "http://www.joda.org/joda-time/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateOptionalTimeParser--">dateOptionalTimeParser</a>
     * @see <a href=
     *      "https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-date-format.html#mapping-date-format">strict_date_optional_time</a>
     */
    @Override
    public void serialize(
            final ZonedDateTime date,
            final JsonGenerator generator,
            final SerializerProvider provider) throws IOException, JsonProcessingException {
        generator.writeString(date != null ? DateUtils.toStringIsoFormat(date) : null);
    }
}
