/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2013 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 */
package org.ark.math.commivoyager.algorithm;

import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.ark.math.commivoyager.InconsistentRouteException;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.*;
import static org.ark.math.commivoyager.model.CityPair.EstimatedCostComparator;

public class BranchAndBoundStrategy
{
	private static final Logger logger = LoggerFactory.getLogger(BranchAndBoundStrategy.class);
	private Long lowBoundCost;
	private Long upperBoundCost;

	public enum OptimizeBy 
	{
		DISTANCE,
		DISTANCE_SYMMETRICAL
	}

	public List<City> optimize(final Set<CityPair> seedRoute, final City startCity, final OptimizeBy optimizeBy)
	{
		Preconditions.checkArgument(CollectionUtils.isNotEmpty(seedRoute), "The source route (CityPair collection) must not be null or empty");

		final Set<CityPair> normalizedRoute = normalizeRoute(seedRoute, optimizeBy);
		final Set<CityPair> routeToOptimize = normalizedRoute.stream().filter(p -> !p.isRouteOptimized()).collect(toSet());

		final Set<CityPair> originalRoute = cloneRoute(routeToOptimize);

		lowBoundCost = calculateLowerBoundCost(cloneRoute(routeToOptimize));
		upperBoundCost = calculateUpperBoundCost(createCycle(cloneRoute(routeToOptimize)));

		final Set<CityPair> optimizedRoute = new HashSet<>();
		while(!routeToOptimize.isEmpty())
		{
			logger.debug("Optimizing: {}", dumpCityPairs(routeToOptimize));
			cleanEstimatedCosts(routeToOptimize);
			optimizedRoute.add(applyBranchesAndBoundariesAlgorithm(routeToOptimize));
		}
		return getFinalRoute(optimizedRoute, startCity);
	}

	private Set<CityPair> cloneRoute(final Set<CityPair> seedRoute)
	{
		return seedRoute.stream().map(this::cloneCityPair).collect(Collectors.toSet());
	}

	private CityPair cloneCityPair(final CityPair cityPair)
	{
		try
		{
			return cityPair.clone();
		}
		catch (final CloneNotSupportedException e)
		{
			throw new InconsistentRouteException("CloneNotSupportedException caught: " + e.getMessage());
		}
	}

	protected List<City> getFinalRoute(final Set<CityPair> route, final City startCity)
	{
	 	final Optional<CityPair> startCityPair = route.stream().filter(p -> p.getCity1().equals(startCity)).findFirst();
		final List<City> finalRoute = new ArrayList<>(); 
		if(startCityPair.isPresent())
		{
			final CityPair cityPair = startCityPair.get();
			route.remove(cityPair);
			finalRoute.add(cityPair.getCity1());
			final List<City> routeTail = getFinalRoute(route, cityPair.getCity2());
			if(!routeTail.isEmpty())
			{
				finalRoute.addAll(routeTail);
			}
		}	
		return finalRoute;
	}

	protected CityPair applyBranchesAndBoundariesAlgorithm(final Set<CityPair> seedRoute)
	{
		final Set<City> cities = getAllCitiesOnTheRoute(seedRoute);
		final Map<City, CityPair> minimizedColumn = getMinimizedColumn(cities, seedRoute);
		reduceRows(seedRoute, minimizedColumn);
		final Map<City, CityPair> minimizedRow = getMinimizedRow(cities, seedRoute);
		reduceColumns(seedRoute, minimizedRow);

		Long lowBoundaryConstant = minimizedColumn.values().stream().reduce(0L, (c, p) -> c + p.getCost(), Long::sum);
		lowBoundaryConstant += minimizedRow.values().stream().reduce(0L, (c, p) -> c + p.getCost(), Long::sum);

		calculateEstimatedCostZeroCell(seedRoute);
		return reduceRoutingMatrix(seedRoute);
	}

	private List<CityPair> createCycle(final Set<CityPair> seedRoute)
	{
		final List<City> sortedCities = getAllCitiesOnTheRoute(seedRoute).stream().sorted(new City.CityComparator()).collect(toList());
		final List<CityPair> sortedRoute = new ArrayList<>();
		for(int i = 0; i < sortedCities.size(); i++)
		{
			final City city1 = sortedCities.get(i);
			final City city2;
			if(i < sortedCities.size() - 1)
			{
				city2 = sortedCities.get(i+1);
			}
			else
			{
				city2 = sortedCities.get(0);
			}
			sortedRoute.add(seedRoute.stream().filter(p -> p.getCity1().equals(city1) && p.getCity2().equals(city2)).findFirst().get());
		}
		return sortedRoute;
	}

	private Long calculateLowerBoundCost(final Set<CityPair> seedRoute)
	{
		final Set<City> cities = getAllCitiesOnTheRoute(seedRoute);
		final Map<City, CityPair> minimizedColumn = getMinimizedColumn(cities, seedRoute);
		reduceRows(seedRoute, minimizedColumn);
		final Map<City, CityPair> minimizedRow = getMinimizedRow(cities, seedRoute);

		Long lowBoundaryConstant = minimizedColumn.values().stream().reduce(0L, (c, p) -> c + p.getCost(), Long::sum);
		lowBoundaryConstant += minimizedRow.values().stream().reduce(0L, (c, p) -> c + p.getCost(), Long::sum);

		return  lowBoundaryConstant;
	}

	private Long calculateUpperBoundCost(final List<CityPair> seedRoute)
	{
		return seedRoute.stream().reduce(0L, (c,p) -> c + p.getCost(), Long::sum);
	}

	private void cleanEstimatedCosts(final Set<CityPair> seedRoute)
	{
		seedRoute.stream().forEach(p -> p.setEstimatedZeroCellCost(null));		
	}

	protected Set<CityPair> normalizeRoute(final Set<CityPair> seedRoute, final OptimizeBy optimizeBy)
	{
		logger.debug("Before route matrix normalization: {}", dumpCityPairs(seedRoute));
		if(OptimizeBy.DISTANCE_SYMMETRICAL.equals(optimizeBy)) {
			symmetrizeRouteMatrix(seedRoute);
		}
		final Set<CityPair> normalizedRoute = seedRoute.stream().filter(p -> !p.isDiagonal()).collect(toSet());
		logger.debug("After matrix normalization: {}", dumpCityPairs(normalizedRoute));
		return normalizedRoute;
	}
	
	protected void symmetrizeRouteMatrix(final Set<CityPair> seedRoute)
	{
		final List<CityPair> orderedCityPairs = seedRoute.stream().sorted(new CityPair.CityPairComparator()).collect(toList());
		final Set<CityPair> processedEntries = new HashSet<>(); 
		for(final CityPair cityPair: orderedCityPairs)
		{
			if(!processedEntries.contains(cityPair))
			{
				final CityPair probeCityPair = new CityPair(cityPair.getCity2(), cityPair.getCity1(), null);
				probeCityPair.setCost(cityPair.getCost());
				seedRoute.remove(probeCityPair);
				seedRoute.add(probeCityPair);
				
				processedEntries.add(cityPair);
				processedEntries.add(probeCityPair);
			}
		}		
	}
	
	// step1: find minimum in every row
	protected Map<City, CityPair> getMinimizedColumn(Set<City> cityRow, final Set<CityPair> seedRoute)
	{
		return cityRow.stream().map(c -> getMinimizedColumnEntry(c, seedRoute)).filter(Optional::isPresent).collect(toMap(c -> c.get().getCity1(), Optional::get));
	}
	
	// step2: reduce each row subtracting from each row the optimized value
	protected void reduceRows(final Set<CityPair> seedRoute, final Map<City, CityPair> minimizedColumn)
	{
		minimizedColumn.values().stream().forEach(p -> reduceRow(seedRoute, p));
	}
	
	// step3: find minimum for every column
	protected Map<City, CityPair> getMinimizedRow(Set<City> cityRow, final Set<CityPair> seedRoute)
	{
		return cityRow.stream().map(c -> getMinimizedRowEntry(c, seedRoute)).filter(Optional::isPresent).collect(toMap(c -> c.get().getCity2(), Optional::get));
	}

	// step4: reduce each column subtracting from each column the optimized value
	protected void reduceColumns(final Set<CityPair> seedRoute, final Map<City, CityPair> minimizedRow)
	{
		minimizedRow.values().stream().forEach(p -> reduceColumn(seedRoute, p));
	}

	// step5: calculate estimated cost of zero cells: is equal to min(row)+min(column) for a given zero cell
	protected void calculateEstimatedCostZeroCell(final Set<CityPair> seedRoute)
	{
		final Set<CityPair> zeroCells = seedRoute.stream().filter(p -> p.getCost().equals(0l)).collect(toSet());
		zeroCells.stream().forEach(p -> setEstimatedCost(p, seedRoute));
	}
	
	private void setEstimatedCost(final CityPair cityPair, final Set<CityPair> seedRoute)
	{
		final Optional<CityPair> minimizedColumnEntry = getMinimizedColumnEntry(cityPair.getCity1(), seedRoute, cityPair.getCity2());
		final Optional<CityPair> minimizedRowEntry = getMinimizedRowEntry(cityPair.getCity2(), seedRoute, cityPair.getCity1());
		if(minimizedColumnEntry.isPresent() && minimizedRowEntry.isPresent())
		{
			cityPair.setEstimatedZeroCellCost(minimizedColumnEntry.get().getCost() + minimizedRowEntry.get().getCost());
		}		
	}
	
	// step6: reduce routing matrix, selecting the zero cell with maximum estimated cost and setting it to INFINITY
	protected CityPair reduceRoutingMatrix(final Set<CityPair> seedRoute)
	{
		final Optional<CityPair> maxEstimatedCostCell = seedRoute.stream().filter(p -> p.getCost().equals(0l))
				.max(new EstimatedCostComparator());
		CityPair edgeNode = maxEstimatedCostCell.get();
		if(maxEstimatedCostCell.isPresent())
		{
			final CityPair cityPair = edgeNode;
			cityPair.setCost(null);
			cityPair.setEstimatedZeroCellCost(null);
			final Optional<CityPair> returnRoute = getReturnRoute(cityPair, seedRoute);
			cityPair.setRouteOptimized(true);
			if(returnRoute.isPresent())
			{
				seedRoute.remove(returnRoute.get());
			}
			seedRoute.removeAll(seedRoute.stream().filter(p -> p.getCity1().equals(cityPair.getCity1()) || p.getCity2().equals(cityPair.getCity2())).collect(toSet()));
		}
		logger.debug("Route fragment: {} -> {}", edgeNode.getCity1().getName(), edgeNode.getCity2().getName());
		seedRoute.stream().forEach(c -> logger.debug("{} -> {}", c.getCity1().getName(), c.getCity2().getName()));
		return edgeNode;
	}
	
	private Optional<CityPair> getReturnRoute(final CityPair cityPair, final Set<CityPair> seedRoute)
	{
		return seedRoute.stream().filter(p -> cityPair.getCity1().equals(p.getCity2()) && cityPair.getCity2().equals(p.getCity1())).findFirst();				
	}
	
	protected void reduceRow(final Set<CityPair> seedRoute, final CityPair minimizedRowEntry)
	{
		seedRoute.stream().filter(p -> p.getCity1().equals(minimizedRowEntry.getCity1())).forEach(p -> p.setCost(p.getCost() - minimizedRowEntry.getCost()));
	}

	protected void reduceColumn(final Set<CityPair> seedRoute, final CityPair minimizedColumnEntry)
	{
		seedRoute.stream().filter(p -> p.getCity2().equals(minimizedColumnEntry.getCity2())).forEach(p -> p.setCost(p.getCost() - minimizedColumnEntry.getCost()));
	}
	
	protected Optional<CityPair> getMinimizedColumnEntry(final City cityRow, final Set<CityPair> seedRoute)
	{
		return getMinimizedColumnEntry(cityRow, seedRoute, null);
	}

	protected Optional<CityPair> getMinimizedColumnEntry(final City cityRow, final Set<CityPair> seedRoute, final City cityColumnToExclude)
	{
		final List<CityPair> routeRow; 
		if(nonNull(cityColumnToExclude))
		{
			routeRow = seedRoute.stream().filter(p -> p.getCity1().equals(cityRow) && !p.getCity2().equals(cityColumnToExclude)).collect(toList());
		}
		else
		{
			routeRow = seedRoute.stream().filter(p -> p.getCity1().equals(cityRow)).collect(toList());
		}
		final Optional<CityPair> minimizedEntry = routeRow.stream().min((s1, s2) -> s1.getCost().compareTo(s2.getCost()));
		if(minimizedEntry.isPresent())
		{
			return Optional.of(new CityPair(minimizedEntry.get().getCity1(), minimizedEntry.get().getCity2(), minimizedEntry.get().getCost()));			
		}
		return Optional.empty();
	}

	protected Optional<CityPair> getMinimizedRowEntry(final City city, final Set<CityPair> seedRoute)
	{
		return getMinimizedRowEntry(city, seedRoute, null);
	}
	
	protected Optional<CityPair> getMinimizedRowEntry(final City cityColumn, final Set<CityPair> seedRoute, final City cityRowToExclude)
	{
		final List<CityPair> routeColumn;
		if(nonNull(cityRowToExclude))
		{			
			routeColumn = seedRoute.stream().filter(p -> p.getCity2().equals(cityColumn) && !p.getCity1().equals(cityRowToExclude)).collect(toList());
		}
		else 
		{
			routeColumn = seedRoute.stream().filter(p -> p.getCity2().equals(cityColumn)).collect(toList());
		}
		final Optional<CityPair> minimizedEntry = routeColumn.stream().min((s1, s2) -> s1.getCost().compareTo(s2.getCost()));
		if(minimizedEntry.isPresent())
		{
			return Optional.of(new CityPair(minimizedEntry.get().getCity1(), minimizedEntry.get().getCity2(), minimizedEntry.get().getCost()));
		}
		return Optional.empty();
	}
	
	protected Set<City> getAllCitiesOnTheRoute(final Set<CityPair> seedRoute)
	{
		return  seedRoute.stream().flatMap(p -> Stream.of(p.getCity1(), p.getCity2())).distinct().collect(toSet());
	}

	private String dumpCityPairs(final Set<CityPair> cityPairs)
	{
		final StringBuilder dumpBuilder = new StringBuilder("Matrix dump:");
		cityPairs.stream().forEach(p -> dumpBuilder.append("(" + p.getCity1().getId() + ", " + p.getCity2().getId() + ") = " + p.getCost()+ " (" + p.getEstimatedZeroCellCost() + ")   "));
		dumpBuilder.append("\n\n");
		return dumpBuilder.toString();
	}
}
