package org.ark.math.commivoyager.repository;

import org.ark.math.commivoyager.model.City;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by arkadys on 4/11/16.
 */
public class CityFactory {

    private Map<String, City> cityCache = new java.util.HashMap<>();
    private AtomicInteger cityId = new AtomicInteger();

    public City createCity(final String name)
    {
        final City city;
        if(!cityCache.containsKey(name)) {
            city = new City(cityId.getAndIncrement(), name);
            cityCache.put(name, city);
        }
        else
        {
            city = cityCache.get(name);
        }
        return city;
    }

}
