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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.ark.math.commivoyager.algorithm.BranchAndBoundStrategy.OptimizeBy.DISTANCE;
import static org.ark.math.commivoyager.algorithm.BranchAndBoundStrategy.OptimizeBy.DISTANCE_SYMMETRICAL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.ark.math.commivoyager.repository.CostRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;

@RunWith(MockitoJUnitRunner.class)
public class BranchAndBoundStrategyUnitTest
{
	@InjectMocks
	private BranchAndBoundStrategy branchAndBoundStrategy;
	@Mock
	private CostRepository costRepository;
	
	private Set<City> cities = ImmutableSet.of(new City(1, "city1"), new City(2, "city2"), new City(3, "city3"), new City(4, "city4"));
	private Set<CityPair> cityPairs;
	
	@Before
	public void setUp()
	{
		cityPairs = getPermutations(cities);
	}
	
	@Test
	public void testGetAllCitiesOnTheRoute()
	{
		final Set<City> allCities = branchAndBoundStrategy.getAllCitiesOnTheRoute(cityPairs);
		assertThat(allCities).isNotEmpty().hasSize(cities.size()).containsAll(cities);
	}

	@Test
	public void testGetMinimizedColumnEntry()
	{
		final City city = new City(1, "city1");
		final Optional<CityPair> optimizedColumnEntry = branchAndBoundStrategy.getMinimizedColumnEntry(city, cityPairs);
		assertThat(optimizedColumnEntry.isPresent()).isTrue();
		assertThat(optimizedColumnEntry.get().getCity1()).isEqualTo(city);
		assertThat(optimizedColumnEntry.get().getCost()).isEqualTo(1l);
	}

	@Test
	public void testGetMinimizedColumn()
	{
		final Map<City, CityPair> optimizedColumn = branchAndBoundStrategy.getMinimizedColumn(cities, cityPairs);
		assertThat(optimizedColumn).isNotNull().hasSize(cities.size());
		final List<Long> optimizedCosts = optimizedColumn.values().stream().map(CityPair::getCost).collect(toList());
		assertThat(optimizedCosts).containsOnly(1l, 4l, 7l, 10l);
	}

	@Test
	public void testGetMinimizedRowEntry()
	{
		final City city = new City(4, "");
		final Optional<CityPair> optimizedColumnEntry = branchAndBoundStrategy.getMinimizedRowEntry(city, cityPairs);
		assertThat(optimizedColumnEntry.isPresent()).isTrue();
		assertThat(optimizedColumnEntry.get().getCity1()).isEqualTo(new City(1, ""));
		assertThat(optimizedColumnEntry.get().getCost()).isEqualTo(3l);
	}

	@Test
	public void testGetMinimizedRow()
	{
		final Map<City, CityPair> optimizedRow = branchAndBoundStrategy.getMinimizedRow(cities, cityPairs);
		assertThat(optimizedRow).isNotNull().hasSize(cities.size());
		final List<Long> optimizedCosts = optimizedRow.values().stream().map(CityPair::getCost).collect(toList());
		assertThat(optimizedCosts).containsOnly(1l, 2l, 3l, 4l);
	}
	
	@Test
	public void testNormalizeRoute()
	{
		cityPairs = getPermutationsWithDiagonal(cities);
		
		final Set<CityPair> normalizedCityPairs = branchAndBoundStrategy.normalizeRoute(cityPairs, DISTANCE_SYMMETRICAL);
		final Set<CityPair> diagonalElements = normalizedCityPairs.stream().filter(p -> p.getCity1().equals(p.getCity2())).collect(toSet());
		assertThat(diagonalElements).isEmpty();
	}

	@Test
	public void testReduceRows()
	{
		cityPairs = branchAndBoundStrategy.normalizeRoute(cityPairs, DISTANCE);
		final Map<City, CityPair> minimizedColumn = branchAndBoundStrategy.getMinimizedColumn(cities, cityPairs);
		branchAndBoundStrategy.reduceRows(cityPairs, minimizedColumn);
		final List<Long> optimizedCosts = cityPairs.stream().filter(p -> p.getCity1().equals(new City(1,""))).map(CityPair::getCost).collect(toList());
		assertThat(optimizedCosts).contains(0l, 1l, 2l);
		final List<Long> optimizedCosts2 = cityPairs.stream().filter(p -> p.getCity1().equals(new City(2,""))).map(CityPair::getCost).collect(toList());
		assertThat(optimizedCosts2).contains(0l, 1l, 2l);
	}

	@Test
	public void testReduceColumns()
	{
		cityPairs = branchAndBoundStrategy.normalizeRoute(cityPairs, DISTANCE_SYMMETRICAL);
		final Map<City, CityPair> minimizedRow = branchAndBoundStrategy.getMinimizedRow(cities, cityPairs);
		branchAndBoundStrategy.reduceColumns(cityPairs, minimizedRow);
		final List<Long> optimizedCosts = cityPairs.stream().filter(p -> p.getCity2().equals(new City(1,""))).map(CityPair::getCost).collect(toList());
		assertThat(optimizedCosts).contains(2l, 1l, 0l);
		final List<Long> optimizedCosts2 = cityPairs.stream().filter(p -> p.getCity2().equals(new City(2,""))).map(CityPair::getCost).collect(toList());
		assertThat(optimizedCosts2).contains(0l, 5l, 4l);
	}
	
	@Test
	public void testCalculateEstimatedCostZeroCell()
	{
		cityPairs = branchAndBoundStrategy.normalizeRoute(cityPairs, DISTANCE_SYMMETRICAL);
		final Map<City, CityPair> minimizedRow = branchAndBoundStrategy.getMinimizedRow(cities, cityPairs);
		branchAndBoundStrategy.reduceColumns(cityPairs, minimizedRow);
		branchAndBoundStrategy.calculateEstimatedCostZeroCell(cityPairs);

		final List<Long> estimatedCosts = cityPairs.stream().filter(p -> p.getCity1().equals(new City(1,""))).map(CityPair::getEstimatedZeroCellCost).collect(toList());
		assertThat(estimatedCosts).contains(4l, 3l, 3l);
		final List<Long> estimatedCosts1 = cityPairs.stream().filter(p -> p.getCity1().equals(new City(2,""))).map(CityPair::getEstimatedZeroCellCost).collect(toList());
		assertThat(estimatedCosts1).contains(4l);
	}

	@Test
	public void testApplyBranchesAndBoundariesAlgorithm()
	{
		final CityPair cityPair = branchAndBoundStrategy.applyBranchesAndBoundariesAlgorithm(cityPairs, Collections.emptySet());
		assertThat(cityPair).isNotNull();
		assertThat(cityPairs).hasSize(6);
	}

	@Test
	public void testOptimize()
	{
		cityPairs = getRandomPermutations(cities);

		final List<City> route = branchAndBoundStrategy.optimize(cityPairs, new City(1, ""), DISTANCE_SYMMETRICAL);
		assertThat(route).isNotEmpty().hasSize(4);
		dumpRoute(route);
	}
	
	private Set<CityPair> getPermutations(final Set<City> cities)
	{
		final Set<CityPair> cityPairs = new HashSet<>();
		Long cost = 1l;
		for (final City city1: cities)
		{
			for (final City city2: cities)
			{
				if(!city1.equals(city2))
				{
					final CityPair cityPair = new CityPair(city1, city2, cost);
					cost += 1l;
					cityPairs.add(cityPair);
				}				
			}
		}
		
		return cityPairs;
	}

	private Set<CityPair> getPermutationsWithDiagonal(final Set<City> cities)
	{
		final Set<CityPair> cityPairs = new HashSet<>();
		Long cost = 1l;
		for (final City city1: cities)
		{
			for (final City city2: cities)
			{
				final CityPair cityPair = new CityPair(city1, city2, cost);
				cost += 1l;
				cityPairs.add(cityPair);
			}
		}

		return cityPairs;
	}

	private Set<CityPair> getRandomPermutations(final Set<City> cities)
	{
		final Random rnd = new Random(100);
		final Set<CityPair> cityPairs = new HashSet<>();
		for (final City city1: cities)
		{
			for (final City city2: cities)
			{
				if(!city1.equals(city2))
				{
					final Long cost = (long)(rnd.nextDouble() * 10000);
					final CityPair cityPair = new CityPair(city1, city2, cost);
					cityPairs.add(cityPair);
				}
			}
		}

		return cityPairs;
	}
	
	private void dumpCityPairs()
	{
		cityPairs.stream().forEach(p -> System.out.print("(" + p.getCity1().getId() + ", " + p.getCity2().getId() + ") = " + p.getCost()+ "   "));
	}
	
	private void dumpRoute(final List<City> route)
	{
		route.stream().forEach(c -> System.out.print(c.getName() + " -> "));		
	}
	
}
