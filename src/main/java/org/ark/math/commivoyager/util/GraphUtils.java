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
package org.ark.math.commivoyager.util;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.ark.math.commivoyager.InconsistentRouteException;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class GraphUtils
{
	private static final Logger logger = LoggerFactory.getLogger(GraphUtils.class);
	
	public Set<CityPair> cloneRoute(final Set<CityPair> seedRoute)
	{
		return seedRoute.stream().map(this::cloneCityPair).collect(toSet());
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

	public List<CityPair> getFinalRoute(final Set<CityPair> route, final City startCity)
	{
		final Optional<CityPair> startCityPair = route.stream().filter(p -> p.getCity1().equals(startCity)).findFirst();
		final List<CityPair> finalRoute = new ArrayList<>();
		if(startCityPair.isPresent())
		{
			final CityPair cityPair = startCityPair.get();
			route.remove(cityPair);
			finalRoute.add(cityPair);
			finalRoute.addAll(getFinalRoute(route, cityPair.getCity2()));
		}
		return finalRoute;
	}

	public void dumpRoute(final List<CityPair> route)
	{
		Preconditions.checkArgument(CollectionUtils.isNotEmpty(route), "Route is empty");
		
		final City startCity = route.get(0).getCity1();
		final String routeDraw = route.stream().reduce("", (s, a) -> s + a.getCity1().getName() + " -> ", (s1, s2) -> s1 + "->" +s2);
		logger.info("Final route: {}{}", routeDraw, startCity.getName());
		final long totalDistance = route.stream().reduce(0l, (d, a) -> d + a.getInitialCost(), (d1, d2) -> d1 + d2);
		logger.info("Total cost: {}", totalDistance);
	}

	public String dumpCityPairs(final Set<CityPair> cityPairs)
	{
		final StringBuilder dumpBuilder = new StringBuilder("Matrix dump:");
		cityPairs.stream().forEach(p -> dumpBuilder.append("(" + p.getCity1().getId() + ", " + p.getCity2().getId() + ") = " + p.getCost()+ " (" + p.getEstimatedZeroCellCost() + ")   "));
		dumpBuilder.append("\n\n");
		return dumpBuilder.toString();
	}
}
