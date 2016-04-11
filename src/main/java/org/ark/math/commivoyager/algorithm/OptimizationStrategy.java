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

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.ark.math.commivoyager.model.CityPair.EstimatedCostComparator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.ark.math.commivoyager.repository.CostRepository;
import org.springframework.beans.factory.annotation.Required;

import com.google.common.base.Preconditions;

public class OptimizationStrategy
{
	private CostRepository costRepository;
	
	public enum OptimizeBy 
	{
		DISTANCE,
		DISTANCE_SYMMETRICAL
	}

	public List<City> optimize(final Set<CityPair> seedRoute, final City startCity, final OptimizeBy optimizeBy)
	{
		Preconditions.checkArgument(CollectionUtils.isNotEmpty(seedRoute), "The source route (CityPair collection) must not be null or empty");
				
		final Set<CityPair> normalizedRoute = normalizeRoute(seedRoute, optimizeBy);
		Set<CityPair> routeToOptimize = normalizedRoute.stream().filter(p -> !p.isRouteOptimized()).collect(toSet());
		final Set<CityPair> optimizedRoute = new HashSet<>();
		while(!routeToOptimize.isEmpty())
		{
			cleanEstimatedCosts(routeToOptimize);
			optimizedRoute.add(applyBranchesAndBoundariesAlgorithm(routeToOptimize));
		}
		return getFinalRoute(optimizedRoute, startCity);
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
		calculateEstimatedCostZeroCell(seedRoute);
		return reduceRoutingMatrix(seedRoute);
	}
	
	private void cleanEstimatedCosts(final Set<CityPair> seedRoute)
	{
		seedRoute.stream().forEach(p -> p.setEstimatedZeroCellCost(null));		
	}

	protected Set<CityPair> normalizeRoute(final Set<CityPair> seedRoute, final OptimizeBy optimizeBy)
	{
		System.out.println("Before normalization:");
		dumpCityPairs(seedRoute);
		if(OptimizeBy.DISTANCE_SYMMETRICAL.equals(optimizeBy))
		{
			symmetrizeRouteMatrix(seedRoute);
		}
		System.out.println("After normalization:");
		dumpCityPairs(seedRoute);
		return seedRoute.stream().filter(p -> !p.isDiagonal()).collect(toSet());
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
		if(maxEstimatedCostCell.isPresent())
		{
			final CityPair cityPair = maxEstimatedCostCell.get();
			cityPair.setCost(null);
			cityPair.setEstimatedZeroCellCost(null);
			final Optional<CityPair> returnRoute = getReturnRoute(cityPair, seedRoute);
			cityPair.setRouteOptimized(true);
			if(returnRoute.isPresent())
			{
				seedRoute.remove(returnRoute.get());
			}
			dumpCityPairs(seedRoute);
			seedRoute.removeAll(seedRoute.stream().filter(p -> p.getCity1().equals(cityPair.getCity1()) || p.getCity2().equals(cityPair.getCity2())).collect(toSet()));
		}
		dumpCityPairs(seedRoute);
		return maxEstimatedCostCell.get();
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

	@Required
	public void setCostRepository(final CostRepository costRepository)
	{
		this.costRepository = costRepository;
	}


	private void dumpCityPairs(final Set<CityPair> cityPairs)
	{
		System.out.println("Matrix dump:");
		cityPairs.stream().forEach(p -> System.out.print("(" + p.getCity1().getId() + ", " + p.getCity2().getId() + ") = " + p.getCost()+ " (" + p.getEstimatedZeroCellCost() + ")   "));
		System.out.println("\n\n");
	}
}
