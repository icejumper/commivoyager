package org.ark.math.commivoyager;

import org.ark.math.commivoyager.algorithm.OptimizationStrategy;
import org.ark.math.commivoyager.config.Application;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.ark.math.commivoyager.repository.RouteReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Set;

/**
 * Created by arkadys on 4/11/16.
 */
public class Commivoyager {


    public static void main(String[] args) {
        final ApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class);
        final RouteReader routeReader = ctx.getBean(RouteReader.class);

        final Set<CityPair> cityPairs = routeReader.readMatrix();
        final OptimizationStrategy strategy = ctx.getBean(OptimizationStrategy.class);
        final List<City> optimizedRoute = strategy.optimize(cityPairs, (City) ctx.getBean("startCity"), OptimizationStrategy.OptimizeBy.DISTANCE_SYMMETRICAL);
        dumpRoute(optimizedRoute);
    }

    private static void dumpRoute(final List<City> route)
    {
        route.stream().forEach(c -> System.out.print(c.getName() + " -> "));
    }
}
