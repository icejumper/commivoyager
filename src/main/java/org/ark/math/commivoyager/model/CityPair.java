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
package org.ark.math.commivoyager.model;

import static java.util.Objects.isNull;

import java.util.Comparator;
import java.util.Objects;

public class CityPair
{
	private City city1;
	private City city2;
	
	private Long cost;
	private Long estimatedZeroCellCost;
	private boolean routeOptimized; 

	public CityPair(final City city1, final City city2, final Long cost)
	{
		this.city1 = city1;
		this.city2 = city2;
		this.cost = cost;
	}

	public City getCity1()
	{
		return city1;
	}

	public City getCity2()
	{
		return city2;
	}

	public Long getCost()
	{
		return cost;
	}

	public void setCost(final Long cost)
	{
		this.cost = cost;
	}

	public Long getEstimatedZeroCellCost()
	{
		return estimatedZeroCellCost;
	}

	public void setEstimatedZeroCellCost(final Long estimatedZeroCellCost)
	{
		this.estimatedZeroCellCost = estimatedZeroCellCost;
	}

	public void setRouteOptimized(final boolean routeOptimized)
	{
		this.routeOptimized = routeOptimized;
	}

	public boolean isRouteOptimized()
	{
		return routeOptimized;
	}

	public boolean isDiagonal()
	{
		if(isNull(city1) || isNull(city1))
		{
			return false;
		}
		
		return city1.equals(city2);		
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof CityPair))
		{
			return false;
		}

		final CityPair cityPair = (CityPair) o;

		if (!city1.equals(cityPair.city1))
		{
			return false;
		}
		if (!city2.equals(cityPair.city2))
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = city1.hashCode();
		result = 31 * result + city2.hashCode();
		return result;
	}
	
	public static class EstimatedCostComparator implements Comparator<CityPair>
	{
		@Override
		public int compare(final CityPair cityPair1, final CityPair cityPair2)
		{
			if(isNull(cityPair1) || isNull(cityPair2))
				return 0;
			
			return cityPair1.getCost().compareTo(cityPair2.getCost());
		}
	}

	public static class CityPairComparator implements Comparator<CityPair>
	{
		@Override
		public int compare(final CityPair cityPair1, final CityPair cityPair2)
		{
			if(isNull(cityPair1) || isNull(cityPair2))
				return 0;
			if(cityPair1.getCity1().getId().equals(cityPair2.getCity1().getId()))
				return cityPair1.getCity2().getId().compareTo(cityPair2.getCity2().getId());

			return cityPair1.getCity1().getId().compareTo(cityPair2.getCity1().getId());
		}
	}
}
