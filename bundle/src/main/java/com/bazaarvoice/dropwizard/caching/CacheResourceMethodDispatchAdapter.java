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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import com.yammer.dropwizard.jersey.caching.CacheControl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.VARY;

/**
 * Wraps resource methods that are configured for request caching.
 */
@Provider
public class CacheResourceMethodDispatchAdapter implements ResourceMethodDispatchAdapter {
    private final ResponseCache _cache;
    private final Function<String, Optional<String>> _cacheControlMapper;

    public CacheResourceMethodDispatchAdapter(ResponseCache cache, Function<String, Optional<String>> cacheControlMapper) {
        _cache = checkNotNull(cache);
        _cacheControlMapper = checkNotNull(cacheControlMapper);
    }

    public ResourceMethodDispatchProvider adapt(ResourceMethodDispatchProvider provider) {
        return new DispatchProvider(provider, _cache, _cacheControlMapper);
    }

    public static class DispatchProvider implements ResourceMethodDispatchProvider {
        private final ResourceMethodDispatchProvider _provider;
        private final ResponseCache _cache;
        private final Function<String, Optional<String>> _cacheControlMapper;

        public DispatchProvider(ResourceMethodDispatchProvider provider, ResponseCache cache, Function<String, Optional<String>> cacheControlMapper) {
            _provider = checkNotNull(provider);
            _cache = checkNotNull(cache);
            _cacheControlMapper = checkNotNull(cacheControlMapper);
        }

        @Override
        public RequestDispatcher create(AbstractResourceMethod abstractResourceMethod) {
            RequestDispatcher dispatcher = _provider.create(abstractResourceMethod);
            CacheGroup groupNameAnn = abstractResourceMethod.getAnnotation(CacheGroup.class);
            Vary varyAnn = abstractResourceMethod.getAnnotation(Vary.class);
            IncludeBodyInCacheKey includeBodyInCacheKeyAnn = abstractResourceMethod.getAnnotation(IncludeBodyInCacheKey.class);

            Set<String> vary = ImmutableSet.of();

            if (varyAnn != null && varyAnn.value() != null) {
                vary = HttpHeaderUtils.headerNames(Iterables.filter(
                        Arrays.asList(varyAnn.value()),
                        Predicates.notNull()));
            }

            boolean includeBodyInCacheKey = includeBodyInCacheKeyAnn != null && includeBodyInCacheKeyAnn.enabled();

            if (groupNameAnn != null || abstractResourceMethod.isAnnotationPresent(CacheControl.class)) {
                String groupName = groupNameAnn == null ? "" : groupNameAnn.value();
                dispatcher = new CachingDispatcher(dispatcher, _cache, _cacheControlMapper.apply(groupName), vary, includeBodyInCacheKey);
            } else if (abstractResourceMethod.getHttpMethod().equals("GET")) {
                Optional<String> cacheControlOverride = _cacheControlMapper.apply("");

                if (cacheControlOverride != null && cacheControlOverride.isPresent()) {
                    dispatcher = new CachingDispatcher(dispatcher, _cache, cacheControlOverride, vary, includeBodyInCacheKey);
                }
            }

            return dispatcher;
        }
    }

    public static class CachingDispatcher implements RequestDispatcher {
        private final RequestDispatcher _dispatcher;
        private final ResponseCache _cache;
        private final Optional<String> _cacheControlHeader;
        private final Set<String> _vary;
        private final String _varyHeader;
        private final boolean _includeBodyInCacheKey;

        public CachingDispatcher(RequestDispatcher dispatcher, ResponseCache cache, Optional<String> cacheControlHeader, Set<String> vary, boolean includeBodyInCacheKey) {
            _dispatcher = checkNotNull(dispatcher);
            _cache = checkNotNull(cache);
            _cacheControlHeader = checkNotNull(cacheControlHeader);
            _vary = checkNotNull(vary);
            _varyHeader = vary.size() == 0 ? "" : Joiner.on(", ").join(_vary);
            _includeBodyInCacheKey = includeBodyInCacheKey;
        }

        @Override
        public void dispatch(Object resource, HttpContext context) {
            try {
                if (_vary.contains("*")) {
                    // Response varies on aspects besides the HTTP request headers. Therefore, the
                    // response can not be provided from a cache.
                    _dispatcher.dispatch(resource, context);
                    context.getResponse().getHttpHeaders().add(VARY, _varyHeader);
                    return;
                }

                CacheRequestContext request = CacheRequestContext.build((ContainerRequest) context.getRequest(), _vary, _includeBodyInCacheKey);
                Optional<Response> cacheResponse = _cache.get(request);

                if (cacheResponse.isPresent()) {
                    // Throw an exception to try and prevent other dispatchers, plugins, etc from modifying the response
                    throw new WebApplicationException(cacheResponse.get());
                } else {
                    ContainerResponse response = (ContainerResponse) context.getResponse();
                    response.setContainerResponseWriter(new CachingResponseWriter(response.getContainerResponseWriter(), request, _cache, _cacheControlHeader));
                    _dispatcher.dispatch(resource, context);
                    context.getResponse().getHttpHeaders().add(VARY, _varyHeader);
                }
            } catch (Exception ex) {
                throw Throwables.propagate(ex);
            }
        }
    }

    private static class CachingResponseWriter implements ContainerResponseWriter {
        private final ContainerResponseWriter _wrapped;
        private final ResponseCache _cache;
        private final CacheRequestContext _request;
        private final Optional<String> _cacheControlHeader;
        private ContainerResponse _response;
        private ByteArrayOutputStream _buffer;

        public CachingResponseWriter(ContainerResponseWriter wrapped, CacheRequestContext request, ResponseCache cache, Optional<String> cacheControlHeader) {
            _wrapped = checkNotNull(wrapped);
            _request = checkNotNull(request);
            _cache = checkNotNull(cache);
            _cacheControlHeader = checkNotNull(cacheControlHeader);
        }

        @Override
        public OutputStream writeStatusAndHeaders(long contentLength, ContainerResponse response) throws IOException {
            _response = response;
            _buffer = new ByteArrayOutputStream(contentLength < 0 ? 128 : (int) contentLength);
            return _buffer;
        }

        @Override
        public void finish() throws IOException {
            byte[] content = _buffer.toByteArray();
            int statusCode = _response.getStatus();

            // See http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.4
            if (statusCode == 200 || statusCode == 203 || statusCode == 206 || statusCode == 300 || statusCode == 301 || statusCode == 410) {
                if (_cacheControlHeader.isPresent()) {
                    // This needs to be done here and not in the RequestDispatcher to ensure that it overrides any other
                    // options set
                    _response.getHttpHeaders().putSingle(CACHE_CONTROL, _cacheControlHeader.get());
                }

                CacheResponseContext response = new CacheResponseContext(_response);
                _cache.put(_request, response, content);
            } else {
                _response.getHttpHeaders().remove(CACHE_CONTROL);
            }

            // This must be done after the cache put to ensure all the headers are set correctly
            OutputStream wrappedStream = _wrapped.writeStatusAndHeaders(content.length, _response);
            wrappedStream.write(content);
            _wrapped.finish();
        }
    }
}
