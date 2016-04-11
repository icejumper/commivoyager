package org.ark.math.commivoyager.config;

import org.ark.math.commivoyager.algorithm.OptimizationStrategy;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.repository.CityFactory;
import org.ark.math.commivoyager.repository.RouteReader;
import org.ark.math.commivoyager.repository.impl.CsvRouteReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;

/**
 * Created by arkadys on 4/11/16.
 */
@Configuration
@PropertySource("classpath:/META-INF/default.properties")
public class Application {

    @Value("${start.city.name}")
    private String startCityName;
    @Value("${route.csv.file.path}")
    private String csvFilePath;

    @Bean
    public RouteReader getRouteReader() throws IOException
    {
        return new CsvRouteReader(csvFilePath);
    }

    @Bean
    public OptimizationStrategy getOptimizationStrategy()
    {
        return new OptimizationStrategy();
    }

    @Bean
    public CityFactory getCityFactory()
    {
        return new CityFactory();
    }

    @Bean
    public City startCity()
    {
        return getCityFactory().createCity(startCityName);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
