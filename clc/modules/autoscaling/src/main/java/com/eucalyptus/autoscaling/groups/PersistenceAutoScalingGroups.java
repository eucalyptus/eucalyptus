/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling.groups;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.HealthStatus;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistentsWithResourceNameSupport;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 *
 */
public class PersistenceAutoScalingGroups extends AutoScalingGroups {
  
  private PersistenceSupport persistenceSupport = new PersistenceSupport();
  
  @Override
  public List<AutoScalingGroup> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName );
  }

  @Override
  public List<AutoScalingGroup> list( final OwnerFullName ownerFullName, 
                                      final Predicate<? super AutoScalingGroup> filter ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter );
  }

  @Override
  public List<AutoScalingGroup> listRequiringScaling() throws AutoScalingMetadataException {
    return persistenceSupport.listByExample( AutoScalingGroup.requiringScaling(), Predicates.alwaysTrue() );
  }

  @Override
  public List<AutoScalingGroup> listRequiringInstanceReplacement() throws AutoScalingMetadataException {
    final DetachedCriteria criteria = DetachedCriteria.forClass( AutoScalingInstance.class )
        .add( Example.create( AutoScalingInstance.withHealthStatus( HealthStatus.Unhealthy ) ) )
        .setProjection( Projections.property( "autoScalingGroup" ) );

    return persistenceSupport.listByExample(
        AutoScalingGroup.withOwner( null ),
        Predicates.alwaysTrue(),
        Property.forName( "id" ).in( criteria ),
        Collections.<String, String>emptyMap() );
  }

  @Override
  public List<AutoScalingGroup> listRequiringMonitoring( final long interval ) throws AutoScalingMetadataException {
    // We want to select some groups depending on the interval / time 
    int group = (int)((System.currentTimeMillis() / interval) % 6 );

    final Collection<String> suffixes = Lists.newArrayList();
    switch ( group ) {
      case 0:
        suffixes.add( "0" );
        suffixes.add( "1" );
        suffixes.add( "2" );
        break;
      case 1:
        suffixes.add( "3" );
        suffixes.add( "4" );
        suffixes.add( "5" );
        break;
      case 2:
        suffixes.add( "6" );
        suffixes.add( "7" );
        suffixes.add( "8" );
        break;
      case 3:
        suffixes.add( "9" );
        suffixes.add( "a" );
        suffixes.add( "b" );
        break;
      case 4:
        suffixes.add( "c" );
        suffixes.add( "d" );
        break;
      default:
        suffixes.add( "e" );
        suffixes.add( "f" );
        break;
    }
    
    final Junction likeAnyOf = Restrictions.disjunction();
    for ( final String suffix : suffixes ) {
      likeAnyOf.add( Restrictions.ilike( "id", "%" + suffix ) );  
    }
    
    return persistenceSupport.listByExample(
        AutoScalingGroup.withOwner( null ),
        Predicates.alwaysTrue(),
        likeAnyOf,
        Collections.<String,String>emptyMap() );
  }

  @Override
  public AutoScalingGroup lookup( final OwnerFullName ownerFullName, final String autoScalingGroupName ) throws AutoScalingMetadataException {
    return persistenceSupport.lookup( ownerFullName, autoScalingGroupName );
  }

  @Override
  public AutoScalingGroup update( final OwnerFullName ownerFullName,
                                  final String autoScalingGroupName,
                                  final Callback<AutoScalingGroup> groupUpdateCallback ) throws AutoScalingMetadataException {
    return persistenceSupport.update( ownerFullName, autoScalingGroupName, groupUpdateCallback );
  }

  @Override
  public boolean delete( final AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( autoScalingGroup );
  }

  @Override
  public AutoScalingGroup save( final AutoScalingGroup autoScalingGroup ) throws AutoScalingMetadataException {
    return persistenceSupport.save( autoScalingGroup );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistentsWithResourceNameSupport<AutoScalingGroup> {
    private PersistenceSupport() {
      super( AutoScalingResourceName.Type.autoScalingGroup );
    }

    @Override
    protected AutoScalingGroup exampleWithOwner( final OwnerFullName ownerFullName ) {
      return AutoScalingGroup.withOwner( ownerFullName );
    }

    @Override
    protected AutoScalingGroup exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return AutoScalingGroup.named( ownerFullName, name );
    }

    @Override
    protected AutoScalingGroup exampleWithUuid( final String uuid ) {
      return AutoScalingGroup.withUuid( uuid );
    }
  }  
}
