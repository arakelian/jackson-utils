package com.arakelian.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Specialized {@link JsonGeneratorDelegate} that allows use of {@link TokenFilter} for outputting a
 * subset of content that caller tries to generate.
 *
 * @since 2.6
 */
public class FilteringJsonGenerator extends JsonGeneratorDelegate {
    protected static final class Context {
        final Type type;
        final String name;
        final Boolean test;

        public Context(final Type type, final String name, final Boolean test) {
            this.type = Preconditions.checkNotNull(type);
            this.name = name;
            this.test = test;
        }

        public void buildPath(final StringBuilder path) {
            switch (type) {
            case ARRAY:
                path.append(CONTEXT_SEPARATOR);
                if (name != null) {
                    path.append(name);
                }
                path.append(ARRAY_BRACKETS);
                break;
            case OBJECT:
                if (name != null) {
                    path.append(CONTEXT_SEPARATOR).append(name);
                }
                break;
            default:
                throw new IllegalStateException();
            }
        }
    }

    protected static enum Type {
        OBJECT, ARRAY;
    }

    private static final String ARRAY_BRACKETS = "[]";

    private static final char CONTEXT_SEPARATOR = '.';

    private static final Logger LOGGER = LoggerFactory.getLogger(FilteringJsonGenerator.class);

    protected final Set<String> includes;
    protected final Set<String> excludes;

    /** Object names we have traverse **/
    protected final List<Context> contexts = new ArrayList<>();

    /** Current field name **/
    protected String fieldName;

    public FilteringJsonGenerator(
            final JsonGenerator delegate,
            final Set<String> includes,
            final Set<String> excludes) {
        super(delegate, false); // do not delegate COPY methods
        this.includes = includes != null ? includes : ImmutableSet.of();
        this.excludes = excludes != null ? excludes : ImmutableSet.of();
    }

    protected void afterValue() {
        resetField();
    }

    protected String getCurrentPath() {
        final StringBuilder path = new StringBuilder();
        for (final Context context : contexts) {
            context.buildPath(path);
        }
        if (fieldName != null) {
            path.append(CONTEXT_SEPARATOR);
            path.append(fieldName);
        }
        if (path.length() == 0) {
            path.append(CONTEXT_SEPARATOR);
        }
        return path.toString();
    }

    private boolean hasArrayBrackets(final CharSequence path, final int index) {
        return index + 1 < path.length() //
                && path.charAt(index) == ARRAY_BRACKETS.charAt(0) //
                && path.charAt(index + 1) == ARRAY_BRACKETS.charAt(1);
    }

    private boolean isRoot(final CharSequence path) {
        return StringUtils.equals(".", path);
    }

    private boolean pathStartsWith(
            final CharSequence path,
            final CharSequence prefix,
            final boolean excluding) {
        if (path == prefix) {
            return true;
        }
        if (path == null || prefix == null) {
            return false;
        }

        // compare as much as we can
        final int pathLength = path.length();
        final int prefixLength = prefix.length();
        final int length = Math.min(pathLength, prefixLength);
        for (int i = 0; i < length; ++i) {
            if (path.charAt(i) != prefix.charAt(0 + i)) {
                return false;
            }
        }

        if (pathLength == prefixLength) {
            // exact match
            return true;
        }

        if (pathLength > prefixLength) {
            // we have matched prefixLength; path must start with prefix + "/"
            final boolean test = path.charAt(prefixLength) == CONTEXT_SEPARATOR
                    || hasArrayBrackets(path, prefixLength);
            return test;
        }

        // we have matched pathLength; do we need to go deeper for a match?
        final boolean test = isRoot(path) || prefix.charAt(pathLength) == CONTEXT_SEPARATOR
                || hasArrayBrackets(prefix, pathLength);
        return test ? !excluding : false;
    }

    protected void resetField() {
        fieldName = null;
    }

    protected boolean test() {
        final boolean test = test_();

        if (LOGGER.isTraceEnabled()) {
            final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            for (final StackTraceElement e : trace) {
                final String m = e.getMethodName();
                if (StringUtils.startsWith(m, "write")) {
                    LOGGER.trace("{} {}: {}", getCurrentPath(), m, test);
                    break;
                }
            }
        }
        return test;
    }

    protected boolean test_() {
        // have we pre-computed value for this depth?
        final int depth = contexts.size();
        if (depth != 0) {
            final Context current = contexts.get(depth - 1);
            if (current.test != null) {
                return current.test.booleanValue();
            }
        }

        final String path = getCurrentPath();

        // excludes are always processed first!
        if (excludes != null) {
            for (final String exclude : excludes) {
                if (pathStartsWith(path, exclude, true)) {
                    return false;
                }
            }
        }

        // include when specifically asked to do so
        if (includes != null) {
            for (final String include : includes) {
                if (pathStartsWith(path, include, false)) {
                    return true;
                }
            }
        }

        // if client specified which fields to include, it must be on that
        // list; otherwise, everything included by default
        return includes == null || includes.size() == 0;
    }

    protected boolean testEnd(final Type type) {
        final int depth = contexts.size();
        Preconditions.checkState(depth != 0, "extra call to writeEndArray or writeEndObject");

        final Context current = contexts.remove(depth - 1);
        Preconditions.checkState(current.type.equals(type), "mismatch start/end of %s", type);
        if (current.test != null) {
            return current.test.booleanValue();
        }
        return true;
    }

    protected boolean testStartArray() {
        final boolean test = test();
        contexts.add(new Context(Type.ARRAY, fieldName, test ? null : Boolean.FALSE));
        resetField();
        return test;
    }

    protected boolean testStartObject() {
        final boolean test = test();
        contexts.add(new Context(Type.OBJECT, fieldName, test ? null : Boolean.FALSE));
        resetField();
        return test;
    }

    @Override
    public String toString() {
        return getCurrentPath();
    }

    @Override
    public void writeBinary(
            final Base64Variant b64variant,
            final byte[] data,
            final int offset,
            final int len) throws IOException {
        if (test()) {
            delegate.writeBinary(b64variant, data, offset, len);
            afterValue();
        }
    }

    @Override
    public int writeBinary(final Base64Variant b64variant, final InputStream data, final int dataLength)
            throws IOException {
        if (test()) {
            final int bytesRead = delegate.writeBinary(b64variant, data, dataLength);
            afterValue();
            return bytesRead;
        }
        return -1;
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        if (test()) {
            delegate.writeBoolean(v);
            afterValue();
        }
    }

    @Override
    public void writeEndArray() throws IOException {
        if (testEnd(Type.ARRAY)) {
            delegate.writeEndArray();
            afterValue();
        }
    }

    @Override
    public void writeEndObject() throws IOException {
        if (testEnd(Type.OBJECT)) {
            delegate.writeEndObject();
            afterValue();
        }
    }

    @Override
    public void writeFieldName(final SerializableString name) throws IOException {
        this.fieldName = name.getValue();
        if (test()) {
            delegate.writeFieldName(name);
        }
    }

    @Override
    public void writeFieldName(final String name) throws IOException {
        this.fieldName = name;
        if (test()) {
            delegate.writeFieldName(name);
        }
    }

    @Override
    public void writeNull() throws IOException {
        if (test()) {
            delegate.writeNull();
            afterValue();
        }
    }

    @Override
    public void writeNumber(final BigDecimal v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final BigInteger v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final double v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final float v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final int v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final long v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final short v) throws IOException {
        if (test()) {
            delegate.writeNumber(v);
            afterValue();
        }
    }

    @Override
    public void writeNumber(final String encodedValue) throws IOException, UnsupportedOperationException {
        if (test()) {
            delegate.writeNumber(encodedValue);
            afterValue();
        }
    }

    @Override
    public void writeObjectId(final Object id) throws IOException {
        if (test()) {
            delegate.writeObjectId(id);
            afterValue();
        }
    }

    @Override
    public void writeObjectRef(final Object id) throws IOException {
        if (test()) {
            delegate.writeObjectRef(id);
            afterValue();
        }
    }

    @Override
    public void writeOmittedField(final String fieldName) throws IOException {
        if (test()) {
            delegate.writeOmittedField(fieldName);
        }
    }

    @Override
    public void writeRaw(final char c) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(final char[] text, final int offset, final int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(final SerializableString text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(final String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(final String text, final int offset, final int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawUTF8String(final byte[] text, final int offset, final int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawValue(final char[] text, final int offset, final int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawValue(final String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawValue(final String text, final int offset, final int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeStartArray() throws IOException {
        if (testStartArray()) {
            delegate.writeStartArray();
        }
    }

    @Override
    public void writeStartArray(final int size) throws IOException {
        if (testStartArray()) {
            delegate.writeStartArray(size);
        }
    }

    @Override
    public void writeStartArray(final Object forValue) throws IOException {
        if (testStartArray()) {
            delegate.writeStartArray(forValue);
        }
    }

    @Override
    public void writeStartArray(final Object forValue, final int size) throws IOException {
        if (testStartArray()) {
            delegate.writeStartArray(forValue, size);
        }
    }

    @Override
    public void writeStartObject() throws IOException {
        if (testStartObject()) {
            delegate.writeStartObject();
        }
    }

    @Override
    public void writeStartObject(final Object forValue) throws IOException {
        if (testStartObject()) {
            delegate.writeStartObject(forValue);
        }
    }

    @Override
    public void writeStartObject(final Object forValue, final int size) throws IOException {
        if (testStartObject()) {
            delegate.writeStartObject(forValue, size);
        }
    }

    @Override
    public void writeString(final char[] text, final int offset, final int len) throws IOException {
        if (test()) {
            delegate.writeString(text, offset, len);
            afterValue();
        }
    }

    @Override
    public void writeString(final SerializableString value) throws IOException {
        if (test()) {
            delegate.writeString(value);
            afterValue();
        }
    }

    @Override
    public void writeString(final String value) throws IOException {
        if (test()) {
            delegate.writeString(value);
            afterValue();
        }
    }

    @Override
    public void writeTypeId(final Object id) throws IOException {
        if (test()) {
            delegate.writeTypeId(id);
            afterValue();
        }
    }

    @Override
    public void writeUTF8String(final byte[] text, final int offset, final int length) throws IOException {
        if (test()) {
            delegate.writeUTF8String(text, offset, length);
            afterValue();
        }
    }
}
