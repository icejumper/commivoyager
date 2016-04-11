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

import java.util.HashMap;
import java.util.Map;

import org.ark.math.commivoyager.model.CityPair;
import org.ark.math.commivoyager.repository.CostRepository;

import com.google.common.base.Preconditions;

public class InMemoryDistanceRepository implements CostRepository
{

	private final Map<CityPair, Double> costMatrix;
	
	public InMemoryDistanceRepository()
	{
		costMatrix = new HashMap<>();		
	}
	
	@Override
	public Double getCostBetweeen(final CityPair cityPair)
	{
		return costMatrix.get(cityPair);
	}

	@Override
	public void saveCostBetween(final CityPair cityPair, final Double cost)
	{
		Preconditions.checkNotNull(cityPair, "City pair should be provided");
		Preconditions.checkNotNull(cost, "Cost must bbe provided");

		costMatrix.put(cityPair, cost);
	}
}
