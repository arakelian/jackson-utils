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
import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.arakelian.core.utils.MoreStringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Adaptation of {@link StringDeserializer} to support trimming. Unfortunately, original class is
 * final so we have to substantially copy that code here.
 */
public class TrimWhitespaceDeserializer extends StdScalarDeserializer<String> {
    private static final long serialVersionUID = 1L;

    public final static TrimWhitespaceDeserializer SINGLETON = new TrimWhitespaceDeserializer();

    private TrimWhitespaceDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(final JsonParser p, final DeserializationContext context) throws IOException {
        // delegate to code copied from StringDeserializer (because of final methods!)
        final String raw = doDeserialize(p, context);

        // remove leading and trailing whitespace, including newlines, tabs, etc.
        String value = MoreStringUtils.trimWhitespace(raw);

        // normalize Unicode message to NFC form
        value = value != null ? Normalizer.normalize(value, Form.NFC) : null;
        return value;
    }

    @Override
    public String deserializeWithType(
            final JsonParser p,
            final DeserializationContext ctxt,
            final TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, ctxt);
    }

    private String doDeserialize(final JsonParser p, final DeserializationContext context)
            throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return p.getText();
        }

        final JsonToken t = p.getCurrentToken();
        if (t == JsonToken.START_ARRAY
                && context.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final String parsed = _parseString(p, context);
            if (p.nextToken() != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, context);
            }
            return parsed;
        }

        // need to gracefully handle byte[] data, as base64
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            final Object ob = p.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (ob instanceof byte[]) {
                return context.getBase64Variant().encode((byte[]) ob, false);
            }
            // otherwise, try conversion using toString()...
            return ob.toString();
        }

        // allow coercions for other scalar types
        final String text = p.getValueAsString();
        if (text != null) {
            return text;
        }

        // could not perform coercion
        return (String) context.handleUnexpectedToken(_valueClass, p);
    }

    @Override
    public boolean isCachable() {
        return true;
    }
}
