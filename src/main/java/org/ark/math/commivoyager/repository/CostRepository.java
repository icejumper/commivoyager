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
package org.ark.math.commivoyager.repository;

import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;

public interface CostRepository
{
	
	Double getCostBetweeen(CityPair cityPair);
	
	void saveCostBetween(CityPair cityPair, Double cost);

	
}
