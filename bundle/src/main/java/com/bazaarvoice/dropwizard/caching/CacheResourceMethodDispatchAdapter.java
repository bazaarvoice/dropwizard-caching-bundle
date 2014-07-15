package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;

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

            if (groupNameAnn != null || abstractResourceMethod.isAnnotationPresent(io.dropwizard.jersey.caching.CacheControl.class)) {
                String groupName = groupNameAnn == null ? "" : groupNameAnn.value();
                dispatcher = new CachingDispatcher(dispatcher, _cache, _cacheControlMapper.apply(groupName));
            } else if (abstractResourceMethod.getHttpMethod().equals("GET")) {
                Optional<String> cacheControlOverride = _cacheControlMapper.apply("");

                if (cacheControlOverride != null && cacheControlOverride.isPresent()) {
                    dispatcher = new CachingDispatcher(dispatcher, _cache, cacheControlOverride);
                }
            }

            return dispatcher;
        }
    }

    public static class CachingDispatcher implements RequestDispatcher {
        private final RequestDispatcher _dispatcher;
        private final ResponseCache _cache;
        private final Optional<String> _cacheControlHeader;

        public CachingDispatcher(RequestDispatcher dispatcher, ResponseCache cache, Optional<String> cacheControlHeader) {
            _dispatcher = checkNotNull(dispatcher);
            _cache = checkNotNull(cache);
            _cacheControlHeader = checkNotNull(cacheControlHeader);
        }

        @Override
        public void dispatch(Object resource, HttpContext context) {
            try {
                CacheRequestContext request = CacheRequestContext.build((ContainerRequest) context.getRequest());
                Optional<Response> cacheResponse = _cache.get(request);

                if (cacheResponse.isPresent()) {
                    // Throw an exception to try and prevent other dispatchers, plugins, etc from modifying the response
                    throw new WebApplicationException(cacheResponse.get());
                } else {
                    ContainerResponse response = (ContainerResponse) context.getResponse();
                    response.setContainerResponseWriter(new CachingResponseWriter(response.getContainerResponseWriter(), request, _cache, _cacheControlHeader));
                    _dispatcher.dispatch(resource, context);
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
