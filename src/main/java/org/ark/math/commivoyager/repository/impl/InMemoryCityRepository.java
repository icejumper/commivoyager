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
package org.ark.math.commivoyager.repository.impl;

import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;

import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.repository.CityRepository;

import com.google.common.base.Preconditions;

public class InMemoryCityRepository implements CityRepository
{
	private final Map<Integer, City> cityMap;

	public InMemoryCityRepository()
	{
		this.cityMap = new HashMap<>();
	}

	@Override
	public City getCity(final Integer id)
	{
		Preconditions.checkNotNull(id, "Id of the city must be provided");
		
		return cityMap.get(id);
	}

	@Override
	public void saveCity(final City city)
	{
		Preconditions.checkArgument(nonNull(city) && nonNull(city.getId()), "City with valid id [integer] must be provided");
		Preconditions.checkArgument(nonNull(city.getName()), "Name of the city must be provided");
		
		cityMap.put(city.getId(), city);
	}
}
