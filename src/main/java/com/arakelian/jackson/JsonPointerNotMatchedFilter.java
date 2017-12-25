package com.arakelian.jackson;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.filter.TokenFilter;

public class JsonPointerNotMatchedFilter extends JsonPointerBasedFilter {
    public JsonPointerNotMatchedFilter(JsonPointer match) {
        super(match);
    }

    public JsonPointerNotMatchedFilter(String ptrExpr) {
        super(ptrExpr);
    }

    @Override
    public TokenFilter includeElement(int index) {
        JsonPointer next = _pathToMatch.matchElement(index);
        if (next == null) {
            return TokenFilter.INCLUDE_ALL;
        }
        if (next.matches()) {
            return null;
        }
        return new JsonPointerNotMatchedFilter(next);
    }

    @Override
    public TokenFilter includeProperty(String name) {
        JsonPointer next = _pathToMatch.matchProperty(name);
        if (next == null) {
            return TokenFilter.INCLUDE_ALL;
        }
        if (next.matches()) {
            return null;
        }
        return new JsonPointerNotMatchedFilter(next);
    }

    @Override
    protected boolean _includeScalar() {
        // should only occur for root-level scalars, path "/"
        return !_pathToMatch.matches();
    }

    @Override
    public String toString() {
        return "[NotJsonPointerFilter at: " + _pathToMatch + "]";
    }
}
