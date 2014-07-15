Response caching bundle for Dropwizard resources.

There are two functions supported by the bundle: generate cache-control options for resources and caching responses.
The cache-control generation can be used without the response caching. For example, if there is already an upstream
HTTP caching proxy.

# Initialize

1. Add the maven dependency:

    ```xml
    <dependency>
        <groupId>com.bazaarvoice.dropwizard</groupId>
        <artifactId>dropwizard-caching-bundle</artifactId>
        <version>${dropwizard-caching-bundle.version}</version>
    </dependency>
    ```
2. Modify your application configuration class to implement `com.bazaarvoice.dropwizard.caching.CachingBundleConfiguration`
3. Load the bundle when during application bootstrap:

    ```java
    public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
        bootstrap.addBundle(new CachingBundle());
    }
    ```
    
At this point, the caching bundle has been loaded into the application. However, the caching bundle has no effect until
configured.

# Configuration

## Cache Control

The first step in configuring caching is to specify the caching options for different resource methods.
 
```yaml
# A list of caching configuration options. The first set of options that match a resource will be
# used.
cacheControl:
      # Only one of group or groupRegex can be specified. If neither is specified, the settings
      # will be used for all resources that were not matched by previous settings and all GET
      # methods without an explicit group name.
    - group: pattern    # Optional. Simple pattern that matches cache group name. * can be used
                        # to match 0 or more characters.
      groupRegex: regex # Optional. Regular expression that matches cache group name.
      
      # For more detailed info on the following cache-control options,
      # see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9
      
      maxAge: duration          # Optional. Max time that the response can be cached.
                                # Example: 5m (5 minutes), 30s (30 seconds)
                                # Resolution is seconds. Partial seconds are rounded down.
                                # Supported suffixes: s (seconds), m (minutes), h (hours), d (days)
                                # Although you can set this value higher, the max recommended
                                # value by the HTTP standard is 1 year (365d).
      sharedMaxAge: duration    # Optional. Max time that the response can be cached in a
                                # shared cache. If specified, this value is used by the caching
                                # layer. Otherwise, maxAge is used. A shared cache is one where a
                                # cached response can be returned to multiple clients. A private
                                # cache is usually the local cache where the request originated
                                # whereas the shared cache is usually the caching proxy server.
                                # The same formatting rules that apply to maxAge apply to this
                                # option.
      flags:                    # Set of caching directives
        - no-cache              # A cache MUST NOT use the response to satisfy a subsequent
                                # request without successful revalidation with the origin server.
        - no-store              # A cache MUST NOT store any part of either the response or the
                                # request that elicited it.
        - must-revalidate       # A cache MUST NOT use the cached response after it has expired.
                                # Because a cache can be configured to ignore cache expiration and
                                # the client can specify the max-stale option, this flag provides
                                # a mechanism to require revalidation of stale entries.
        - proxy-revalidate      # Has the same meaning as must-revalidate, but only applies to
                                # shared caches.
        - no-transform          # An intermediate cache or proxy MUST NOT change the headers that
                                # are subject to the no-transform directive or the response body
                                # itself.
                                # http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.5.2
        - private               # Indicates that all or part of the response message is intended
                                # for a single user and MUST NOT be cached by a shared cache.
        - public                # Indicates that the response MAY be cached by any cache, even if
                                # it would normally be non-cacheable or cacheable only within a
                                # non-shared cache.
                                 
      private:                  # List of header fields in the response that are intended for a
                                # single user and MUST NOT be cached by a shared cache. Setting
                                # this option automatically sets the private flag.
        - Authorization
        
      noCache:                  # List of header fields that MUST NOT be sent in the response to a
                                # subsequent request without successful revalidation with the
                                # origin server.
        - Content-Type
        
      extensions:               # Custom cache-control directives.
        bare:                   # With no or empty value, output is a bare directive
        has-value: 17           # With a value, output is has-value="17"
```

# TODO

## Short Term

* More logging
* Metrics reporting
* Testing
* Documentation
* More efficient operation when caching is not being used, but cache-control header is being generated
    * No need to capture response content or deal with ResponseCache at all

## Long Term

* Custom cache key generator implementation

## Unsupported HTTP Headers

* Vary
    * Right now hard coded to vary on: ACCEPT, ACCEPT-ENCODING, ACCEPT-LANGUAGE, ACCEPT-CHARSET
* Expect
* If-Match, If-None-Match
* If-Modified-Since, If-Unmodified-Since
* Cache-Control: no-cache fields, private fields
    * no-cache with fields and private with fields are simply treated as a bare no-cache/private directive