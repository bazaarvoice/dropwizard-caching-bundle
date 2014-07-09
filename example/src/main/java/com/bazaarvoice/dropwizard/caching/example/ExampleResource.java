package com.bazaarvoice.dropwizard.caching.example;

import com.sun.jersey.api.core.HttpContext;
import io.dropwizard.jersey.caching.CacheControl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api")
public class ExampleResource {
    private int _count = 1;

    @GET
    @Path("/test")
    @CacheControl(maxAge = 60)
    public ExampleResult getTestData(@Context HttpContext requestContext) {
//        throw new RuntimeException("uh oh");
        return new ExampleResult(_count++);
    }

    public static class ExampleResult {
        public int resultValue = 100;

        public ExampleResult(int value) {
            resultValue = value;
        }
    }
}
