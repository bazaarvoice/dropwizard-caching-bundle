package com.bazaarvoice.dropwizard.caching;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bundle that sets up request caching for an application's resources.
 */
public class CachingBundle implements Bundle {
    private static final Set<String> SINGLETON_HEADERS = HttpHeaderUtils.headerNames(
            // Jetty sets the Date header automatically to the current time after the request has been
            // processed. Any other attempts to set the date header result in duplicate date headers. The
            // caching layer needs to be able to set the date header to the date the cached response was
            // generated. Duplicate headers are unexpected, confusing, and will likely result in problems
            // for clients.
            HttpHeaders.DATE,

            // The dropwizard @CacheControl annotation will set the cache-control header for each response, even if
            // the response was loaded from the cache and already has a cache-control header. The result is duplicate
            // cache-control headers when the response is served from the cache.
            // This does prevent having multiple cache-control headers, which is technically allowed, but supporting
            // that seems to be tricky.
            HttpHeaders.CACHE_CONTROL
    );

    private final boolean _enabled;
    private final ResponseCache _cache;

    public CachingBundle(CachingConfiguration configuration) {
        checkNotNull(configuration);

        _enabled = configuration.isEnabled();
        _cache = configuration.buildCache();
    }

    public void initialize(Bootstrap<?> bootstrap) {
        // Nothing to do
    }

    @Override
    public void run(Environment environment) {
        if (_enabled) {
            environment.jersey().register(new CacheResourceMethodDispatchAdapter(_cache));

            environment.servlets().addFilter("cache", new Filter() {
                @Override
                public void init(FilterConfig filterConfig) throws ServletException {
                    // Nothing to do
                }

                @Override
                public void doFilter(ServletRequest request, final ServletResponse response, FilterChain chain) throws IOException, ServletException {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;

                    chain.doFilter(request, new HttpServletResponseWrapper(httpResponse) {
                        @Override
                        public void addHeader(String name, String value) {
                            if (SINGLETON_HEADERS.contains(name)) {
                                super.setHeader(name, value);
                            } else {
                                super.addHeader(name, value);
                            }
                        }
                    });
                }

                @Override
                public void destroy() {
                    // Nothing to do
                }
            }).addMappingForUrlPatterns(null, false, "*");
        }
    }
}
