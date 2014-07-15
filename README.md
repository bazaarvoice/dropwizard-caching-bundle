Response caching bundle for Dropwizard resources.

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