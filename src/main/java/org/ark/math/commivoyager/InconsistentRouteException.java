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
package org.ark.math.commivoyager;

public class InconsistentRouteException extends RuntimeException
{
	public InconsistentRouteException(final String message)
	{
		super(message);
	}

	public InconsistentRouteException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
