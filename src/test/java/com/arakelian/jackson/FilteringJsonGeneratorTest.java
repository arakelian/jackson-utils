package com.arakelian.jackson;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arakelian.jackson.utils.JacksonUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

public class FilteringJsonGeneratorTest {
    public static class FilterParser {
        private static final Splitter COMMA_SPLITTER = Splitter.on(Pattern.compile("\\s*,\\s*")).trimResults()
                .omitEmptyStrings();

        private final String resource;
        private int lineNo;

        public FilterParser(final String resource) {
            this.resource = resource;
            this.lineNo = 0;
        }

        public Collection<Object[]> data() throws IOException {
            final URL url = FilterParser.class.getResource(resource);
            assertTrue(url != null, "Resource does not exist: " + resource);
            final String content = Resources.toString(url, Charsets.UTF_8);

            final List<Object[]> data = Lists.newArrayList();

            try (BufferedReader r = new BufferedReader(new StringReader(content))) {
                for (;;) {
                    lineNo++;
                    final String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    if (skip(line)) {
                        continue;
                    }

                    final int lineNumber = lineNo;
                    final Set<String> includes = split(line);

                    lineNo++;
                    final Set<String> excludes = split(r.readLine());
                    Preconditions.checkState(!skip(line), "Expected exclude predicate");

                    final String input = readBlock(r);
                    final String expected = readBlock(r);
                    final String testName = "Line " + lineNumber + ": +" + includes + " / -" + excludes;
                    data.add(new Object[] { testName, includes, excludes, input, expected.toString() });
                }
            }
            return data;
        }

        private String readBlock(final BufferedReader r) throws IOException {
            final StringBuilder json = new StringBuilder();
            for (;;) {
                lineNo++;
                final String line = r.readLine();
                final boolean notEmpty = json.length() != 0;
                if (skip(line)) {
                    if (line == null || notEmpty) {
                        break;
                    }
                    continue;
                }
                if (notEmpty) {
                    json.append('\n');
                }
                json.append(line);
                if ("}".equals(line) || "]".equals(line)) {
                    break;
                }
            }
            return json.toString();
        }

        private boolean skip(final String line) {
            if (line == null) {
                return true;
            }
            final int length = line.length();
            if (length == 0) {
                return true;
            }
            for (int i = 0; i < length; i++) {
                final char ch = line.charAt(i);
                if (ch == ' ' || ch == '\t') {
                    continue;
                }
                if (ch == '#') {
                    return true;
                }
                break;
            }
            return false;
        }

        private Set<String> split(final String path) {
            final List<String> paths = COMMA_SPLITTER
                    .splitToList(StringUtils.defaultString(StringUtils.equals(path, "empty") ? null : path));
            return ImmutableSet.copyOf(paths);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FilteringJsonGeneratorTest.class);

    public static Collection<Object[]> data() throws IOException {
        return new FilterParser("/filter.test").data();
    }

    private String filter(final String json, final Set<String> include, final Set<String> exclude)
            throws IOException {
        final StringWriter writer = new StringWriter();
        final JsonFactory factory = JacksonUtils.getObjectMapper().getFactory();
        final JsonGenerator generator = factory.createGenerator(writer).useDefaultPrettyPrinter();
        try (final FilteringJsonGenerator filtering = new FilteringJsonGenerator(generator, include,
                exclude)) {
            final JsonNode node = JacksonUtils.readValue(json, JsonNode.class);
            filtering.writeObject(node);
            return writer.toString();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(
            final String testName,
            final Set<String> includes,
            final Set<String> excludes,
            final String input,
            final String expected) throws IOException {
        LOGGER.debug("Starting {}", testName);
        Assertions.assertEquals(expected, filter(input, includes, excludes));
    }
}
