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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.arakelian.core.utils.DateUtils;
import com.arakelian.jackson.model.GeoPoint;
import com.google.common.collect.ImmutableList;

public class MapPathTest {
    private static final String JSON = "{\n" + //
            "  \"_index\" : \"c9c26c1cbd7e47eb8a85f6c07ddff12d\",\n" + //
            "  \"_type\" : \"test\",\n" + //
            "  \"_id\" : \"random-id\",\n" + //
            "  \"_score\" : 1.0,\n" + //
            "  \"_source\" : {\n" + //
            "    \"id\" : \"random-id\",\n" + //
            "    \"firstName\" : \"GREG\",\n" + //
            "    \"lastName\" : \"ARAKELIAN\",\n" + //
            "    \"gender\" : \"MALE\",\n" + //
            "    \"location\" : \"drm3btev3e86\",\n" + //
            "    \"birthday\" : \"2000-12-25T16:46Z\",\n" + //
            "    \"age\" : 18\n" + //
            "  },\n" + //
            "  \"matched_queries\" : [\n" + //
            "    \"ids_query\"\n" + //
            "  ]\n" + //
            "}\n";

    private MapPath mapPath;

    @Before
    public void setupMap() throws IOException {
        mapPath = MapPath.of(JSON);
    }

    @Test
    public void testMapPath() {
        Assert.assertEquals("GREG", mapPath.getString("_source/firstName"));
        Assert.assertEquals(1.0d, mapPath.getDouble("_score"), 0.001d);
        Assert.assertEquals(Integer.valueOf(18), mapPath.getInt("_source/age"));
        Assert.assertEquals(GeoPoint.of("41.12,-71.34"), mapPath.getGeoPoint("_source/location"));
        Assert.assertEquals(
                DateUtils.toZonedDateTimeUtc("2000-12-25T16:46Z"),
                mapPath.getZonedDateTime("_source/birthday"));
        Assert.assertEquals(ImmutableList.of("ids_query"), mapPath.getList("matched_queries"));
    }
}
