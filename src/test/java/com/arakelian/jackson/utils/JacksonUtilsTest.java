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

package com.arakelian.jackson.utils;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.arakelian.jackson.model.Jackson;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class JacksonUtilsTest {
    public static enum Gender {
        MALE, FEMALE;
    }

    public static final class ImmutableBean {
        private final String firstName;
        private final String lastName;
        private final Gender gender;

        // Jackson will infer parameter name because we have pulled jackson-module-parameter-names
        // as dependency, and have
        // org.eclipse.jdt.core.prefs/org.eclipse.jdt.core.compiler.codegen.methodParameters=generate
        @JsonCreator
        public ImmutableBean(final String firstName, final String lastName, final Gender gender) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.gender = gender;
        }

        public final String getFirstName() {
            return firstName;
        }

        public final Gender getGender() {
            return gender;
        }

        public final String getLastName() {
            return lastName;
        }
    }

    public static class SensitiveBean {
        @JsonView(Views.Public.class)
        private String id;

        @JsonView(Views.Public.class)
        private String name;

        @JsonView(Views.Private.class)
        private String username;

        @JsonView(Views.Private.class)
        private String password;

        public final String getId() {
            return id;
        }

        public final String getName() {
            return name;
        }

        public final String getPassword() {
            return password;
        }

        public final String getUsername() {
            return username;
        }

        public final void setId(final String id) {
            this.id = id;
        }

        public final void setName(final String name) {
            this.name = name;
        }

        public final void setPassword(final String password) {
            this.password = password;
        }

        public final void setUsername(final String username) {
            this.username = username;
        }
    }

    public static class TimeBean {
        private LocalDateTime localDateTime;

        private ZonedDateTime zonedDateTime;

        public final LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public final ZonedDateTime getZonedDateTime() {
            return zonedDateTime;
        }

        public final void setLocalDateTime(final LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public final void setZonedDateTime(final ZonedDateTime zonedDateTime) {
            this.zonedDateTime = zonedDateTime;
        }

        public final TimeBean withLocalDateTime(final LocalDateTime localDateTime) {
            setLocalDateTime(localDateTime);
            return this;
        }

        public final TimeBean withZonedDateTime(final ZonedDateTime zonedDateTime) {
            setZonedDateTime(zonedDateTime);
            return this;
        }
    }

    public static class Views {
        public static class Empty {
        }

        public static class Private extends Public {
        }

        public static class Public {
        }
    }

    private SensitiveBean sensitiveBean() {
        final SensitiveBean sensitive = new SensitiveBean();
        sensitive.setId("100");
        sensitive.setName("Greg Arakelian");
        sensitive.setUsername("garakelian");
        sensitive.setPassword("mysecret");
        return sensitive;
    }

    @Test
    public void testDateSerialization() throws IOException {
        // UTC dates
        verifyDateSerialization(
                "2016-12-21T16:46:39.830000000Z",
                ZonedDateTime.of(2016, 12, 21, 16, 46, 39, 830000000, ZoneOffset.UTC));
        verifyDateSerialization(
                "2016-12-21T16:46:39.800000000Z",
                ZonedDateTime.of(2016, 12, 21, 16, 46, 39, 800000000, ZoneOffset.UTC));
        verifyDateSerialization(
                "2016-12-21T16:46:39.000000000Z",
                ZonedDateTime.of(2016, 12, 21, 16, 46, 39, 000000000, ZoneOffset.UTC));

        // Daylight savings not in effect so +04:00
        verifyDateSerialization(
                "2016-06-21T20:46:39.830000000Z",
                ZonedDateTime.of(2016, 6, 21, 16, 46, 39, 830000000, ZoneId.of("America/New_York")));

        // Daylight savings is in effect so +05:00
        verifyDateSerialization(
                "2016-12-21T21:46:39.830000000Z",
                ZonedDateTime.of(2016, 12, 21, 16, 46, 39, 830000000, ZoneId.of("America/New_York")));
    }

    @Test
    public void testDefaultView() {
        final SensitiveBean bean = sensitiveBean();

        final String expectedCompact = "{\"id\":\"100\",\"name\":\"Greg Arakelian\",\"username\":\"garakelian\",\"password\":\"mysecret\"}";

        final String expectedPretty = "{\n" + //
                "  \"id\" : \"100\",\n" + //
                "  \"name\" : \"Greg Arakelian\",\n" + //
                "  \"username\" : \"garakelian\",\n" + //
                "  \"password\" : \"mysecret\"\n" + //
                "}" //
                        .replace("\n", System.getProperty("line.separator"));

        // ObjectMapper is compact
        assertEquals(
                expectedCompact,
                Jackson.fromDefault() //
                        .pretty(false) //
                        .build() //
                        .toString(bean));

        // ObjectMapper is pretty
        assertEquals(
                expectedPretty,
                Jackson.fromDefault() //
                        .pretty(true) //
                        .build() //
                        .toString(bean));

        // ObjectWriter is compact
        assertEquals(
                expectedCompact,
                Jackson.fromDefault() //
                        .build() //
                        .toString(bean, false));

        // ObjectWriter is pretty
        assertEquals(
                expectedPretty,
                Jackson.fromDefault() //
                        .build() //
                        .toString(bean, true));
    }

    @Test
    public void testEmptyView() {
        final SensitiveBean bean = sensitiveBean();
        final String expectedCompact = "{}";
        final String expectedPretty = "{ }";

        // ObjectMapper is compact
        assertEquals(
                expectedCompact,
                Jackson.fromDefault() //
                        .view(Views.Empty.class) //
                        .pretty(false) //
                        .build() //
                        .toString(bean));

        // ObjectMapper is pretty
        assertEquals(
                expectedPretty,
                Jackson.fromDefault() //
                        .view(Views.Empty.class) //
                        .pretty(true) //
                        .build() //
                        .toString(bean));

        // ObjectWriter is compact
        assertEquals(
                expectedCompact,
                Jackson.fromDefault() //
                        .view(Views.Empty.class) //
                        .pretty(false) //
                        .build() //
                        .toString(bean, false));

        // ObjectWriter is pretty
        assertEquals(
                expectedPretty,
                Jackson.fromDefault() //
                        .view(Views.Empty.class) //
                        .pretty(false) //
                        .build() //
                        .toString(bean, true));
    }

    @Test
    public void testJsonCreator() throws IOException {
        // should not have to use @JsonCreator and specify property names with Java 8;
        // also enum should be forced to uppercase for match
        final ImmutableBean immutableBean = StringUtils
                .isEmpty("{\"firstName\":\"Greg\",\"lastName\":\"Arakelian\",\"gender\":\"maLE\"}")
                        ? null
                        : JacksonUtils.readValue(
                                "{\"firstName\":\"Greg\",\"lastName\":\"Arakelian\",\"gender\":\"maLE\"}",
                                ImmutableBean.class);
        assertNotNull(immutableBean);
        assertEquals("Greg", immutableBean.firstName);
        assertEquals("Arakelian", immutableBean.lastName);
        assertEquals(Gender.MALE, immutableBean.gender);
    }

    @Test
    public void testPrivateView() {
        final SensitiveBean bean = sensitiveBean();
        final String expectedCompact = "{\"id\":\"100\",\"name\":\"Greg Arakelian\",\"username\":\"garakelian\",\"password\":\"mysecret\"}";
        final String expectedPretty = "{\n" + //
                "  \"id\" : \"100\",\n" + //
                "  \"name\" : \"Greg Arakelian\",\n" + //
                "  \"username\" : \"garakelian\",\n" + //
                "  \"password\" : \"mysecret\"\n" + //
                "}" //
                        .replace("\n", System.getProperty("line.separator"));

        // ObjectMapper is compact
        assertEquals(
                expectedCompact,
                Jackson.fromDefault() //
                        .view(Views.Private.class) //
                        .pretty(false) //
                        .build() //
                        .toString(bean));

        // ObjectMapper is pretty
        assertEquals(
                expectedPretty,
                Jackson.fromDefault() //
                        .view(Views.Private.class) //
                        .pretty(true) //
                        .build() //
                        .toString(bean));

        // ObjectWriter is compact
        assertEquals(
                expectedCompact,
                Jackson.fromDefault() //
                        .view(Views.Private.class) //
                        .build() //
                        .toString(bean, false));

        // ObjectWriter is pretty
        assertEquals(
                expectedPretty,
                Jackson.fromDefault() //
                        .view(Views.Private.class) //
                        .build() //
                        .toString(bean, true));
    }

    @Test
    public void testPublicView() {
        final SensitiveBean bean = sensitiveBean();
        // public view - compact formatting
        assertEquals(
                "{\"id\":\"100\",\"name\":\"Greg Arakelian\"}",
                Jackson.fromDefault() //
                        .view(Views.Public.class) //
                        .pretty(false) //
                        .build() //
                        .toString(bean));

        // public view - pretty formatting
        assertEquals(
                "{\n  \"id\" : \"100\",\n  \"name\" : \"Greg Arakelian\"\n}"
                        .replace("\n", System.getProperty("line.separator")),
                Jackson.fromDefault() //
                        .view(Views.Public.class) //
                        .pretty(true) //
                        .build() //
                        .toString(bean));

    }

    @Test
    public void testToJson() {
        assertJsonEquals(
                "{\"one\":1,\"two\":2,\"three\":3.0}",
                JacksonUtils.buildJson("one", 1, "two", 2, "three", Double.valueOf(3)).toString());
        assertJsonEquals("{}", JacksonUtils.buildJson().toString());
    }

    @Test
    public void testTraverse() throws IOException {
        final JsonNode root = JacksonUtils.readValue("{\n" + //
                "    \"id\": 4,\n" + //
                "    \"name\": \"Patricia Lebsack\",\n" + //
                "    \"username\": \"Karianne\",\n" + //
                "    \"email\": \"Julianne.OConner@kory.org\",\n" + //
                "    \"address\": {\n" + //
                "      \"street\": \"Hoeger Mall\",\n" + //
                "      \"suite\": \"Apt. 692\",\n" + //
                "      \"city\": \"South Elvis\",\n" + //
                "      \"zipcode\": \"53919-4257\",\n" + //
                "      \"geo\": {\n" + //
                "        \"lat\": \"29.4572\",\n" + //
                "        \"lng\": \"-164.2990\"\n" + //
                "      }\n" + //
                "    },\n" + //
                "    \"phones\": [\"703-555-1212\", \"493-170-9623 x156\"],\n" + //
                "    \"website\": \"kale.biz\",\n" + //
                "    \"company\": {\n" + //
                "      \"name\": \"Robel-Corkery\",\n" + //
                "      \"catchPhrase\": \"Multi-tiered zero tolerance productivity\",\n" + //
                "      \"bs\": \"transition cutting-edge web services\"\n" + //
                "    }\n" + //
                "  }", JsonNode.class);

        final List<String> values = Lists.newArrayList();
        JacksonUtils.traverse(
                root,
                null,
                node -> {
                    // collect values as we traverse
                    values.add(node.asText());
                });

        // these are the values that should have been collected
        assertEquals(
                ImmutableList.of(
                        "4",
                        "Patricia Lebsack",
                        "Karianne",
                        "Julianne.OConner@kory.org",
                        "Hoeger Mall",
                        "Apt. 692",
                        "South Elvis",
                        "53919-4257",
                        "29.4572",
                        "-164.2990",
                        "703-555-1212",
                        "493-170-9623 x156",
                        "kale.biz",
                        "Robel-Corkery",
                        "Multi-tiered zero tolerance productivity",
                        "transition cutting-edge web services"),
                values);

        // now let's filter address
        values.clear();
        JacksonUtils.traverse(
                root,
                name -> !StringUtils.equals(name, "address"),
                node -> {
                    // collect values as we traverse
                    values.add(node.asText());
                });

        // these are the values that should have been collected, when adddress values have been
        // filtered out
        assertEquals(
                ImmutableList.of(
                        "4",
                        "Patricia Lebsack",
                        "Karianne",
                        "Julianne.OConner@kory.org",
                        "703-555-1212",
                        "493-170-9623 x156",
                        "kale.biz",
                        "Robel-Corkery",
                        "Multi-tiered zero tolerance productivity",
                        "transition cutting-edge web services"),
                values);
    }

    private void verifyDateSerialization(final String dateString, final ZonedDateTime expected)
            throws IOException {
        // serialize and confirm we have expected value
        final String json = JacksonUtils.toString(new TimeBean().withZonedDateTime(expected), false);
        assertEquals("{\"zonedDateTime\":\"" + dateString + "\"}", json);

        // deserialize and confirm we have same ZonedDateTime
        final TimeBean bean = StringUtils.isEmpty(json) ? null : JacksonUtils.readValue(json, TimeBean.class);
        assertEquals(expected.withZoneSameInstant(ZoneOffset.UTC), bean.getZonedDateTime());

        // serialize again and confirm we get back same date
        assertEquals(json, JacksonUtils.toString(bean, false));
    }
}
