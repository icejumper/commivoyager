package org.ark.math.commivoyager.repository;

import org.ark.math.commivoyager.model.CityPair;

import java.util.Set;

/**
 * Created by arkadys on 4/11/16.
 */
public interface RouteReader {

    Set<CityPair> readMatrix();

}
