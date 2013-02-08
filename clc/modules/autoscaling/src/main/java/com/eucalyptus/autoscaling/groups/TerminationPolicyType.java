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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.TerminationPolicyTypeMetadata;
import static com.eucalyptus.autoscaling.instances.AutoScalingInstances.launchConfigurationName;
import static com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents.createdDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.instances.AutoScalingInstance;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 *
 */
public enum TerminationPolicyType implements TerminationPolicyTypeMetadata {  
  
  OldestInstance {
    @Override
    public List<AutoScalingInstance> selectForTermination( final List<AutoScalingInstance> instances ) {
      final Date oldest = dateOfOldestInstance( instances );
      return filterByPropertyEquality( instances, oldest, createdDate() );
    }
  }, 
  
  OldestLaunchConfiguration {
    @Override
    public List<AutoScalingInstance> selectForTermination( final List<AutoScalingInstance> instances ) {
      final Date oldest = dateOfOldestInstance( instances );
      final AutoScalingInstance instanceWithOldestLaunchConfiguration = 
          Iterables.find(
              instances, 
              Predicates.compose( Predicates.equalTo( oldest ), createdDate() ), 
              null 
          );
      final String oldestLaunchConfiguration = instanceWithOldestLaunchConfiguration == null ?
          null :
          instanceWithOldestLaunchConfiguration.getLaunchConfigurationName();
      return filterByPropertyEquality( instances, oldestLaunchConfiguration, launchConfigurationName() );
    }
  }, 
  
  NewestInstance {
    @Override
    public List<AutoScalingInstance> selectForTermination( final List<AutoScalingInstance> instances ) {
      final Date newest = dateOfNewestInstance( instances );
      return filterByPropertyEquality( instances, newest, createdDate() );
    }
  }, 
  
  ClosestToNextInstanceHour {
    @Override
    public List<AutoScalingInstance> selectForTermination( final List<AutoScalingInstance> instances ) {
      final Function<AutoScalingInstance,Integer> secondsToInstanceHourFunction =
          Functions.compose( DateToTimeUntilNextHour.INSTANCE, createdDate() );
      final Integer secondsToInstanceHour = CollectionUtils.reduce(
          Iterables.transform( instances, secondsToInstanceHourFunction ),
          Integer.MAX_VALUE,
          CollectionUtils.comparator( Ordering.<Integer>natural() ) );
      return filterByPropertyEquality( instances, secondsToInstanceHour, secondsToInstanceHourFunction );
    }
  }, 
  
  Default {
    @Override
    public List<AutoScalingInstance> selectForTermination( final List<AutoScalingInstance> instances ) {
      final List<AutoScalingInstance> oldestList = OldestLaunchConfiguration.selectForTermination( instances );
      final List<AutoScalingInstance> closestToInstanceHour = ClosestToNextInstanceHour.selectForTermination( oldestList );
      final List<AutoScalingInstance> result;
      
      if ( closestToInstanceHour.size() == 1 ) {
        result = closestToInstanceHour;        
      } else if ( closestToInstanceHour.isEmpty() ) {
        result = Lists.newArrayList();        
      } else {
        Collections.shuffle( closestToInstanceHour );
        result = closestToInstanceHour.subList( 0, 1 );
      }
      
      return result;
    }
  };

  @Override
  public String getDisplayName() {
    return name();
  }

  @Override
  public OwnerFullName getOwner() {
    return Principals.systemFullName();
  }
  
  public abstract List<AutoScalingInstance> selectForTermination( List<AutoScalingInstance> instances );

  /**
   * Select an instance for termination from the given list.
   * 
   * @param terminationPolicyTypes The ordered list of termination policies to use.
   * @param instances The non empty list of instances
   * @return The instance selected for termination
   * @throws IllegalArgumentException if passed an empty list
   */
  public static AutoScalingInstance selectForTermination( final Collection<TerminationPolicyType> terminationPolicyTypes,
                                                          final List<AutoScalingInstance> instances ) {
    if ( instances.isEmpty() ) throw new IllegalArgumentException( "No instances provided" );
    List<AutoScalingInstance> currentList = instances;
    for ( TerminationPolicyType terminationPolicyType : 
        Iterables.concat( terminationPolicyTypes, Collections.singleton( TerminationPolicyType.Default ) ) ) {
      if ( currentList.size() == 1 ) break;
      currentList = terminationPolicyType.selectForTermination( currentList );  
    }
    return currentList.get( 0 );   
  }
  
  private static <T> List<AutoScalingInstance> filterByPropertyEquality( final List<AutoScalingInstance> instances, 
                                                                         final T target,
                                                                         final Function<AutoScalingInstance,T> propertyFunction ) {
    return Lists.newArrayList( Iterables.filter(
        instances,
        Predicates.compose( Predicates.equalTo( target ), propertyFunction ) ) );
  }

  private static Date dateOfOldestInstance( final List<AutoScalingInstance> instances ) {
    return dateOfInstance( instances, new Date( Long.MAX_VALUE ), Ordering.<Date>natural() );
  }

  private static Date dateOfNewestInstance( final List<AutoScalingInstance> instances ) {
    return dateOfInstance( instances, new Date( 0 ), Ordering.<Date>natural().reverse() );
  }

  private static Date dateOfInstance( final List<AutoScalingInstance> instances,
                                      final Date initialDate,
                                      final Comparator<Date> comparator ) {
    return CollectionUtils.reduce(
        Iterables.transform( instances, AbstractOwnedPersistents.createdDate() ),
        initialDate,
        CollectionUtils.comparator( comparator ) );
  }
  
  private enum DateToTimeUntilNextHour implements Function<Date,Integer> {
    INSTANCE;

    @Override
    public Integer apply( final Date date ) {
      long timeInHour = date.getTime() % TimeUnit.HOURS.toMillis( 1 );
      return (int)(( TimeUnit.HOURS.toMillis( 1 ) - timeInHour ) / 1000L);
    }
  }  
}
