package com.bazaarvoice.dropwizard.caching;

import com.google.common.base.Throwables;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.api.core.HttpResponseContext;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.spi.container.ResourceMethodDispatchAdapter;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.ws.http.HTTPException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps resource methods that are configured for request caching.
 */
@Provider
public class CacheResourceMethodDispatchAdapter implements ResourceMethodDispatchAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CacheResourceMethodDispatchAdapter.class);
    private static final CacheControl DEFAULT_CACHE_CONTROL = new CacheControl();
    private final ResponseCache _cache;

    public CacheResourceMethodDispatchAdapter(ResponseCache cache) {
        _cache = checkNotNull(cache);
    }

    public ResourceMethodDispatchProvider adapt(ResourceMethodDispatchProvider provider) {
        return new DispatchProvider(provider, _cache);
    }

    public static class DispatchProvider implements ResourceMethodDispatchProvider {
        private final ResourceMethodDispatchProvider _provider;
        private final ResponseCache _cache;

        public DispatchProvider(ResourceMethodDispatchProvider provider, ResponseCache cache) {
            _provider = checkNotNull(provider);
            _cache = checkNotNull(cache);
        }

        @Override
        public RequestDispatcher create(AbstractResourceMethod abstractResourceMethod) {
            RequestDispatcher dispatcher = _provider.create(abstractResourceMethod);

            if (abstractResourceMethod.isAnnotationPresent(io.dropwizard.jersey.caching.CacheControl.class)) {
                dispatcher = new CachingDispatcher(dispatcher, _cache);
            }

            return dispatcher;
        }
    }

    public static class CachingDispatcher implements RequestDispatcher {
        private final RequestDispatcher _dispatcher;
        private final ResponseCache _cache;

        public CachingDispatcher(RequestDispatcher dispatcher, ResponseCache cache) {
            _dispatcher = checkNotNull(dispatcher);
            _cache = checkNotNull(cache);
        }

        @Override
        public void dispatch(Object resource, HttpContext context) {
            try {
                HttpRequestContext request = context.getRequest();
                final CacheControl requestCacheConfig = extractCacheControl(request);

                if (CacheControlUtils.isRequestIgnoreCache(requestCacheConfig)) {
                    // Request can not be served from the cache and the response can not be cached.
                    // Bypass the caching code completely and just directly dispatch the request.
                    _dispatcher.dispatch(resource, context);
                    return;
                }

                if (CacheControlUtils.isRequestServableFromCache(requestCacheConfig)) {
                    CachedResponse cachedResponse = _cache.get(request).orNull();

                    if (!isExpired(cachedResponse, CacheControlUtils.getSharedCacheMaxAge(requestCacheConfig))) {
                        LOG.info("Serving response from cache");

                        Response.ResponseBuilder responseBuilder = Response
                                .status(cachedResponse.getStatusCode())
                                .entity(cachedResponse.getResponseContent())
                                .header("Age", HttpHeaderUtils.toAge(cachedResponse.getDate(), DateTime.now()));

                        for (Map.Entry<String, List<String>> header : cachedResponse.getResponseHeaders().entrySet()) {
                            for (String headerValue : header.getValue()) {
                                responseBuilder.header(header.getKey(), headerValue);
                            }
                        }

                        context.getResponse().setResponse(responseBuilder.build());
                        return;
                    }
                }

                if (requestCacheConfig.getCacheExtension().containsKey("only-if-cached")) {
                    context.getResponse().setResponse(Response.status(HttpUtils.GATEWAY_TIMEOUT).build());
                    return;
                }

                ContainerResponse response = (ContainerResponse) context.getResponse();
                response.setContainerResponseWriter(new CachingResponseWriter(response.getContainerResponseWriter(), _cache));
                _dispatcher.dispatch(resource, context);
            } catch (Exception ex) {
                throw Throwables.propagate(ex);
            }
        }
    }

    private static class CachingResponseWriter implements ContainerResponseWriter {
        private final ContainerResponseWriter _wrapped;
        private final ResponseCache _cache;
        private ContainerResponse _response;
        private ByteArrayOutputStream _buffer;

        public CachingResponseWriter(ContainerResponseWriter wrapped, ResponseCache cache) {
            _wrapped = checkNotNull(wrapped);
            _cache = checkNotNull(cache);
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
            MultivaluedMap<String, Object> responseHeaders = _response.getHttpHeaders();
            CacheControl responseCacheConfig = extractCacheControl(_response);

            if (!responseHeaders.containsKey(HttpHeaders.ETAG)) {
                responseHeaders.putSingle(HttpHeaders.ETAG, ETagUtils.generateETag(content));
            }

            if (!responseHeaders.containsKey(HttpHeaders.CONTENT_LENGTH)) {
                responseHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, content.length);
            }

            if (CacheControlUtils.isResponseCacheable(responseCacheConfig)) {
                int maxAgeSecs = CacheControlUtils.getSharedCacheMaxAge(responseCacheConfig);

                if (maxAgeSecs > 0) {
                    DateTime responseDate = HttpHeaderUtils
                            .getDateHeader(responseHeaders, HttpHeaders.DATE)
                            .orNull();

                    if (responseDate == null) {
                        responseDate = DateTime.now();
                        HttpHeaderUtils.setDateHeader(responseHeaders, HttpHeaders.DATE, responseDate);
                        responseHeaders.putSingle("Age", "0");
                    } else {
                        responseHeaders.putSingle("Age", HttpHeaderUtils.toAge(responseDate, DateTime.now()));
                    }

                    HttpHeaderUtils.setDateHeader(responseHeaders, HttpHeaders.EXPIRES, responseDate.plusSeconds(maxAgeSecs));
                    _cache.put(_response.getContainerRequest(), _response, content);
                }
            }

            OutputStream wrappedStream = _wrapped.writeStatusAndHeaders(content.length, _response);
            wrappedStream.write(content);
            _wrapped.finish();
        }
    }

    private static boolean isExpired(CachedResponse value, int maxAgeSecs) {
        if (value == null || maxAgeSecs == 0) {
            return true;
        } else if (maxAgeSecs < 0) {
            return false;
        }

        return value.getDate().plusSeconds(maxAgeSecs).isBefore(DateTime.now());
    }

    private static CacheControl extractCacheControl(HttpRequestContext request) {
        return HttpHeaderUtils
                .getCacheControl(request.getRequestHeaders())
                .or(DEFAULT_CACHE_CONTROL);
    }

    private static CacheControl extractCacheControl(HttpResponseContext response) {
        return HttpHeaderUtils
                .getCacheControl(response.getHttpHeaders())
                .or(DEFAULT_CACHE_CONTROL);
    }
}
