package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
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
            String groupName = null;

            if (groupNameAnn != null && groupNameAnn.value().length() > 0) {
                groupName = groupNameAnn.value();
            }

            // Mapper.apply will not return null, so disable the inspection warning
            //noinspection ConstantConditions
            String cacheControlHeader = _cacheControlMapper.apply(groupName == null ? "default" : groupName).orNull();

            if (groupNameAnn != null || abstractResourceMethod.isAnnotationPresent(io.dropwizard.jersey.caching.CacheControl.class)) {
                dispatcher = new CachingDispatcher(dispatcher, _cache, groupName, cacheControlHeader);
            }

            return dispatcher;
        }
    }

    public static class CachingDispatcher implements RequestDispatcher {
        private final RequestDispatcher _dispatcher;
        private final ResponseCache _cache;
        private final String _cacheControlHeader;
        private final String _groupNameHeader;

        public CachingDispatcher(RequestDispatcher dispatcher, ResponseCache cache, String groupName, String cacheControlHeader) {
            _dispatcher = checkNotNull(dispatcher);
            _cache = checkNotNull(cache);
            _groupNameHeader = groupName == null ? null : "group=\"" + groupName + "\"";
            _cacheControlHeader = cacheControlHeader; // Null is allowed
        }

        @Override
        public void dispatch(Object resource, HttpContext context) {
            try {
                CacheRequestContext request = new CacheRequestContext(context.getRequest());
                Optional<Response> cacheResponse = _cache.get(request);

                if (cacheResponse.isPresent()) {
                    // Throw an exception to try and prevent other dispatchers, plugins, etc from modifying the response
                    throw new WebApplicationException(cacheResponse.get());
                } else {
                    ContainerResponse response = (ContainerResponse) context.getResponse();
                    response.setContainerResponseWriter(new CachingResponseWriter(response.getContainerResponseWriter(), request, _cache, _groupNameHeader, _cacheControlHeader));
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
        private final String _cacheControlHeader;
        private final String _groupNameHeader;
        private ContainerResponse _response;
        private ByteArrayOutputStream _buffer;

        public CachingResponseWriter(ContainerResponseWriter wrapped, CacheRequestContext request, ResponseCache cache, String groupNameHeader, String cacheControlHeader) {
            _wrapped = checkNotNull(wrapped);
            _request = checkNotNull(request);
            _cache = checkNotNull(cache);
            _groupNameHeader = groupNameHeader; // Null is allowed
            _cacheControlHeader = cacheControlHeader; // Null is allowed
        }

        @Override
        public OutputStream writeStatusAndHeaders(long contentLength, ContainerResponse response) throws IOException {
            _response = response;
            _buffer = new ByteArrayOutputStream(contentLength < 0 ? 128 : (int) contentLength);
            return _buffer;
        }

        @Override
        public void finish() throws IOException {
            if (_cacheControlHeader != null) {
                // This needs to be done here and not in the RequestDispatcher to ensure that it overrides any other
                // options set
                _response.getHttpHeaders().putSingle(CACHE_CONTROL, _cacheControlHeader);
            } else if (_groupNameHeader != null) {
                _response.getHttpHeaders().add(CACHE_CONTROL, _groupNameHeader);
            }

            byte[] content = _buffer.toByteArray();
            CacheResponseContext response = new CacheResponseContext(_response);
            _cache.put(_request, response, content);

            OutputStream wrappedStream = _wrapped.writeStatusAndHeaders(content.length, _response);
            wrappedStream.write(content);
            _wrapped.finish();
        }
    }
}
