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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.instances.AutoScalingInstanceCoreView;
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
    public List<AutoScalingInstanceCoreView> selectForTermination( final List<AutoScalingInstanceCoreView> instances ) {
      final Date oldest = dateOfOldestInstance( instances );
      return filterByPropertyEquality( instances, oldest, InstanceCreatedDate.INSTANCE );
    }
  }, 
  
  OldestLaunchConfiguration {
    @Override
    public List<AutoScalingInstanceCoreView> selectForTermination( final List<AutoScalingInstanceCoreView> instances ) {
      final Date oldest = dateOfOldestInstance( instances );
      final AutoScalingInstanceCoreView instanceWithOldestLaunchConfiguration =
          Iterables.find(
              instances, 
              Predicates.compose( Predicates.equalTo( oldest ), InstanceCreatedDate.INSTANCE ),
              null 
          );
      final String oldestLaunchConfiguration = instanceWithOldestLaunchConfiguration == null ?
          null :
          instanceWithOldestLaunchConfiguration.getLaunchConfigurationName();
      return filterByPropertyEquality( instances, oldestLaunchConfiguration, launchConfigurationName( ) );
    }
  }, 
  
  NewestInstance {
    @Override
    public List<AutoScalingInstanceCoreView> selectForTermination( final List<AutoScalingInstanceCoreView> instances ) {
      final Date newest = dateOfNewestInstance( instances );
      return filterByPropertyEquality( instances, newest, InstanceCreatedDate.INSTANCE );
    }
  }, 
  
  ClosestToNextInstanceHour {
    @Override
    public List<AutoScalingInstanceCoreView> selectForTermination( final List<AutoScalingInstanceCoreView> instances ) {
      final Function<? super AutoScalingInstanceCoreView,Integer> secondsToInstanceHourFunction =
          Functions.compose( DateToTimeUntilNextHour.INSTANCE, InstanceCreatedDate.INSTANCE );
      final Integer secondsToInstanceHour = CollectionUtils.reduce(
          Iterables.transform( instances, secondsToInstanceHourFunction ),
          Integer.MAX_VALUE,
          CollectionUtils.comparator( Ordering.<Integer>natural() ) );
      return filterByPropertyEquality( instances, secondsToInstanceHour, secondsToInstanceHourFunction );
    }
  }, 
  
  Default {
    @Override
    public List<AutoScalingInstanceCoreView> selectForTermination( final List<AutoScalingInstanceCoreView> instances ) {
      final List<AutoScalingInstanceCoreView> oldestList = OldestLaunchConfiguration.selectForTermination( instances );
      final List<AutoScalingInstanceCoreView> closestToInstanceHour = ClosestToNextInstanceHour.selectForTermination( oldestList );
      final List<AutoScalingInstanceCoreView> result;
      
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
  
  public abstract List<AutoScalingInstanceCoreView> selectForTermination( List<AutoScalingInstanceCoreView> instances );

  /**
   * Select an instance for termination from the given list.
   * 
   * @param terminationPolicyTypes The ordered list of termination policies to use.
   * @param instances The non empty list of instances
   * @return The instance selected for termination
   * @throws IllegalArgumentException if passed an empty list
   */
  public static AutoScalingInstanceCoreView selectForTermination( final Collection<TerminationPolicyType> terminationPolicyTypes,
                                                                  final List<AutoScalingInstanceCoreView> instances ) {
    if ( instances.isEmpty() ) throw new IllegalArgumentException( "No instances provided" );
    List<AutoScalingInstanceCoreView> currentList = instances;
    for ( TerminationPolicyType terminationPolicyType : 
        Iterables.concat( terminationPolicyTypes, Collections.singleton( TerminationPolicyType.Default ) ) ) {
      if ( currentList.size() == 1 ) break;
      currentList = terminationPolicyType.selectForTermination( currentList );  
    }
    return currentList.get( 0 );   
  }
  
  private static <T> List<AutoScalingInstanceCoreView> filterByPropertyEquality( final List<AutoScalingInstanceCoreView> instances,
                                                                                 final T target,
                                                                                 final Function<? super AutoScalingInstanceCoreView,T> propertyFunction ) {
    return Lists.newArrayList( Iterables.filter(
        instances,
        Predicates.compose( Predicates.equalTo( target ), propertyFunction ) ) );
  }

  private static Date dateOfOldestInstance( final List<AutoScalingInstanceCoreView> instances ) {
    return dateOfInstance( instances, new Date( Long.MAX_VALUE ), Ordering.<Date>natural() );
  }

  private static Date dateOfNewestInstance( final List<AutoScalingInstanceCoreView> instances ) {
    return dateOfInstance( instances, new Date( 0 ), Ordering.<Date>natural().reverse() );
  }

  private static Date dateOfInstance( final List<AutoScalingInstanceCoreView> instances,
                                      final Date initialDate,
                                      final Comparator<Date> comparator ) {
    return CollectionUtils.reduce(
        Iterables.transform( instances, InstanceCreatedDate.INSTANCE ),
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

  private enum InstanceCreatedDate implements Function<AutoScalingInstanceCoreView,Date> {
    INSTANCE;

    @Override
    public Date apply( final AutoScalingInstanceCoreView instance ) {
      return new Date( instance.getCreationTimestamp( ) );
    }
  }
}
