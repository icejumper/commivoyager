package org.ark.math.commivoyager;

import java.util.List;
import java.util.Set;

import org.ark.math.commivoyager.algorithm.BranchAndBoundStrategy;
import org.ark.math.commivoyager.config.Application;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.ark.math.commivoyager.repository.RouteReader;
import org.ark.math.commivoyager.util.GraphUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by arkadys on 4/11/16.
 */
public class Commivoyager {

    public static void main(String[] args) {
        final ApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class);
        final RouteReader routeReader = ctx.getBean(RouteReader.class);
		final GraphUtils graphUtils = ctx.getBean(GraphUtils.class);

        final Set<CityPair> cityPairs = routeReader.readMatrix();
		final Set<CityPair> seedArrows = graphUtils.cloneRoute(cityPairs);
		
        final BranchAndBoundStrategy strategy = ctx.getBean(BranchAndBoundStrategy.class);
        final List<CityPair> optimizedRoute = strategy.optimize(seedArrows, (City) ctx.getBean("startCity"), BranchAndBoundStrategy.OptimizeBy.DISTANCE_SYMMETRICAL);
        graphUtils.dumpRoute(optimizedRoute);
    }

}
