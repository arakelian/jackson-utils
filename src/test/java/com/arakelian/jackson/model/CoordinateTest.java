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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.arakelian.core.utils.SerializableTestUtils;
import com.arakelian.jackson.utils.JacksonTestUtils;
import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CoordinateTest {
    public static final Coordinate POINT2D = ImmutableCoordinate.builder() //
            .x(41.12d) //
            .y(-71.34d) //
            .build();

    public static final Coordinate POINT3D = ImmutableCoordinate.builder() //
            .x(41.12d) //
            .y(-71.34d) //
            .z(100d) //
            .build();

    @Test
    public void testCommaSeparatedString() {
        final Coordinate point = Coordinate.of("41.12,-71.34");
        assertEquals(41.12d, point.getX(), Coordinate.DEFAULT_ERROR);
        assertEquals(-71.34d, point.getY(), Coordinate.DEFAULT_ERROR);
    }

    @Test
    public void testDecimalRounding() {
        Assert.assertEquals(51.009830d, Coordinate.round(51.00982963107526d, 6), 0.000001d);
        Assert.assertEquals(51.0098296d, Coordinate.round(51.00982963107526d, 7), 0.0000001d);
        Assert.assertEquals(51.00982963d, Coordinate.round(51.00982963107526d, 8), 0.00000001d);
    }

    @Test
    public void testCoordinateAsArray() throws IOException {
        testJackson("[ -71.34, 41.12 ]", 41.12d, -71.34d);
    }

    @Test
    public void testCoordinateAsString() throws IOException {
        testJackson("\"41.12,-71.34\"", 41.12d, -71.34d);
    }

    @Test
    public void testJackson() throws IOException {
        JacksonTestUtils.testReadWrite(POINT2D, Coordinate.class);
        JacksonTestUtils.testReadWrite(POINT3D, Coordinate.class);
    }

    @Test
    public void testSerializable() {
        SerializableTestUtils.testSerializable(POINT2D, Coordinate.class);
        SerializableTestUtils.testSerializable(POINT3D, Coordinate.class);
    }

    private Coordinate testJackson(final String value, final double x, final double y) throws IOException {
        final ObjectMapper mapper = JacksonUtils.getObjectMapper();
        final Coordinate point = mapper.readValue(value, Coordinate.class);
        assertEquals(x, point.getX(), 0.001d);
        assertEquals(y, point.getY(), 0.001d);
        return point;
    }
}
