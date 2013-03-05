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
package com.eucalyptus.autoscaling.instances;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.eucalyptus.autoscaling.common.AutoScalingInstanceDetails;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.common.Instance;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public abstract class AutoScalingInstances {
  public abstract List<AutoScalingInstance> list( OwnerFullName ownerFullName ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingInstance> list( OwnerFullName ownerFullName,
                                                  Predicate<? super AutoScalingInstance> filter ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingInstance> listByGroup( OwnerFullName ownerFullName,
                                                         String groupName ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingInstance> listByGroup( AutoScalingGroup group ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingInstance> listByState( LifecycleState state ) throws AutoScalingMetadataException;

  public abstract List<AutoScalingInstance> listUnhealthyByGroup( AutoScalingGroup group ) throws AutoScalingMetadataException;

  public abstract AutoScalingInstance lookup( OwnerFullName ownerFullName,
                                              String instanceId ) throws AutoScalingMetadataException;

  public abstract AutoScalingInstance update( OwnerFullName ownerFullName,
                                              String instanceId,
                                              Callback<AutoScalingInstance> instanceUpdateCallback ) throws AutoScalingMetadataException;

  public abstract void markMissingInstancesUnhealthy( AutoScalingGroup group, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract Set<String> verifyInstanceIds( String accountNumber, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract void transitionState( AutoScalingGroup group, LifecycleState from, LifecycleState to, Collection<String> instanceIds ) throws AutoScalingMetadataException;

  public abstract boolean delete( AutoScalingInstance autoScalingInstance ) throws AutoScalingMetadataException;

  public abstract boolean deleteByGroup( final AutoScalingGroup group ) throws AutoScalingMetadataException;

  public abstract AutoScalingInstance save( AutoScalingInstance autoScalingInstance ) throws AutoScalingMetadataException;

  public static Function<AutoScalingInstance,String> instanceId() {
    return AutoScalingMetadatas.toDisplayName();
  }

  public static Function<AutoScalingInstance,String> launchConfigurationName() {
    return AutoScalingInstanceProperties.LAUNCH_CONFIGURATION_NAME;  
  }

  public static Function<AutoScalingInstance,String> groupArn() {
    return AutoScalingInstanceProperties.GROUP_ARN;
  }
  
  public static Function<AutoScalingInstance,String> availabilityZone() {
    return AutoScalingInstanceProperties.AVAILABILITY_ZONE; 
  }

  @TypeMapper
  public enum AutoScalingInstanceTransform implements Function<AutoScalingInstance, AutoScalingInstanceDetails> {
    INSTANCE;

    @Override
    public AutoScalingInstanceDetails apply( final AutoScalingInstance autoScalingInstance ) {
      final AutoScalingInstanceDetails details = new AutoScalingInstanceDetails();      
      details.setAutoScalingGroupName( autoScalingInstance.getAutoScalingGroupName() );
      details.setAvailabilityZone( autoScalingInstance.getAvailabilityZone() );
      details.setHealthStatus( Strings.toString( autoScalingInstance.getHealthStatus() ) );
      details.setInstanceId( autoScalingInstance.getInstanceId() );
      details.setLaunchConfigurationName( autoScalingInstance.getLaunchConfigurationName() );
      details.setLifecycleState( Strings.toString( autoScalingInstance.getLifecycleState() ) );      
      return details; 
    }
  }

  @TypeMapper
  public enum AutoScalingInstanceSummaryTransform implements Function<AutoScalingInstance, Instance> {
    INSTANCE;

    @Override
    public Instance apply( final AutoScalingInstance autoScalingInstance ) {
      final Instance details = new Instance();
      details.setAvailabilityZone( autoScalingInstance.getAvailabilityZone() );
      details.setHealthStatus( Strings.toString( autoScalingInstance.getHealthStatus() ) );
      details.setInstanceId( autoScalingInstance.getInstanceId() );
      details.setLaunchConfigurationName( autoScalingInstance.getLaunchConfigurationName() );
      details.setLifecycleState( Strings.toString( autoScalingInstance.getLifecycleState() ) );
      return details;
    }
  }
  
  private enum AutoScalingInstanceProperties implements Function<AutoScalingInstance,String> {
    AVAILABILITY_ZONE {
      @Override
      public String apply( final AutoScalingInstance autoScalingInstance ) {
        return autoScalingInstance.getAvailabilityZone();
      }
    },
    GROUP_ARN {
      @Override
      public String apply( final AutoScalingInstance autoScalingInstance ) {
        return AutoScalingMetadatas.toArn().apply( autoScalingInstance.getAutoScalingGroup() ); 
      }
    },
    LAUNCH_CONFIGURATION_NAME {
      @Override
      public String apply( final AutoScalingInstance autoScalingInstance ) {
        return autoScalingInstance.getLaunchConfigurationName();
      }
    }
  }
}
