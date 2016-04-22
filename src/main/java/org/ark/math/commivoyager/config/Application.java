package org.ark.math.commivoyager.config;

import java.io.IOException;

import org.ark.math.commivoyager.algorithm.BranchAndBoundStrategy;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.repository.CityFactory;
import org.ark.math.commivoyager.repository.RouteReader;
import org.ark.math.commivoyager.repository.impl.CsvRouteReader;
import org.ark.math.commivoyager.util.GraphUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

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
	@Value("${route.csv.delimiter:;}")
	private char delimiter;

    @Bean
    public RouteReader getRouteReader() throws IOException
    {
        return new CsvRouteReader(csvFilePath, delimiter);
    }

    @Bean
    public BranchAndBoundStrategy getOptimizationStrategy()
    {
        return new BranchAndBoundStrategy();
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
	public GraphUtils graphUtils()
	{
		return new GraphUtils();
	}

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
