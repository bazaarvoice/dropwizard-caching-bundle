package com.bazaarvoice.dropwizard.caching.example;

import io.dropwizard.jersey.caching.CacheControl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class ExampleResource {
    private int _count = 1;

    @GET
    @Path("/test")
    @CacheControl(maxAge = 60)
    public ExampleResult getTestData() {
        return new ExampleResult(_count++);
    }

    @GET
    @Path("/other")
    public Response otherTestData() {
        return Response.ok()
                .header("Date", new Date(2014, 4, 1))
                .build();
    }

    public static class ExampleResult {
        public int resultValue = 100;

        public ExampleResult(int value) {
            resultValue = value;
        }
    }
}
