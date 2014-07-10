package com.bazaarvoice.dropwizard.caching.example;

import com.bazaarvoice.dropwizard.caching.CachingBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ExampleApplication extends Application<ExampleConfiguration> {
    @Override
    public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
        bootstrap.addBundle(new CachingBundle());
    }

    @Override
    public void run(ExampleConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new ExampleResource());
    }

    public static void main(String[] args) throws Exception {
        new ExampleApplication().run(args);
    }
}
