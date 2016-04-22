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

public class Test
{
	
	private String code;
	private Long version;

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Test))
		{
			return false;
		}

		final Test test = (Test) o;

		if (!code.equals(test.code))
		{
			return false;
		}
		if (!version.equals(test.version))
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = code.hashCode();
		result = 31 * result + version.hashCode();
		return result;
	}
}
