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

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Preconditions;

@Value.Immutable(copy = false)
@JsonSerialize(as = ImmutableGeoPoint.class)
@JsonDeserialize(using = GeoPoint.GeoPointDeserializer.class)
@JsonPropertyOrder({ "lat", "lon" })
public abstract class GeoPoint implements Serializable {
    public static class GeoPointDeserializer extends JsonDeserializer<GeoPoint> {
        @Override
        public GeoPoint deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            final JsonNode node = ctxt.readValue(p, JsonNode.class);
            if (node instanceof ObjectNode) {
                final ObjectNode obj = (ObjectNode) node;
                final JsonNode lat = obj.get("lat");
                final JsonNode lon = obj.get("lon");
                if (lat == null || lon == null || !lat.isNumber() || !lon.isNumber()) {
                    // always throws exception
                    ctxt.reportInputMismatch(this, "Expecting object with numeric 'lat' and 'lon' fields");
                    return null;
                }
                return ImmutableGeoPoint.builder() //
                        .lat(lat.asDouble()) //
                        .lon(lon.asDouble()) //
                        .build();
            }

            if (node instanceof ArrayNode) {
                final ArrayNode arr = (ArrayNode) node;
                if (arr.size() != 2) {
                    // always throws exception
                    ctxt.reportInputMismatch(
                            this,
                            "Expecting array with 2 elements but found %s elements",
                            arr.size());
                    return null;
                }
                JsonNode lonNode = arr.get(0);
                JsonNode latNode = arr.get(1);
                if (lonNode.isNumber() && latNode.isNumber()) {
                    return ImmutableGeoPoint.builder() //
                            .lon(lonNode.asDouble()) //
                            .lat(latNode.asDouble()) //
                            .build();
                }
                ctxt.reportInputMismatch(this, "Expecting array with [longitude, latitude]");
                return null;
            }

            if (node instanceof TextNode) {
                final TextNode text = (TextNode) node;
                final String v = text.asText("");
                GeoPoint point = of(v);
                return point;
            }

            // always throws exception
            ctxt.reportInputMismatch(this, "Expecting array, object or text node");
            return null;
        }
    }

    /** Magic numbers for bit interleaving **/
    private static final long MAGIC[] = { //
            0x5555555555555555L, //
            0x3333333333333333L, //
            0x0F0F0F0F0F0F0F0FL, //
            0x00FF00FF00FF00FFL, //
            0x0000FFFF0000FFFFL, //
            0x00000000FFFFFFFFL, //
            0xAAAAAAAAAAAAAAAAL //
    };

    /** Shift values for bit interleaving **/
    private static final short SHIFT[] = { 1, 2, 4, 8, 16 };

    /**
     * Base 32 encoding doesn't use certain letters, like "l" or "i", or "o" which could be mistaken
     * for numerals 0 or 1.
     **/
    private static final char[] BASE_32 = { //
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', //
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', //
            'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', //
            'y', 'z' };

    private static final String BASE_32_STRING = new String(BASE_32);

    /** maximum precision for geohash strings */
    public static final int PRECISION = 12;

    /** number of bits used for quantizing latitude and longitude values */
    public static final short BITS = 31;

    /** scaling factors to convert lat/lon into unsigned space */
    private static final double LAT_SCALE = (0x1L << BITS) / 180.0D;

    private static final double LON_SCALE = (0x1L << BITS) / 360.0D;

    /** scaling factors to convert lat/lon into unsigned space */
    private static final short MORTON_OFFSET = (BITS << 1) - PRECISION * 5;

    /** Minimum longitude value. */
    public static final double MIN_LON_INCL = -180.0D;

    /** Maximum longitude value. */
    public static final double MAX_LON_INCL = 180.0D;

    /** Minimum latitude value. */
    public static final double MIN_LAT_INCL = -90.0D;

    /** Maximum latitude value. */
    public static final double MAX_LAT_INCL = 90.0D;

    private static final double LAT_DECODE = 1 / ((0x1L << 32) / 180.0D);

    private static final double LON_DECODE = 1 / ((0x1L << 32) / 360.0D);

    /**
     * 7 decimal places is worth 1.1 millimiters of accuracy; this is good for charting motions of
     * tectonic plates and movements of volcanoes. Permanent, corrected, constantly-running GPS base
     * stations might be able to achieve this level of accuracy
     */
    public static final int DEFAULT_PLACES = 6;

    /**
     * Error margin when comparison latitude and longitude values
     */
    public static final double DEFAULT_ERROR = 0.000001d;

    /**
     * Returns one of two 32-bit values that were previously interleaved to produce a 64-bit value.
     *
     * @param value
     *            64-bit value that was created by interleaving two 32-bit values
     * @return one of two previously interleaved values
     */
    protected static long deinterleave(long value) {
        value &= MAGIC[0];
        value = (value ^ value >>> SHIFT[0]) & MAGIC[1];
        value = (value ^ value >>> SHIFT[1]) & MAGIC[2];
        value = (value ^ value >>> SHIFT[2]) & MAGIC[3];
        value = (value ^ value >>> SHIFT[3]) & MAGIC[4];
        value = (value ^ value >>> SHIFT[4]) & MAGIC[5];
        return value;
    }

    /**
     * Returns a value with the even and odd bits flipped.
     *
     * @param val
     *            64-bit value
     * @return value with the even and odd bits flipped
     */
    public static final long flipFlop(final long val) {
        return (val & MAGIC[6]) >>> 1 | (val & MAGIC[0]) << 1;
    }

    public static GeoPoint of(final double lat, final double lon) {
        return ImmutableGeoPoint.builder() //
                .lat(lat) //
                .lon(lon) //
                .build();
    }

    public static GeoPoint of(final String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        final double lat;
        final double lon;

        final int comma = value.indexOf(',');
        if (comma != -1) {
            lat = Double.parseDouble(value.substring(0, comma).trim());
            lon = Double.parseDouble(value.substring(comma + 1).trim());
        } else {
            int level = 11;
            long l = 0L;
            final char[] chars = value.toCharArray();
            for (int i = 0, size = chars.length; i < size; i++) {
                final char c = chars[i];
                final long b = BASE_32_STRING.indexOf(c);
                l |= b << level-- * 5 + MORTON_OFFSET;
            }
            final long mortonHash = flipFlop(l);

            lat = deinterleave(mortonHash >>> 1) / LAT_SCALE - MAX_LAT_INCL;
            lon = deinterleave(mortonHash) / LON_SCALE - MAX_LON_INCL;
        }

        return of(lat, lon);
    }

    public static double round(final double value) {
        return round(value, DEFAULT_PLACES);
    }

    public static double round(final double value, final int places) {
        Preconditions.checkArgument(places >= 0, "places must be >= 0");

        return new BigDecimal(Double.toString(value)) //
                .setScale(places, RoundingMode.HALF_UP) //
                .doubleValue();
    }

    @JsonIgnore
    @Value.Lazy
    @Value.Auxiliary
    public String getGeohash() {
        // quantizes double (64 bit) latitude into 32 bits (rounding down: in the direction of -90)
        double latitude = getLat();
        if (latitude == MAX_LAT_INCL) {
            // the maximum possible value cannot be encoded without overflow
            latitude = Math.nextDown(latitude);
        }

        double longitude = getLon();
        if (longitude == MAX_LON_INCL) {
            // the maximum possible value cannot be encoded without overflow
            longitude = Math.nextDown(longitude);
        }

        // we flip the sign bits so negative ints sort before positive ints
        long even = ((int) Math.floor(longitude / LON_DECODE) ^ 0x80000000) & 0x00000000FFFFFFFFL;
        even = (even | even << SHIFT[4]) & MAGIC[4];
        even = (even | even << SHIFT[3]) & MAGIC[3];
        even = (even | even << SHIFT[2]) & MAGIC[2];
        even = (even | even << SHIFT[1]) & MAGIC[1];
        even = (even | even << SHIFT[0]) & MAGIC[0];

        long odd = ((int) Math.floor(latitude / LAT_DECODE) ^ 0x80000000) & 0x00000000FFFFFFFFL;
        odd = (odd | odd << SHIFT[4]) & MAGIC[4];
        odd = (odd | odd << SHIFT[3]) & MAGIC[3];
        odd = (odd | odd << SHIFT[2]) & MAGIC[2];
        odd = (odd | odd << SHIFT[1]) & MAGIC[1];
        odd = (odd | odd << SHIFT[0]) & MAGIC[0];

        // interleave even and odd bits
        final long result = odd << 1 | even;

        // convert morton encoding to geohash encoding
        long morton;
        if (result == 0xFFFFFFFFFFFFFFFFL) {
            morton = flipFlop(result & 0xC000000000000000L);
        } else {
            morton = flipFlop(result >>> 2);
        }
        morton >>>= (PRECISION - 12) * 5 + MORTON_OFFSET;
        long geohash = morton << 4 | 12;

        // convert geohash to string
        int level = (int) geohash & 15;
        geohash >>>= 4;
        final char[] chars = new char[level];
        do {
            chars[--level] = BASE_32[(int) (geohash & 31L)];
            geohash >>>= 5;
        } while (level > 0);
        return new String(chars);
    }

    public abstract double getLat();

    public abstract double getLon();

    @Value.Check
    protected GeoPoint normalize() {
        // validates latitude is within standard +/-90 coordinate bounds
        final double lat = getLat();
        if (Double.isNaN(lat) || lat < MIN_LAT_INCL || lat > MAX_LAT_INCL) {
            throw new IllegalArgumentException(
                    "invalid latitude " + lat + "; must be between " + MIN_LAT_INCL + " and " + MAX_LAT_INCL);
        }

        // validates longitude is within standard +/-180 coordinate bounds
        final double lon = getLon();
        if (Double.isNaN(lon) || lon < MIN_LON_INCL || lon > MAX_LON_INCL) {
            throw new IllegalArgumentException("invalid longitude " + lon + "; must be between "
                    + MIN_LON_INCL + " and " + MAX_LON_INCL);
        }

        // round decimals
        return round(DEFAULT_PLACES);
    }

    public GeoPoint round(final int places) {
        final double lat = getLat();
        final double newLat = round(lat, places);
        final double lon = getLon();
        final double newLon = round(lon, places);

        if (Double.doubleToLongBits(lat) == Double.doubleToLongBits(newLat)
                && Double.doubleToLongBits(lon) == Double.doubleToLongBits(newLon)) {
            return this;
        }

        return of(newLat, newLon);
    }
}
