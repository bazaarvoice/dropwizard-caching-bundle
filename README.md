Response caching bundle for Dropwizard resources.

# TODO

## Unsupported HTTP Headers

* Vary
    * Right now hard coded to vary on: ACCEPT, ACCEPT-ENCODING, ACCEPT-LANGUAGE, ACCEPT-CHARSET
* Expect
* If-Match, If-None-Match
* If-Modified-Since, If-Unmodified-Since
* Cache-Control: no-cache fields, private fields
    * no-cache with fields and private with fields are simply treated as a bare no-cache/private directive