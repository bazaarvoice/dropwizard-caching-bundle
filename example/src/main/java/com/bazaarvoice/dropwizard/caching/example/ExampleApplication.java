package com.bazaarvoice.dropwizard.caching.example;

import com.bazaarvoice.dropwizard.caching.CachingBundle;
import com.bazaarvoice.dropwizard.caching.CachingConfiguration;
import com.bazaarvoice.dropwizard.caching.LocalCacheConfiguration;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

public class ExampleApplication extends Application<Configuration> {
    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new CachingBundle(new CachingConfiguration()
                        .local(new LocalCacheConfiguration()
                                        .maximumSize(100)
                                        .expire(Duration.seconds(10))
                        )
                )
        );
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        environment.jersey().register(new ExampleResource());
    }

    public static void main(String[] args) throws Exception {
        new ExampleApplication().run(args);
    }
}
