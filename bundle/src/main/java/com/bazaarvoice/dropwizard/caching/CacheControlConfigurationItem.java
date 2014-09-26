/*
 * Copyright 2014 Bazaarvoice, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.util.Duration;

import javax.ws.rs.core.CacheControl;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Cache control options to apply to an endpoint.
 */
public class CacheControlConfigurationItem {
    private Optional<String> _group = Optional.absent();
    private Optional<Pattern> _groupRegex = Optional.absent();

    private Optional<Duration> _maxAge = Optional.absent();
    private Optional<Duration> _sharedMaxAge = Optional.absent();
    private Set<CacheControlFlag> _flags = ImmutableSet.of();
    private Set<String> _privateFields = ImmutableSet.of();
    private Set<String> _noCacheFields = ImmutableSet.of();
    private Map<String, String> _cacheExtensions = ImmutableMap.of();

    public Optional<Pattern> getGroupRegex() {
        return _groupRegex;
    }

    @JsonProperty
    public void setGroupRegex(Optional<Pattern> groupRegex) {
        checkNotNull(groupRegex);
        checkState(groupRegex.isPresent() || _group.isPresent(), "only one of group or groupRegex can be specified");
        _groupRegex = groupRegex;
    }

    public Optional<String> getGroup() {
        return _group;
    }

    @JsonProperty
    public void setGroup(Optional<String> group) {
        checkNotNull(group);
        checkState(group.isPresent() || _group.isPresent(), "only one of group or groupRegex can be specified");
        _group = group;
    }

    public Optional<Duration> getMaxAge() {
        return _maxAge;
    }

    @JsonProperty
    public void setMaxAge(Optional<Duration> maxAge) {
        checkNotNull(maxAge);
        checkArgument(!maxAge.isPresent() || maxAge.get().getQuantity() >= 0, "maxAge must be >= 0");
        _maxAge = maxAge;
    }

    public Optional<Duration> getSharedMaxAge() {
        return _sharedMaxAge;
    }

    @JsonProperty
    public void setSharedMaxAge(Optional<Duration> sharedMaxAge) {
        checkNotNull(sharedMaxAge);
        checkArgument(!sharedMaxAge.isPresent() || sharedMaxAge.get().getQuantity() >= 0, "sharedMaxAge must be >= 0");
        _sharedMaxAge = sharedMaxAge;
    }

    public Set<CacheControlFlag> getFlags() {
        return _flags;
    }

    @JsonProperty
    public void setFlags(Set<CacheControlFlag> flags) {
        checkNotNull(flags);
        checkArgument(!flags.contains(null), "flags must not contain null");
        _flags = ImmutableSet.copyOf(flags);
    }

    public Set<String> getPrivateFields() {
        return _privateFields;
    }

    @JsonProperty("private")
    public void setPrivateFields(Set<String> privateFields) {
        checkNotNull(privateFields);
        checkArgument(!privateFields.contains(null), "privateFields must not contain null");
        _privateFields = ImmutableSet.copyOf(privateFields);
    }

    public Set<String> getNoCacheFields() {
        return _noCacheFields;
    }

    @JsonProperty("noCache")
    public void setNoCacheFields(Set<String> noCacheFields) {
        checkNotNull(noCacheFields);
        checkArgument(!noCacheFields.contains(null), "noCacheFields must not contain null");
        _noCacheFields = ImmutableSet.copyOf(noCacheFields);
    }

    public Map<String, String> getCacheExtensions() {
        return _cacheExtensions;
    }

    @JsonProperty("extensions")
    public void setCacheExtensions(Map<String, String> cacheExtensions) {
        checkNotNull(cacheExtensions);
        _cacheExtensions = ImmutableMap.copyOf(cacheExtensions);
    }

    public CacheControl buildCacheControl() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false); // Default is true

        if (_maxAge.isPresent()) {
            cacheControl.setMaxAge((int) _maxAge.get().toSeconds());
        }

        if (_sharedMaxAge.isPresent()) {
            cacheControl.setSMaxAge((int) _maxAge.get().toSeconds());
        }

        if (_privateFields.size() > 0) {
            cacheControl.setPrivate(true);
            cacheControl.getPrivateFields().addAll(_privateFields);
        }

        if (_noCacheFields.size() > 0) {
            cacheControl.setNoCache(true);
            cacheControl.getNoCacheFields().addAll(_noCacheFields);
        }

        for (CacheControlFlag flag : _flags) {
            switch (flag) {
                case NO_CACHE:
                    cacheControl.setNoCache(true);
                    break;

                case NO_STORE:
                    cacheControl.setNoStore(true);
                    break;

                case MUST_REVALIDATE:
                    cacheControl.setMustRevalidate(true);
                    break;

                case PROXY_REVALIDATE:
                    cacheControl.setProxyRevalidate(true);
                    break;

                case NO_TRANSFORM:
                    cacheControl.setNoTransform(true);
                    break;

                case PRIVATE:
                    cacheControl.setPrivate(true);
                    break;

                case PUBLIC:
                    // public is not directly supported by the CacheControl object, so use extension map
                    cacheControl.getCacheExtension().put("public", "");
                    break;

                default:
                    checkState(false, "Unhandled cache control flag: " + flag);
                    break;
            }
        }

        // Although the docs don't state it explicitly, both null and empty string get converted to a bare directive
        // for cache extensions.
        cacheControl.getCacheExtension().putAll(_cacheExtensions);
        return cacheControl;
    }

    public Predicate<String> buildGroupMatcher() {
        Predicate<String> matcher;

        if (_groupRegex.isPresent()) {
            matcher = regexMatcher(_groupRegex.get());
        } else {
            matcher = groupNameMatcher(_group.or("*"));
        }

        return matcher;
    }

    private static Predicate<String> groupNameMatcher(String name) {
        if (name.equals("*")) {
            return Predicates.alwaysTrue();
        }

        return regexMatcher(Pattern.compile("" +
                        "^" +
                        Joiner.on(".*").join(
                                FluentIterable
                                        .from(Splitter.on('*').split(name))
                                        .transform(new Function<String, Object>() {
                                            public Object apply(String input) {
                                                return input.length() == 0
                                                        ? input
                                                        : Pattern.quote(input);
                                            }
                                        })
                        )
                        + "$",
                Pattern.CASE_INSENSITIVE
        ));
    }

    private static Predicate<String> regexMatcher(final Pattern pattern) {
        return new Predicate<String>() {
            public boolean apply(String input) {
                return pattern.matcher(input).find();
            }
        };
    }
}
