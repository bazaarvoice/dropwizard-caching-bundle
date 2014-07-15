Response caching bundle for Dropwizard resources.

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