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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.filter.TokenFilter;

public class JsonPointerNotMatchedFilter extends JsonPointerBasedFilter {
    public JsonPointerNotMatchedFilter(final JsonPointer match) {
        super(match);
    }

    public JsonPointerNotMatchedFilter(final String ptrExpr) {
        super(ptrExpr);
    }

    @Override
    protected boolean _includeScalar() {
        // should only occur for root-level scalars, path "/"
        return !_pathToMatch.matches();
    }

    @Override
    public TokenFilter includeElement(final int index) {
        final JsonPointer next = _pathToMatch.matchElement(index);
        if (next == null) {
            return TokenFilter.INCLUDE_ALL;
        }
        if (next.matches()) {
            return null;
        }
        return new JsonPointerNotMatchedFilter(next);
    }

    @Override
    public TokenFilter includeProperty(final String name) {
        final JsonPointer next = _pathToMatch.matchProperty(name);
        if (next == null) {
            return TokenFilter.INCLUDE_ALL;
        }
        if (next.matches()) {
            return null;
        }
        return new JsonPointerNotMatchedFilter(next);
    }

    @Override
    public String toString() {
        return "[NotJsonPointerFilter at: " + _pathToMatch + "]";
    }
}
