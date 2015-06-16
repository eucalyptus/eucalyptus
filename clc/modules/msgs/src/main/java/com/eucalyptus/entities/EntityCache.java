/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.util.Pair;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 *
 */
public class EntityCache<E extends AbstractPersistent, TE extends Comparable<TE>> implements Supplier<Iterable<TE>> {

  private static final int batchSize = 500;
  private final E example;
  private final Criterion criterion;
  private final Set<String> eagerAssociationPaths;
  private final Set<String> lazyAssociationPaths;
  private final Function<? super E,TE> transformFunction;
  private final ConcurrentMap<Pair<String,Integer>,TE> cache = Maps.newConcurrentMap( );

  /**
   * Create an entity cache for the given example.
   *
   * @param example The example object
   * @param transformFunction Function to transform to immutable cache format
   */
  public EntityCache( final E example,
                      final Function<? super E,TE> transformFunction ) {
    this(
        example,
        Restrictions.conjunction( ),
        Collections.<String>emptySet( ),
        Collections.<String>emptySet( ),
        transformFunction );
  }

  /**
   * Create an entity cache for the given example.
   *
   * @param example The example object
   * @param criterion Additional criterion
   * @param eagerAssociationPaths Paths to be eagerly loaded
   * @param lazyAssociationPaths Paths to be lazily (or not) loaded
   * @param transformFunction Function to transform to immutable cache format
   */
  public EntityCache( final E example,
                      final Criterion criterion,
                      final Set<String> eagerAssociationPaths,
                      final Set<String> lazyAssociationPaths,
                      final Function<? super E,TE> transformFunction ) {
    this.example = example;
    this.criterion = criterion;
    this.eagerAssociationPaths = eagerAssociationPaths;
    this.lazyAssociationPaths = lazyAssociationPaths;
    this.transformFunction = transformFunction;
  }

  @SuppressWarnings( "unchecked" )
  private Collection<Pair<String,Integer>> loadVersionMap( ) {
    try ( final TransactionResource db = Entities.readOnlyDistinctTransactionFor( example ) ){
      final Criteria criteria = Entities.createCriteria( example.getClass( ) )
          .add( Example.create( example ) )
          .add( criterion )
          .setProjection( Projections.projectionList( )
              .add( Projections.property( "id" ) )
              .add( Projections.property( "version" ) ) );
      final List<Object[]> idVersionList = (List<Object[]>) criteria.list( );
      final Set<Pair<String,Integer>> results = Sets.newLinkedHashSetWithExpectedSize( idVersionList.size( ) );
      Iterables.addAll( results, Iterables.transform( idVersionList, ObjectArrayToStringIntPair.INSTANCE ) );
      return results;
    }
  }

  @SuppressWarnings( { "unchecked", "ConstantConditions" } )
  private void refresh( ) {
    final Collection<Pair<String,Integer>> currentKeys = loadVersionMap( );
    cache.keySet( ).retainAll( currentKeys );
    currentKeys.removeAll( cache.keySet( ) );
    for ( final List<Pair<String,Integer>> keyBatch : Iterables.partition( currentKeys, batchSize ) ) {
      try ( final TransactionResource db = Entities.readOnlyDistinctTransactionFor( example ) ) {
        final Criteria criteria =  Entities.createCriteria( example.getClass( ) )
            .add( Example.create( example ) )
            .add( criterion )
            .setFetchSize( batchSize )
            .add( Restrictions.in( "id", Lists.newArrayList( Iterables.transform( keyBatch, Pair.<String, Integer>left( ) ) ) ) );
        for ( final String path : eagerAssociationPaths ) criteria.setFetchMode( path, FetchMode.JOIN );
        for ( final String path : lazyAssociationPaths ) criteria.setFetchMode( path, FetchMode.SELECT );
        final List<E> entities = (List<E> ) criteria.list( );
        for ( final E entity : entities ) {
          cache.put( Pair.pair( getId( entity ), entity.getVersion( ) ), transformFunction.apply( entity ) );
        }
      }
    }
  }

  @Override
  public Iterable<TE> get( ) {
    refresh( );
    return Ordering.natural( ).sortedCopy( cache.values( ) );
  }

  private String getId( final E entity ) {
    return Objects.toString( Entities.resolvePrimaryKey( entity ) );
  }

  private enum ObjectArrayToStringIntPair implements Function<Object[],Pair<String,Integer>> {
    INSTANCE;

    @Override
    public Pair<String, Integer> apply( final Object[] objects ) {
      return Pair.pair( String.valueOf( objects[ 0 ] ), ( (Number) objects[ 1 ] ).intValue() );
    }
  }
}
