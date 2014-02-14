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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.autoscaling.common.AutoScalingResourceName;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.instances.HealthStatus;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistentsWithResourceNameSupport;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
public class PersistenceAutoScalingGroups extends AutoScalingGroups {
  
  private PersistenceSupport persistenceSupport = new PersistenceSupport();
  
  @Override
  public <T> List<T> list( final OwnerFullName ownerFullName,
                           final Predicate<? super AutoScalingGroup> filter,
                           final Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter, transform );
  }

  @Override
  public <T> List<T> listRequiringScaling( final Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.listByExample( AutoScalingGroup.requiringScaling(), Predicates.alwaysTrue(), transform );
  }

  @Override
  public <T> List<T> listRequiringInstanceReplacement( final Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException {
    final DetachedCriteria criteria = DetachedCriteria.forClass( AutoScalingInstance.class )
        .add( Example.create( AutoScalingInstance.withHealthStatus( HealthStatus.Unhealthy ) ) )
        .setProjection( Projections.property( "autoScalingGroup" ) );

    return persistenceSupport.listByExample(
        AutoScalingGroup.withOwner( null ),
        Predicates.alwaysTrue(),
        Property.forName( "id" ).in( criteria ),
        Collections.<String, String>emptyMap(),
        transform );
  }

  @Override
  public <T> List<T> listRequiringMonitoring( final long interval,
                                              final Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException {
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
        Collections.<String,String>emptyMap(),
        transform );
  }

  @Override
  public <T> T lookup( final OwnerFullName ownerFullName,
                       final String autoScalingGroupName,
                       final Function<? super AutoScalingGroup,T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.lookup( ownerFullName, autoScalingGroupName, transform );
  }

  @Override
  public void update( final OwnerFullName ownerFullName,
                      final String autoScalingGroupName,
                      final Callback<AutoScalingGroup> groupUpdateCallback ) throws AutoScalingMetadataException {
    persistenceSupport.update( ownerFullName, autoScalingGroupName, groupUpdateCallback );
  }

  @Override
  public void markScalingRequiredForZones( final Set<String> availabilityZones ) throws AutoScalingMetadataException {
    if ( !availabilityZones.isEmpty() ) {
      persistenceSupport.transactionWithRetry( AutoScalingGroup.class, new AbstractOwnedPersistents.WorkCallback<Void>() {
        @Override
        public Void doWork() throws AutoScalingMetadataException {
          final List<AutoScalingGroup> groups =
              persistenceSupport.listByExample( AutoScalingGroup.withOwner( null ), Predicates.alwaysTrue(), Functions.<AutoScalingGroup>identity() );
          for ( final AutoScalingGroup group : groups ) {
            if ( !Sets.union( Sets.newHashSet( group.getAvailabilityZones() ), availabilityZones ).isEmpty() ) {
              group.setScalingRequired( true );
            }
          }
          return null;
        }
      } );
    }
  }

  @Override
  public boolean delete( final AutoScalingGroupMetadata autoScalingGroup ) throws AutoScalingMetadataException {
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
