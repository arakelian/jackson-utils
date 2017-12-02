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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

public final class JacksonOptions {
    private static LoadingCache<JacksonOptions, JsonProcessors> MAPPER_CACHE = CacheBuilder.newBuilder()
            .maximumSize(Integer.MAX_VALUE).build(new CacheLoader<JacksonOptions, JsonProcessors>() {
                @Override
                public JsonProcessors load(final JacksonOptions opts) {
                    // same configuration as default mapper, but with different locale
                    final ObjectMapper mapper = opts.defaultMapper != null ? opts.defaultMapper.copy()
                            : new ObjectMapper();
                    mapper.setLocale(opts.getLocale());

                    // add request-specific modules
                    for (final Module module : opts.getModules()) {
                        mapper.registerModule(module);
                    }

                    for (final MapperFeature feature : opts.getEnabled()) {
                        mapper.enable(feature);
                    }
                    for (final MapperFeature feature : opts.getDisabled()) {
                        mapper.disable(feature);
                    }

                    final Class<?> view = opts.getView();
                    final boolean pretty = opts.isPretty();
                    final ObjectWriter writer;
                    if (view != null) {
                        // view-based rendering
                        writer = pretty ? mapper.writerWithView(view).withDefaultPrettyPrinter()
                                : mapper.writerWithView(view);
                    } else {
                        writer = pretty ? mapper.writerWithDefaultPrettyPrinter() : mapper.writer();
                    }
                    return new JsonProcessors(mapper, writer);
                }
            });
    private Locale locale;
    private Set<Module> modules;
    private Set<MapperFeature> enabled;
    private Set<MapperFeature> disabled;
    private Class<?> view;
    private boolean pretty;
    private boolean sealed;
    private final ObjectMapper defaultMapper;

    private final int defaultMapperCacheBuster;

    public JacksonOptions(final ObjectMapper defaultMapper, final int defaultMapperCacheBuster) {
        // when defaultMapper changes this will suffice as cache busting
        this.defaultMapper = defaultMapper;
        this.defaultMapperCacheBuster = defaultMapperCacheBuster;
    }

    private void assertNotSealed() {
        if (sealed) {
            throw new IllegalStateException("Cannot change JsonOptions once sealed");
        }
    }

    public JsonProcessors build() {
        sealed = true;
        return MAPPER_CACHE.getUnchecked(this);
    }

    public final JacksonOptions disable(final MapperFeature disable) {
        assertNotSealed();
        if (disabled == null) {
            disabled = Sets.newHashSet(disable);
        } else {
            disabled.add(disable);
        }
        return this;
    }

    public final JacksonOptions enable(final MapperFeature enable) {
        assertNotSealed();
        if (enabled == null) {
            enabled = Sets.newHashSet(enable);
        } else {
            enabled.add(enable);
        }
        return this;
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
        final JacksonOptions other = (JacksonOptions) obj;
        if (disabled == null) {
            if (other.disabled != null) {
                return false;
            }
        } else if (!disabled.equals(other.disabled)) {
            return false;
        }
        if (enabled == null) {
            if (other.enabled != null) {
                return false;
            }
        } else if (!enabled.equals(other.enabled)) {
            return false;
        }
        if (defaultMapperCacheBuster != other.defaultMapperCacheBuster) {
            return false;
        }
        if (locale == null) {
            if (other.locale != null) {
                return false;
            }
        } else if (!locale.equals(other.locale)) {
            return false;
        }
        if (modules == null) {
            if (other.modules != null) {
                return false;
            }
        } else if (!modules.equals(other.modules)) {
            return false;
        }
        if (pretty != other.pretty) {
            return false;
        }
        if (sealed != other.sealed) {
            return false;
        }
        if (view == null) {
            if (other.view != null) {
                return false;
            }
        } else if (!view.equals(other.view)) {
            return false;
        }
        return true;
    }

    final Set<MapperFeature> getDisabled() {
        return disabled != null ? disabled : Collections.<MapperFeature> emptySet();
    }

    final Set<MapperFeature> getEnabled() {
        return enabled != null ? enabled : Collections.<MapperFeature> emptySet();
    }

    final Locale getLocale() {
        return locale != null ? locale : Locale.getDefault();
    }

    final Set<Module> getModules() {
        return modules != null ? modules : Collections.<Module> emptySet();
    }

    final Class<?> getView() {
        return view;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (disabled == null ? 0 : disabled.hashCode());
        result = prime * result + (enabled == null ? 0 : enabled.hashCode());
        result = prime * result + defaultMapperCacheBuster;
        result = prime * result + (locale == null ? 0 : locale.hashCode());
        result = prime * result + (modules == null ? 0 : modules.hashCode());
        result = prime * result + (pretty ? 1231 : 1237);
        result = prime * result + (sealed ? 1231 : 1237);
        result = prime * result + (view == null ? 0 : view.hashCode());
        return result;
    }

    public final boolean isPretty() {
        return pretty;
    }

    public final JacksonOptions locale(final Locale locale) {
        assertNotSealed();
        this.locale = locale;
        return this;
    }

    public final JacksonOptions modules(final Module module) {
        assertNotSealed();
        if (this.modules == null) {
            this.modules = Sets.newHashSet(module);
        } else {
            this.modules.add(module);
        }
        return this;
    }

    public final JacksonOptions modules(final Set<Module> modules) {
        assertNotSealed();
        if (this.modules == null) {
            this.modules = Sets.newHashSet(modules);
        } else {
            this.modules.addAll(modules);
        }
        return this;
    }

    public final JacksonOptions pretty(final boolean pretty) {
        assertNotSealed();
        this.pretty = pretty;
        return this;
    }

    public final JacksonOptions view(final Class<?> view) {
        assertNotSealed();
        this.view = view;
        disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        return this;
    }

}
