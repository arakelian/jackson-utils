package com.arakelian.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Preconditions;

public class ExcludeSerializer<T> extends JsonSerializer<T> {
    private final Class<T> handledType;
    private final JsonPointerNotMatchedFilter excludeFilter;
    private final JsonSerializer<Object> delegate;

    public ExcludeSerializer(Class<T> handledType, JsonPointerNotMatchedFilter excludeFilter) {
        this(handledType, excludeFilter, null);
    }

    public ExcludeSerializer(
            Class<T> handledType,
            JsonPointerNotMatchedFilter excludeFilter,
            JsonSerializer<Object> delegate) {
        this.handledType = Preconditions.checkNotNull(handledType);
        this.excludeFilter = Preconditions.checkNotNull(excludeFilter);
        this.delegate = delegate;
    }

    @Override
    public Class<T> handledType() {
        return handledType;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        FilteringGeneratorDelegate filtering = new FilteringGeneratorDelegate(gen, excludeFilter, true, true);
        if (value == null || delegate == null) {
            serializers.defaultSerializeValue(value, filtering);
        } else {
            delegate.serialize(value, filtering, serializers);
        }
    }
}
