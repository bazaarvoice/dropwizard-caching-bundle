package com.bazaarvoice.dropwizard.caching.example;

import com.bazaarvoice.dropwizard.caching.CachingBundle;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class ExampleService extends Service<ExampleConfiguration> {
    @Override
    public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
        bootstrap.addBundle(new CachingBundle());
    }

    @Override
    public void run(ExampleConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(ExampleResource.class);
    }

    public static void main(String[] args) throws Exception {
        new ExampleService().run(args);
    }
}
