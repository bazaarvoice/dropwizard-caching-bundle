package com.bazaarvoice.dropwizard.caching;

import javax.ws.rs.core.Response;

/**
 * HTTP protocol helpers.
 */
class HttpUtils {
    public static Response.StatusType GATEWAY_TIMEOUT = new SimpleStatus(504, "Gateway Timeout");

    private static class SimpleStatus implements Response.StatusType {
        private final int _statusCode;
        private final String _reasonPhrase;
        private final Response.Status.Family _family;

        public SimpleStatus(int statusCode, String reasonPhrase) {
            _statusCode = statusCode;
            _reasonPhrase = reasonPhrase;

            switch (statusCode / 100) {
                case 1:
                    _family = Response.Status.Family.INFORMATIONAL;
                    break;
                case 2:
                    _family = Response.Status.Family.SUCCESSFUL;
                    break;
                case 3:
                    _family = Response.Status.Family.REDIRECTION;
                    break;
                case 4:
                    _family = Response.Status.Family.CLIENT_ERROR;
                    break;
                case 5:
                    _family = Response.Status.Family.SERVER_ERROR;
                    break;
                default:
                    _family = Response.Status.Family.OTHER;
                    break;
            }
        }

        @Override
        public int getStatusCode() {
            return _statusCode;
        }

        @Override
        public Response.Status.Family getFamily() {
            return _family;
        }

        @Override
        public String getReasonPhrase() {
            return _reasonPhrase;
        }
    }
}
