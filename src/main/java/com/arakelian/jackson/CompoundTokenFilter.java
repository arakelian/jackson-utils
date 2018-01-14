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
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.google.common.base.Preconditions;

public class CompoundTokenFilter extends TokenFilter {
    @FunctionalInterface
    public interface ScalarFunction {
        boolean apply(TokenFilter filter);
    }

    private static enum Type {
        NULL, INCLUDE_ALL, MIX;
    }

    public static TokenFilter of(final TokenFilter... filters) {
        final Type newType = type(filters);
        switch (newType) {
        case NULL:
            return null;
        case INCLUDE_ALL:
            return TokenFilter.INCLUDE_ALL;
        case MIX:
            break;
        }

        return new CompoundTokenFilter(filters, newType);
    }

    private static Type type(final TokenFilter[] filters) {
        if (filters == null || filters.length == 0) {
            return Type.INCLUDE_ALL;
        }

        boolean includeAll = true;
        for (int i = 0, size = filters.length; i < size; i++) {
            final TokenFilter filter = filters[i];
            if (filter == null) {
                return Type.NULL;
            }
            includeAll = includeAll && filter == TokenFilter.INCLUDE_ALL;
        }

        if (includeAll) {
            return Type.INCLUDE_ALL;
        }
        return Type.MIX;
    }

    private final TokenFilter[] filters;

    private final Type type;

    private CompoundTokenFilter(final TokenFilter[] filters, final Type type) {
        this.filters = Preconditions.checkNotNull(filters);
        this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompoundTokenFilter other = (CompoundTokenFilter) obj;
        if (!Arrays.equals(filters, other.filters)) {
            return false;
        }
        return true;
    }

    private TokenFilter filter(final Function<TokenFilter, TokenFilter> function) {
        switch (type) {
        case NULL:
            return null;
        case INCLUDE_ALL:
            return TokenFilter.INCLUDE_ALL;
        case MIX:
            break;
        }

        final TokenFilter[] newFilters = new TokenFilter[filters.length];
        for (int i = 0, size = filters.length; i < size; i++) {
            final TokenFilter filter = filters[i];
            newFilters[i] = filter != null ? function.apply(filter) : null;
        }

        if (Arrays.equals(filters, newFilters)) {
            return this;
        }

        return CompoundTokenFilter.of(newFilters);
    }

    @Override
    public TokenFilter filterStartArray() {
        return filter(filter -> filter.filterStartArray());
    }

    @Override
    public TokenFilter filterStartObject() {
        return filter(filter -> filter.filterStartObject());
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(filters);
        return result;
    }

    @Override
    public boolean includeBinary() {
        return scalar(filter -> filter.includeBinary());
    }

    @Override
    public boolean includeBoolean(final boolean value) {
        return scalar(filter -> filter.includeBoolean(value));
    }

    @Override
    public TokenFilter includeElement(final int index) {
        return filter(filter -> filter.includeElement(index));
    }

    @Override
    public boolean includeEmbeddedValue(final Object ob) {
        return scalar(filter -> filter.includeEmbeddedValue(ob));
    }

    @Override
    public boolean includeNull() {
        return scalar(filter -> filter.includeNull());
    }

    @Override
    public boolean includeNumber(final BigDecimal v) {
        return scalar(filter -> filter.includeNumber(v));
    }

    @Override
    public boolean includeNumber(final BigInteger v) {
        return scalar(filter -> filter.includeNumber(v));
    }

    @Override
    public boolean includeNumber(final double v) {
        return scalar(filter -> filter.includeNumber(v));
    }

    @Override
    public boolean includeNumber(final float v) {
        return scalar(filter -> filter.includeNumber(v));
    }

    @Override
    public boolean includeNumber(final int v) {
        return scalar(filter -> filter.includeNumber(v));
    }

    @Override
    public boolean includeNumber(final long v) {
        return scalar(filter -> filter.includeNumber(v));
    }

    @Override
    public TokenFilter includeProperty(final String name) {
        return filter(filter -> filter.includeProperty(name));
    }

    @Override
    public boolean includeRawValue() {
        return scalar(filter -> filter.includeRawValue());
    }

    @Override
    public TokenFilter includeRootValue(final int index) {
        return filter(filter -> filter.includeRootValue(index));
    }

    @Override
    public boolean includeString(final String value) {
        return scalar(filter -> filter.includeString(value));
    }

    @Override
    public boolean includeValue(final JsonParser p) throws IOException {
        try {
            return scalar(filter -> {
                try {
                    return filter.includeValue(p);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private boolean scalar(final ScalarFunction function) {
        switch (type) {
        case NULL:
            return false;
        case INCLUDE_ALL:
            return _includeScalar();
        case MIX:
            break;
        }

        for (int i = 0, size = filters.length; i < size; i++) {
            final TokenFilter filter = filters[i];
            if (filter != null && function.apply(filter)) {
                return true;
            }
        }

        return false;
    }
}
