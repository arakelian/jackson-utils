package com.arakelian.jackson.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

@FunctionalInterface
public interface JsonGeneratorCallback {
    public void accept(JsonGenerator generator) throws IOException;
}
