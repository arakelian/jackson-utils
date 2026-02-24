package com.arakelian.jackson.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Functional interface for writing JSON content to a {@link JsonGenerator}.
 */
@FunctionalInterface
public interface JsonGeneratorCallback {
    /** Writes JSON content to the given {@link JsonGenerator}. */
    public void accept(JsonGenerator generator) throws IOException;
}
