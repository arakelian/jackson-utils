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

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

@Value.Immutable(copy = false)
@JsonSerialize(using = Coordinate.CoordinateSerializer.class)
@JsonDeserialize(using = Coordinate.CoordinateDeserializer.class)
@JsonPropertyOrder({ "lat", "lon" })
public abstract class Coordinate implements Serializable, Comparable<Coordinate> {
    public static class CoordinateDeserializer extends JsonDeserializer<Coordinate> {
        @Override
        public Coordinate deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            final JsonNode node = ctxt.readValue(p, JsonNode.class);
            if (node instanceof ArrayNode) {
                final ArrayNode arr = (ArrayNode) node;
                final int size = arr.size();
                if (size == 2) {
                    return ImmutableCoordinate.builder() //
                            .x(arr.get(0).asDouble()) //
                            .y(arr.get(1).asDouble()) //
                            .build();
                } else if (size == 3) {
                    return ImmutableCoordinate.builder() //
                            .x(arr.get(0).asDouble()) //
                            .y(arr.get(1).asDouble()) //
                            .z(arr.get(2).asDouble()) //
                            .build();
                }

                // always throws exception
                ctxt.reportMappingException(
                        "Expecting array with 2 or 3 elements but found %s elements",
                        size);
                return null;

            }

            if (node instanceof TextNode) {
                final TextNode text = (TextNode) node;
                final String v = text.asText("");
                return of(v);
            }

            // always throws exception
            ctxt.reportMappingException("Expecting array, object or text node");
            return null;
        }
    }

    public static class CoordinateSerializer extends JsonSerializer<Coordinate> {
        @Override
        public void serialize(
                final Coordinate value,
                final JsonGenerator gen,
                final SerializerProvider serializers) throws IOException, JsonProcessingException {
            gen.writeStartArray();
            gen.writeNumber(value.getX());
            gen.writeNumber(value.getY());

            final double z = value.getZ();
            if (!Double.isNaN(z)) {
                gen.writeNumber(z);
            }

            gen.writeEndArray();
        }
    }

    private static final Splitter COMMA_SPLITTER = Splitter.on(Pattern.compile("\\s*,\\s*"));

    /**
     * Default number of decimals places that we round to
     */
    public static final int DEFAULT_PLACES = 6;

    /**
     * Error margin when comparison x/y/z values
     */
    public static final double DEFAULT_ERROR = 0.000001d;

    /**
     * The value used to indicate a null or missing ordinate value. In particular, used for the
     * value of ordinates for dimensions greater than the defined dimension of a coordinate.
     */
    public static final double NULL_ORDINATE = Double.NaN;

    private static boolean equalsWithTolerance(final double x1, final double x2, final double tolerance) {
        return Math.abs(x1 - x2) <= tolerance;
    }

    public static Coordinate of(final double x, final double y) {
        return ImmutableCoordinate.builder() //
                .x(x) //
                .y(y) //
                .build();
    }

    public static Coordinate of(final double x, final double y, final double z) {
        return ImmutableCoordinate.builder() //
                .x(x) //
                .y(y) //
                .z(z) //
                .build();
    }

    public static Coordinate of(final String value) {
        final Iterator<String> it = COMMA_SPLITTER.split(value).iterator();

        Preconditions.checkState(it.hasNext(), "Expected coordinate in x,y,z format but have: %s", value);
        final double x = Double.parseDouble(it.next());

        Preconditions.checkState(it.hasNext(), "Expected coordinate in x,y,z format but have: %s", value);
        final double y = Double.parseDouble(it.next());

        final double z;
        if (it.hasNext()) {
            z = Double.parseDouble(it.next());
            Preconditions
                    .checkState(!it.hasNext(), "Expected coordinate in x,y,z format but have: %s", value);
        } else {
            z = NULL_ORDINATE;
        }

        return of(x, y, z);
    }

    public static double round(final double value) {
        return round(value, DEFAULT_PLACES);
    }

    public static double round(final double value, final int places) {
        Preconditions.checkArgument(places >= 0, "places must be >= 0");

        if (Double.isNaN(value)) {
            return value;
        }

        return new BigDecimal(Double.toString(value)) //
                .setScale(places, RoundingMode.HALF_UP) //
                .doubleValue();
    }

    /**
     * Compares this {@link Coordinate} with the specified {@link Coordinate} for order. This method
     * ignores the z value when making the comparison. Returns:
     * <UL>
     * <LI>-1 : this.x &lt; other.x || ((this.x == other.x) &amp;&amp; (this.y &lt; other.y))
     * <LI>0 : this.x == other.x &amp;&amp; this.y = other.y
     * <LI>1 : this.x &gt; other.x || ((this.x == other.x) &amp;&amp; (this.y &gt; other.y))
     *
     * </UL>
     * Note: This method assumes that ordinate values are valid numbers. NaN values are not handled
     * correctly.
     *
     * @param other
     *            the <code>Coordinate</code> with which this <code>Coordinate</code> is being
     *            compared
     * @return -1, zero, or 1 as this <code>Coordinate</code> is less than, equal to, or greater
     *         than the specified <code>Coordinate</code>
     */
    @Override
    public int compareTo(final Coordinate other) {
        if (getX() < other.getX()) {
            return -1;
        }
        if (getX() > other.getX()) {
            return 1;
        }
        if (getY() < other.getY()) {
            return -1;
        }
        if (getY() > other.getY()) {
            return 1;
        }
        return 0;
    }

    /**
     * Computes the 2-dimensional Euclidean distance to another location. The Z-ordinate is ignored.
     *
     * @param c
     *            a point
     * @return the 2-dimensional Euclidean distance between the locations
     */
    public double distance(final Coordinate c) {
        final double dx = getX() - c.getX();
        final double dy = getY() - c.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Computes the 3-dimensional Euclidean distance to another location.
     *
     * @param c
     *            a coordinate
     * @return the 3-dimensional Euclidean distance between the locations
     */
    public double distance3D(final Coordinate c) {
        final double dx = getX() - c.getX();
        final double dy = getY() - c.getY();
        final double dz = getZ() - c.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Tests if another coordinate has the same value for Z, within a tolerance.
     *
     * @param c
     *            a coordinate
     * @param tolerance
     *            the tolerance value
     * @return true if the Z ordinates are within the given tolerance
     */
    public boolean equalInZ(final Coordinate c, final double tolerance) {
        return equalsWithTolerance(this.getZ(), c.getZ(), tolerance);
    }

    /**
     * Tests if another coordinate has the same values for the X and Y ordinates. The Z ordinate is
     * ignored.
     *
     * @param c
     *            a <code>Coordinate</code> with which to do the 2D comparison.
     * @param tolerance
     *            margin of error
     * @return true if <code>other</code> is a <code>Coordinate</code> with the same values for X
     *         and Y.
     */
    public boolean equals2D(final Coordinate c, final double tolerance) {
        if (!equalsWithTolerance(this.getX(), c.getX(), tolerance)) {
            return false;
        }
        if (!equalsWithTolerance(this.getY(), c.getY(), tolerance)) {
            return false;
        }
        return true;
    }

    /**
     * Tests if another coordinate has the same values for the X, Y and Z ordinates.
     *
     * @param other
     *            a <code>Coordinate</code> with which to do the 3D comparison.
     * @return true if <code>other</code> is a <code>Coordinate</code> with the same values for X, Y
     *         and Z.
     */
    public boolean equals3D(final Coordinate other) {
        return getX() == other.getX() && getY() == other.getY()
                && (getZ() == other.getZ() || Double.isNaN(getZ()) && Double.isNaN(other.getZ()));
    }

    public abstract double getX();

    public abstract double getY();

    @Value.Default
    public double getZ() {
        return NULL_ORDINATE;
    }

    @Value.Check
    protected Coordinate normalize() {
        final double x = getX();
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("invalid x: " + x);
        }

        final double y = getY();
        if (Double.isNaN(y)) {
            throw new IllegalArgumentException("invalid y: " + y);
        }

        // round decimals
        return round(DEFAULT_PLACES);
    }

    public Coordinate round(final int places) {
        final double x = getX();
        final double newX = round(x, places);
        final double y = getY();
        final double newY = round(y, places);
        final double z = getZ();
        final double newZ = round(z, places);

        if (Double.doubleToLongBits(x) == Double.doubleToLongBits(newX)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(newY)
                && Double.doubleToLongBits(z) == Double.doubleToLongBits(newZ)) {
            return this;
        }

        return of(newX, newY, newZ);
    }

    /**
     * Returns a <code>String</code> of the form <I>(x,y,z)</I> .
     *
     * @return a <code>String</code> of the form <I>(x,y,z)</I>
     */
    @Override
    public String toString() {
        return "(" + getX() + ", " + getY() + ", " + getZ() + ")";
    }
}
