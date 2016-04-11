package org.ark.math.commivoyager.repository.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.ark.math.commivoyager.model.City;
import org.ark.math.commivoyager.model.CityPair;
import org.ark.math.commivoyager.repository.CityFactory;
import org.ark.math.commivoyager.repository.RouteReader;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

/**
 * Created by arkadys on 4/11/16.
 */
public class CsvRouteReader implements RouteReader {

    private CSVParser csvParser;

    @Autowired
    private CityFactory cityFactory;

    public CsvRouteReader(final String filePath) throws IOException
    {
        final Reader reader = new FileReader(filePath);
        csvParser = new CSVParser(reader, CSVFormat.EXCEL);
    }

    @Override
    public Set<CityPair> readMatrix() {

        final Set<CityPair> route = new HashSet<>();
        List<City> cities = null;
        int i = 0;
        int n = 0;
        for(final CSVRecord record: csvParser)
        {
            if(i == 0)
            {
                cities = stream(record.spliterator(), false).map(cityFactory::createCity).collect(toList());
                n = cities.size();
            }
            else
            {
                final City city = cityFactory.createCity(record.get(0));
                for(int j = 1; j < n; j++)
                {
                    if(StringUtils.isNoneEmpty(record.get(j))) {
                        Long cost = Long.valueOf(record.get(j));
                        route.add(new CityPair(city, cities.get(j), cost));
                    }
                }
            }
            i++;
        }
        return route;
    }
}
