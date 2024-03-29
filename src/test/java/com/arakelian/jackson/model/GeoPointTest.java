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

package com.arakelian.jackson.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.arakelian.core.utils.SerializableTestUtils;
import com.arakelian.jackson.utils.JacksonTestUtils;
import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class GeoPointTest {
    public static final GeoPoint POINT = ImmutableGeoPoint.builder() //
            .lat(41.12d) //
            .lon(-71.34d) //
            .build();

    @Test
    public void testCommaSeparatedString() {
        final GeoPoint point = GeoPoint.of("41.12,-71.34");
        assertEquals(41.12d, point.getLat(), GeoPoint.DEFAULT_ERROR);
        assertEquals(-71.34d, point.getLon(), GeoPoint.DEFAULT_ERROR);
    }

    @Test
    public void testDecimalRounding() {
        Assertions.assertEquals(51.009830d, GeoPoint.round(51.00982963107526d, 6), 0.000001d);
        Assertions.assertEquals(51.0098296d, GeoPoint.round(51.00982963107526d, 7), 0.0000001d);
        Assertions.assertEquals(51.00982963d, GeoPoint.round(51.00982963107526d, 8), 0.00000001d);
    }

    @Test
    public void testEmpty() throws JsonMappingException, JsonProcessingException {
        assertNull(GeoPoint.of(null));
        assertNull(GeoPoint.of(""));
        assertNull(convertValueToGeoPoint("null"));
        assertNull(convertValueToGeoPoint("\"\""));
    }

    @Test
    public void testGeohash() throws IOException {
        final GeoPoint point = assertConvertValue("\"drm3btev3e86\"", 41.12d, -71.34d);
        assertEquals(POINT, point.round(6));
        assertEquals("drm3btev3e86", point.getGeohash());
    }

    @Test
    public void testGeoPointAsArray() throws IOException {
        assertConvertValue("[ -71.34, 41.12 ]", 41.12d, -71.34d);
        assertThrows(MismatchedInputException.class, () -> {
            convertValueToGeoPoint("[ \"not\", \"number\" ]");
        });
        assertThrows(MismatchedInputException.class, () -> {
            convertValueToGeoPoint("[ 71.34 ]");
        });
        assertThrows(MismatchedInputException.class, () -> {
            convertValueToGeoPoint("[ 1,2,3 ]");
        });
    }

    @Test
    public void testGeoPointAsObject() throws IOException {
        assertConvertValue("{ \n" + //
                "    \"lat\": 41.12,\n" + //
                "    \"lon\": -71.34\n" + //
                "  }", 41.12d, -71.34d);

        assertThrows(MismatchedInputException.class, () -> {
            convertValueToGeoPoint("{ \n" + //
            "    \"lat\": \"blah\",\n" + //
            "    \"lon\": null\n" + //
            "  }");
        });

        assertThrows(MismatchedInputException.class, () -> {
            convertValueToGeoPoint("{ \n" + //
            "    \"lat\": null,\n" + //
            "    \"lon\": null\n" + //
            "  }");
        });
    }

    @Test
    public void testGeoPointAsString() throws IOException {
        assertConvertValue("\"41.12,-71.34\"", 41.12d, -71.34d);
    }

    @Test
    public void testInvalidGeoPointJson() {
        assertThrows(JsonMappingException.class, () -> {
            // should be 'lat' and 'lon' (not 'lng')
            assertConvertValue("{ \n" + //
            "    \"lat\": 41.12,\n" + //
            "    \"lng\": -71.34\n" + //
            "  }", 41.12d, -71.34d);
        });
    }

    @Test
    public void testJackson() throws IOException {
        JacksonTestUtils.testReadWrite(POINT, GeoPoint.class);
    }

    private GeoPoint assertConvertValue(final String value, final double lat, final double lon)
            throws IOException {
        final GeoPoint point = convertValueToGeoPoint(value);
        assertEquals(lat, point.getLat(), 0.001d);
        assertEquals(lon, point.getLon(), 0.001d);
        return point;
    }

    private GeoPoint convertValueToGeoPoint(final String value)
            throws JsonProcessingException, JsonMappingException {
        final ObjectMapper mapper = JacksonUtils.getObjectMapper();
        final GeoPoint point = mapper.readValue(value, GeoPoint.class);
        return point;
    }

    @Test
    public void testRounding() {
        final GeoPoint point = GeoPoint.of("drm3btev3e86");
        Assertions.assertEquals("drm3btev3e86", point.getGeohash());

        for (int places = 0; places < 10; places++) {
            final GeoPoint rounded = point.round(places);
            assertSame(rounded, rounded.round(places));
        }
        Assertions.assertEquals(POINT, point.round(6));
    }

    @Test
    public void testSerializable() {
        SerializableTestUtils.testSerializable(POINT, GeoPoint.class);
    }
}
