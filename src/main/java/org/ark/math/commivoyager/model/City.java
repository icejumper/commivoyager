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

import java.util.Comparator;

public class City
{
	private Integer id;
	private String name;

	public City(final Integer id, final String name)
	{
		this.id = id;
		this.name = name;
	}

	public Integer getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof City))
		{
			return false;
		}

		final City city = (City) o;

		if (!id.equals(city.id))
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	public static class CityComparator implements Comparator<City>
	{
		@Override
		public int compare(final City city1, final City city2)
		{
			return city1.getId().compareTo(city2.getId());
		}
	}
}
